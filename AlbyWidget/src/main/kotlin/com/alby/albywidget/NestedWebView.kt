package com.alby.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs

/**
 * The chat scrolls inside a DOM element, which Android cannot see, so the page
 * reports through JS whether the chat can scroll up or down.
 *
 * If the chat cannot scroll, drags are left to the host. Otherwise the widget
 * claims the drag on touch down (a Compose host sees moves before an embedded
 * View, so deciding later would race it). Once the direction is known, the chat
 * keeps the drag if it can scroll that way; if not, the drag is handed to the
 * host through standard nested scrolling (Compose, NestedScrollView,
 * CoordinatorLayout), or released so the host intercepts it (RecyclerView,
 * ScrollView, or a View scroller around a ComposeView).
 */
@SuppressLint("JavascriptInterface")
internal class NestedWebView(context: Context) : WebView(context) {
    private val viewConfig = ViewConfiguration.get(context)
    private val nestedScroll = NestedScrollingChildHelper(this).apply { isNestedScrollingEnabled = true }
    private val consumed = IntArray(2)

    // Anything in the page can scroll, known before a touch reaches the page (may be stale).
    @Volatile private var canScrollUp = false
    @Volatile private var canScrollDown = false
    // What can scroll under the finger, reported on touchstart of the current drag.
    @Volatile private var touchReported = false
    @Volatile private var touchCanScrollUp = false
    @Volatile private var touchCanScrollDown = false

    private var inGesture = false
    private var claimed = false
    private var decided = false
    private var handingOff = false
    private var hostScrolled = false
    private var downY = 0f
    private var lastY = 0f
    private var velocity: VelocityTracker? = null

    init {
        // Compose's AndroidView only forwards nested scroll if the View itself has it enabled.
        isNestedScrollingEnabled = true
        addJavascriptInterface(ScrollBridge(), JS_INTERFACE)
    }

    fun installScrollDetection() {
        evaluateJavascript(SCROLL_STATE_JS, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.rawY
                inGesture = true
                touchReported = false
                decided = false
                handingOff = false
                claimed = canScrollUp || canScrollDown
                if (claimed) requestParentsDisallowIntercept(true)
            }

            MotionEvent.ACTION_MOVE -> {
                if (claimed && !decided && abs(event.rawY - downY) > viewConfig.scaledTouchSlop) {
                    decided = true
                    val fingerUp = event.rawY < downY
                    val up = if (touchReported) touchCanScrollUp else canScrollUp
                    val down = if (touchReported) touchCanScrollDown else canScrollDown
                    if (!(if (fingerUp) down else up)) startHandOff(event)
                }
                if (handingOff) {
                    trackVelocity(event)
                    // Positive dy scrolls content down (finger up), as in View.scrollBy.
                    val dy = (lastY - event.rawY).toInt()
                    lastY -= dy
                    if (scrollHost(dy)) {
                        hostScrolled = true
                    } else if (!hostScrolled) {
                        // No nested scrolling host took it: let a View host intercept instead.
                        // Posted so Compose's AndroidView finishes this event first; releasing
                        // mid-event makes it cancel the WebView and stop sending it touches.
                        stopHandOff()
                        post { requestParentsDisallowIntercept(false) }
                    }
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                inGesture = false
                if (handingOff) {
                    flingHost(event)
                    stopHandOff()
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

    // The touch report can show the chat scrolls when the state seen on touch down was
    // stale; claim then, unless the host already took the drag.
    private fun claimFromTouchReport() {
        if (!inGesture || claimed || !(touchCanScrollUp || touchCanScrollDown)) return
        claimed = true
        requestParentsDisallowIntercept(true)
    }

    private fun startHandOff(event: MotionEvent) {
        handingOff = true
        hostScrolled = false
        lastY = downY
        velocity = VelocityTracker.obtain()
        nestedScroll.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
        // Stop the WebView's own gesture now that the host owns the drag.
        val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        super.onTouchEvent(cancel)
        cancel.recycle()
    }

    private fun stopHandOff() {
        handingOff = false
        nestedScroll.stopNestedScroll()
        velocity?.recycle()
        velocity = null
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
        if (dy == 0) return hostScrolled
        consumed.fill(0)
        nestedScroll.dispatchNestedPreScroll(0, dy, consumed, null)
        val preConsumed = consumed[1]
        consumed.fill(0)
        nestedScroll.dispatchNestedScroll(0, preConsumed, 0, dy - preConsumed, null, ViewCompat.TYPE_TOUCH, consumed)
        return preConsumed != 0 || consumed[1] != 0
    }

    private fun flingHost(event: MotionEvent) {
        val tracker = velocity ?: return
        trackVelocity(event)
        tracker.computeCurrentVelocity(1000, viewConfig.scaledMaximumFlingVelocity.toFloat())
        val vy = -tracker.yVelocity
        if (abs(vy) < viewConfig.scaledMinimumFlingVelocity) return
        if (!nestedScroll.dispatchNestedPreFling(0f, vy)) nestedScroll.dispatchNestedFling(0f, vy, false)
    }

    // Screen coordinates, since the WebView moves with the host while it scrolls.
    private fun trackVelocity(event: MotionEvent) {
        val screenEvent = MotionEvent.obtain(event).apply { setLocation(event.rawX, event.rawY) }
        velocity?.addMovement(screenEvent)
        screenEvent.recycle()
    }

    inner class ScrollBridge {
        @JavascriptInterface
        fun update(scrollUp: Boolean, scrollDown: Boolean, isTouch: Boolean, touchUp: Boolean, touchDown: Boolean) {
            canScrollUp = scrollUp
            canScrollDown = scrollDown
            if (isTouch) {
                touchCanScrollUp = touchUp
                touchCanScrollDown = touchDown
                touchReported = true
                post { claimFromTouchReport() }
            }
        }
    }

    private companion object {
        const val JS_INTERFACE = "albyNestedScroll"

        // Reports whether any scroller (including inside shadow roots) or the
        // document can scroll up/down. Runs on load, on every touch start/end,
        // and after DOM changes or resizes (so content that becomes scrollable is
        // known before the next touch). On touchstart it also reports what can
        // scroll under the finger: scrollers on the touch path, then the document,
        // which is how the browser chains the scroll.
        const val SCROLL_STATE_JS = """
            (function() {
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
                var touch = {up: false, down: false};
                var isTouch = !!(e && e.type === 'touchstart');
                if (isTouch) {
                  e.composedPath().forEach(function(el) { checkScroller(el, touch); });
                  check(doc, touch);
                }
                albyNestedScroll.update(all.up, all.down, isTouch, touch.up, touch.down);
              }
              if (!window.__albyScrollInstalled) {
                window.__albyScrollInstalled = true;
                ['touchstart', 'touchend'].forEach(function(type) {
                  document.addEventListener(type, report, {capture: true, passive: true});
                });
                observe(document);
                window.addEventListener('resize', schedule);
              }
              report();
            })();
        """
    }
}
