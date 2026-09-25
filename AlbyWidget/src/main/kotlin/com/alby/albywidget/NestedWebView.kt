package com.alby.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.AbsListView
import android.widget.ScrollView
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import kotlin.math.abs

/**
 * The chat scrolls inside a DOM element, which Android cannot see, so the page
 * reports through JS whether the chat can scroll up or down.
 *
 * If the chat cannot scroll, drags are left to the host. Otherwise the widget
 * claims the drag on touch down (a Compose host sees moves before an embedded
 * View, so deciding later would race it). Once the direction is known, the chat
 * keeps the drag if it can scroll that way; if not, the drag is handed to the
 * host through standard nested scrolling (Compose, CoordinatorLayout), or released so
 * a View scroller around a ComposeView intercepts it (NestedScrollView, RecyclerView,
 * ScrollView). A drag only decided on lift has nothing left to intercept, so the
 * nearest vertical View scroller is scrolled and flung directly instead.
 */
@SuppressLint("JavascriptInterface")
internal class NestedWebView(context: Context) : WebView(context) {
    private val viewConfig = ViewConfiguration.get(context)
    private val nestedScroll = NestedScrollingChildHelper(this).apply { isNestedScrollingEnabled = true }
    private val consumed = IntArray(2)

    // Anything in the page can scroll, known before a touch reaches the page (may be stale).
    @Volatile private var canScrollUp = false
    @Volatile private var canScrollDown = false
    // The current press; its touchstart report says what can scroll under the finger.
    @Volatile private var press: Press? = null

    private var inGesture = false
    private var claimed = false
    private var decided = false
    private var handingOff = false
    private var hostScrolled = false
    private var viewScroller: View? = null
    private var downY = 0f
    private var lastY = 0f
    private val velocity = VelocityTracker.obtain()

    init {
        // Compose's AndroidView only forwards nested scroll if the View itself has it enabled.
        isNestedScrollingEnabled = true
        addJavascriptInterface(ScrollBridge(), JS_INTERFACE)
    }

    fun installScrollDetection() {
        evaluateJavascript(SCROLL_STATE_JS, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val slop = viewConfig.scaledTouchSlop
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                press = Press(event.x, event.y)
                downY = event.rawY
                inGesture = true
                decided = false
                handingOff = false
                velocity.clear()
                trackVelocity(event)
                claimed = canScrollUp || canScrollDown
                if (claimed) requestParentsDisallowIntercept(true)
            }

            MotionEvent.ACTION_MOVE -> {
                trackVelocity(event)
                // A claimed drag cannot be stolen, so wait for the report of what can
                // scroll under the finger (or give up on it after moving well past slop).
                val moved = abs(event.rawY - downY)
                if (claimed && !decided && moved > slop && (press?.reported == true || moved > 3 * slop)) {
                    decide(event)
                }
                if (handingOff) {
                    followFinger(event, lifted = false)
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                inGesture = false
                trackVelocity(event)
                // The report may land after the last move, so a short drag decides on lift.
                if (claimed && !decided && abs(event.rawY - downY) > slop) decide(event)
                if (handingOff) {
                    followFinger(event, lifted = true)
                    if (handingOff) {
                        flingHost()
                        stopHandOff()
                    }
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                inGesture = false
                if (handingOff) {
                    stopHandOff()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun decide(event: MotionEvent) {
        decided = true
        val current = press
        val fingerUp = event.rawY < downY
        val up = if (current?.reported == true) current.canScrollUp else canScrollUp
        val down = if (current?.reported == true) current.canScrollDown else canScrollDown
        if (!(if (fingerUp) down else up)) startHandOff(event)
    }

    // The state seen on touch down may have been stale; claim once the report shows the
    // chat scrolls under the finger, unless the host already took the drag.
    private fun claimFromTouchReport(reported: Press) {
        if (reported !== press || !inGesture || claimed) return
        claimed = true
        requestParentsDisallowIntercept(true)
    }

    private fun startHandOff(event: MotionEvent) {
        handingOff = true
        hostScrolled = false
        viewScroller = null
        lastY = downY
        nestedScroll.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
        // Stop the WebView's own gesture now that the host owns the drag.
        val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        super.onTouchEvent(cancel)
        cancel.recycle()
    }

    private fun stopHandOff() {
        handingOff = false
        nestedScroll.stopNestedScroll()
    }

    /** Scrolls the host by how far the finger moved since the last event. */
    private fun followFinger(event: MotionEvent, lifted: Boolean) {
        // Positive dy scrolls content down (finger up), as in View.scrollBy.
        val dy = (lastY - event.rawY).toInt()
        lastY -= dy
        if (dy == 0) return
        when {
            scrollHost(dy) -> hostScrolled = true
            hostScrolled -> Unit
            // No nested scrolling parent took it (e.g. a View scroller around a ComposeView).
            // On lift nothing is left to intercept, so scroll that View scroller directly.
            lifted -> viewScroller = findViewScroller(dy)?.also { scrollView(it, dy) }
            else -> {
                // Let a View host intercept instead, so it also drives its own parents
                // (app bars, pull to refresh). Posted so Compose's AndroidView finishes this
                // event first; releasing mid-event makes it cancel the WebView and stop
                // sending it touches.
                stopHandOff()
                post { requestParentsDisallowIntercept(false) }
            }
        }
    }

    // Compose hosts AndroidView in a container that never gets touch dispatch, so its
    // disallow flag is never reset and stops the request from propagating. Tell every
    // ancestor directly.
    private fun requestParentsDisallowIntercept(disallow: Boolean) {
        var ancestor = parent
        while (ancestor != null) {
            ancestor.requestDisallowInterceptTouchEvent(disallow)
            ancestor = ancestor.parent
        }
    }

    /** Returns whether any nested scrolling host consumed part of [dy]. */
    private fun scrollHost(dy: Int): Boolean {
        consumed.fill(0)
        nestedScroll.dispatchNestedPreScroll(0, dy, consumed, null)
        val preConsumed = consumed[1]
        consumed.fill(0)
        nestedScroll.dispatchNestedScroll(0, preConsumed, 0, dy - preConsumed, null, ViewCompat.TYPE_TOUCH, consumed)
        return preConsumed != 0 || consumed[1] != 0
    }

    private fun flingHost() {
        velocity.computeCurrentVelocity(1000, viewConfig.scaledMaximumFlingVelocity.toFloat())
        val vy = -velocity.yVelocity
        if (abs(vy) < viewConfig.scaledMinimumFlingVelocity) return
        val scroller = viewScroller
        if (scroller != null) {
            flingView(scroller, vy.toInt())
        } else if (!nestedScroll.dispatchNestedPreFling(0f, vy)) {
            nestedScroll.dispatchNestedFling(0f, vy, false)
        }
    }

    /** Nearest View scroller that can scroll vertically in the direction of [dy]. */
    private fun findViewScroller(dy: Int): View? {
        val direction = if (dy > 0) 1 else -1
        var ancestor = parent
        while (ancestor is View) {
            val isScroller = ancestor is NestedScrollView || ancestor is ScrollView ||
                ancestor is AbsListView || isRecyclerView(ancestor)
            if (isScroller && ancestor.canScrollVertically(direction)) return ancestor
            ancestor = ancestor.parent
        }
        return null
    }

    // The SDK does not depend on RecyclerView, so match it (or a subclass) by name.
    private fun isRecyclerView(view: View): Boolean {
        var type: Class<*>? = view.javaClass
        while (type != null) {
            if (type.name == RECYCLER_VIEW) return true
            type = type.superclass
        }
        return false
    }

    private fun scrollView(scroller: View, dy: Int) {
        if (scroller is AbsListView) scroller.scrollListBy(dy) else scroller.scrollBy(0, dy)
    }

    private fun flingView(scroller: View, vy: Int) {
        when (scroller) {
            is NestedScrollView -> scroller.fling(vy)
            is ScrollView -> scroller.fling(vy)
            is AbsListView -> scroller.fling(vy)
            // RecyclerView.fling(int, int); kept by consumer-rules.pro.
            else -> runCatching {
                scroller.javaClass.getMethod("fling", Int::class.java, Int::class.java).invoke(scroller, 0, vy)
            }
        }
    }

    // Screen coordinates, since the WebView moves with the host while it scrolls.
    private fun trackVelocity(event: MotionEvent) {
        val screenEvent = MotionEvent.obtain(event).apply { setLocation(event.rawX, event.rawY) }
        velocity.addMovement(screenEvent)
        screenEvent.recycle()
    }

    inner class ScrollBridge {
        @JavascriptInterface
        fun update(scrollUp: Boolean, scrollDown: Boolean) {
            canScrollUp = scrollUp
            canScrollDown = scrollDown
        }

        /** [x] and [y] are where the touch started, in WebView pixels. */
        @JavascriptInterface
        fun touch(x: Float, y: Float, scrollUp: Boolean, scrollDown: Boolean) {
            val current = press ?: return
            // A late report from an earlier press does not match where this one started.
            val slop = viewConfig.scaledTouchSlop
            if (abs(x - current.x) > slop || abs(y - current.y) > slop) return
            current.canScrollUp = scrollUp
            current.canScrollDown = scrollDown
            current.reported = true
            if (scrollUp || scrollDown) post { claimFromTouchReport(current) }
        }
    }

    /** Where a press started, in WebView pixels, and what its touchstart reported. */
    private class Press(val x: Float, val y: Float) {
        @Volatile var canScrollUp = false
        @Volatile var canScrollDown = false
        @Volatile var reported = false
    }

    private companion object {
        const val JS_INTERFACE = "albyNestedScroll"
        const val RECYCLER_VIEW = "androidx.recyclerview.widget.RecyclerView"

        // Reports whether any scroller (including inside shadow roots) or the
        // document can scroll up/down. Runs on load, on every touch start/end,
        // while an answer streams in, and after DOM changes or resizes, so content
        // that becomes scrollable is known before the next touch. On touchstart it
        // also reports what can scroll under the finger: scrollers on the touch
        // path, then the document, which is how the browser chains the scroll.
        // Installed once per page; later calls only report.
        const val SCROLL_STATE_JS = """
            (function() {
              if (window.__albyScrollReport) { window.__albyScrollReport(); return; }
              function check(el, s) {
                if (el.scrollHeight - el.clientHeight <= 8) return;
                s.up = s.up || el.scrollTop > 8;
                s.down = s.down || el.scrollTop + el.clientHeight < el.scrollHeight - 8;
              }
              function checkScroller(el, s) {
                if (el.nodeType === 1 && el !== document.body && el !== document.documentElement &&
                    /auto|scroll|overlay/.test(getComputedStyle(el).overflowY)) check(el, s);
              }
              var pending = false;
              function schedule() {
                if (pending) return;
                pending = true;
                requestAnimationFrame(function() { pending = false; report(); });
              }
              var observed = new WeakSet();
              function observe(root) {
                if (observed.has(root)) return;
                observed.add(root);
                new MutationObserver(schedule).observe(root,
                    {childList: true, subtree: true, characterData: true, attributes: true});
              }
              function walk(root, s) {
                root.querySelectorAll('*').forEach(function(el) {
                  checkScroller(el, s);
                  if (el.shadowRoot) {
                    observe(el.shadowRoot);
                    walk(el.shadowRoot, s);
                  }
                });
              }
              function report(e) {
                var doc = document.scrollingElement || document.documentElement;
                var all = {up: false, down: false};
                walk(document, all);
                check(doc, all);
                albyNestedScroll.update(all.up, all.down);
                if (e && e.type === 'touchstart' && e.touches.length === 1) {
                  var touch = {up: false, down: false};
                  e.composedPath().forEach(function(el) { checkScroller(el, touch); });
                  check(doc, touch);
                  var t = e.touches[0];
                  albyNestedScroll.touch(t.clientX * devicePixelRatio, t.clientY * devicePixelRatio,
                      touch.up, touch.down);
                }
              }
              window.__albyScrollReport = function() { report(); };
              ['touchstart', 'touchend'].forEach(function(type) {
                document.addEventListener(type, report, {capture: true, passive: true});
              });
              ['streaming-in-progress', 'streaming-finished'].forEach(function(type) {
                document.addEventListener(type, schedule);
              });
              window.addEventListener('resize', schedule);
              observe(document);
              report();
            })();
        """
    }
}
