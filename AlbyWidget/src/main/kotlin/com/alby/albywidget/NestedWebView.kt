package com.alby.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewParent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chat content usually scrolls in a CSS overflow container, not the WebView document.
 * We ask the page whether that container can still move, keep the gesture while it can,
 * and pass leftover movement to a host scroller (LazyColumn, NestedScrollView, etc.).
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
internal class NestedWebView(context: Context) : WebView(context), NestedScrollingChild3 {
    private val childHelper = NestedScrollingChildHelper(this)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val scrollConsumed = IntArray(2)
    private val scrollOffset = IntArray(2)

    @Volatile
    private var jsReady = false

    @Volatile
    private var canScrollUpJs = false

    @Volatile
    private var canScrollDownJs = false

    private var startY = 0f
    private var lastY = 0f
    private var passedSlop = false
    private var handedOffToParent = false

    init {
        isNestedScrollingEnabled = false
    }

    var handoffScrollToParent: Boolean = false
        set(value) {
            field = value
            isNestedScrollingEnabled = value
            if (value) {
                ensureScrollBridge()
            }
        }

    private var scrollBridgeInstalled = false

    private fun ensureScrollBridge() {
        if (scrollBridgeInstalled) return
        addJavascriptInterface(ScrollBridge(), JS_INTERFACE)
        scrollBridgeInstalled = true
    }

    fun installScrollDetection() {
        if (!handoffScrollToParent) return
        ensureScrollBridge()
        evaluateJavascript(SCROLL_DETECTION_JS, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!handoffScrollToParent) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    requestParentsDisallowIntercept(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    requestParentsDisallowIntercept(false)
                }
            }
            return super.onTouchEvent(event)
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                lastY = event.y
                passedSlop = false
                handedOffToParent = false
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                requestParentsDisallowIntercept(true)
                reportScrollState(event)
            }

            MotionEvent.ACTION_MOVE -> {
                reportScrollState(event)
                val dy = lastY - event.y
                lastY = event.y

                if (!passedSlop && abs(event.y - startY) > touchSlop) {
                    passedSlop = true
                }

                if (passedSlop) {
                    val childCanScroll = when {
                        dy > 0 -> canScrollDown()
                        dy < 0 -> canScrollUp()
                        else -> true
                    }

                    if (handedOffToParent || !childCanScroll) {
                        handedOffToParent = true
                        requestParentsDisallowIntercept(false)
                        dispatchUnconsumedToParent(dy.roundToInt())
                        return true
                    }

                    requestParentsDisallowIntercept(true)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                requestParentsDisallowIntercept(false)
                stopNestedScroll()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dispatchUnconsumedToParent(dy: Int) {
        if (dy == 0) return
        scrollConsumed.fill(0)
        dispatchNestedPreScroll(0, dy, scrollConsumed, scrollOffset)
        val unconsumed = dy - scrollConsumed[1]
        if (unconsumed != 0) {
            dispatchNestedScroll(0, 0, 0, unconsumed, scrollOffset)
        }
    }

    private fun canScrollUp(): Boolean {
        return if (!jsReady) true else canScrollUpJs || canScrollVertically(-1)
    }

    private fun canScrollDown(): Boolean {
        return if (!jsReady) true else canScrollDownJs || canScrollVertically(1)
    }

    private fun reportScrollState(event: MotionEvent) {
        val density = resources.displayMetrics.density
        val x = event.x / density
        val y = event.y / density
        evaluateJavascript("window.__albyReportScroll&&window.__albyReportScroll($x,$y)", null)
    }

    private fun requestParentsDisallowIntercept(disallow: Boolean) {
        var ancestor: ViewParent? = parent
        while (ancestor != null) {
            ancestor.requestDisallowInterceptTouchEvent(disallow)
            ancestor = ancestor.parent
        }
    }

    inner class ScrollBridge {
        @JavascriptInterface
        fun update(canScrollUp: Boolean, canScrollDown: Boolean) {
            canScrollUpJs = canScrollUp
            canScrollDownJs = canScrollDown
            jsReady = true
        }
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean = childHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int): Boolean = childHelper.startNestedScroll(axes)

    override fun startNestedScroll(axes: Int, type: Int): Boolean =
        childHelper.startNestedScroll(axes, type)

    override fun stopNestedScroll() = childHelper.stopNestedScroll()

    override fun stopNestedScroll(type: Int) = childHelper.stopNestedScroll(type)

    override fun hasNestedScrollingParent(): Boolean = childHelper.hasNestedScrollingParent()

    override fun hasNestedScrollingParent(type: Int): Boolean =
        childHelper.hasNestedScrollingParent(type)

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        childHelper.dispatchNestedScroll(
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            offsetInWindow,
            type,
            consumed
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean = childHelper.dispatchNestedScroll(
        dxConsumed,
        dyConsumed,
        dxUnconsumed,
        dyUnconsumed,
        offsetInWindow,
        type
    )

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedScroll(
        dxConsumed,
        dyConsumed,
        dxUnconsumed,
        dyUnconsumed,
        offsetInWindow
    )

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean = childHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean =
        childHelper.dispatchNestedPreFling(velocityX, velocityY)

    private companion object {
        const val JS_INTERFACE = "albyNestedScroll"

        const val SCROLL_DETECTION_JS = """
            (function() {
              if (window.__albyNestedScrollInstalled) return;
              window.__albyNestedScrollInstalled = true;

              function isScrollable(el) {
                if (!el) return false;
                var oy = window.getComputedStyle(el).overflowY;
                var canOverflow = oy === 'auto' || oy === 'scroll' || oy === 'overlay';
                var isRoot = el === document.documentElement || el === document.body ||
                  el === document.scrollingElement;
                if (!canOverflow && !isRoot) return false;
                return el.scrollHeight > el.clientHeight + 1;
              }

              function parentOf(el) {
                if (el.parentElement) return el.parentElement;
                var root = el.getRootNode && el.getRootNode();
                return root && root.host ? root.host : null;
              }

              function deepElementFromPoint(x, y) {
                var el = document.elementFromPoint(x, y);
                while (el && el.shadowRoot) {
                  var inner = el.shadowRoot.elementFromPoint(x, y);
                  if (!inner || inner === el) break;
                  el = inner;
                }
                return el;
              }

              function collectFromPoint(x, y) {
                var found = [];
                var el = deepElementFromPoint(x, y);
                while (el) {
                  if (isScrollable(el)) found.push(el);
                  el = parentOf(el);
                }
                return found;
              }

              function collectAll(root, acc) {
                if (!root) return;
                if (root.querySelectorAll) {
                  var nodes = root.querySelectorAll('*');
                  for (var i = 0; i < nodes.length; i++) {
                    if (isScrollable(nodes[i])) acc.push(nodes[i]);
                    if (nodes[i].shadowRoot) collectAll(nodes[i].shadowRoot, acc);
                  }
                }
              }

              function report(x, y) {
                if (!window.albyNestedScroll) return;
                var els = collectFromPoint(x, y);
                if (!els.length) {
                  collectAll(document, els);
                  if (document.scrollingElement && isScrollable(document.scrollingElement)) {
                    els.push(document.scrollingElement);
                  }
                }
                var up = false;
                var down = false;
                for (var i = 0; i < els.length; i++) {
                  var el = els[i];
                  up = up || el.scrollTop > 1;
                  down = down || el.scrollTop < el.scrollHeight - el.clientHeight - 1;
                }
                window.albyNestedScroll.update(up, down);
              }

              window.__albyReportScroll = report;
              window.addEventListener('scroll', function() {
                report(window.__albyLastX || 0, window.__albyLastY || 0);
              }, {capture: true, passive: true});
              window.addEventListener('touchstart', function(e) {
                var t = e.touches[0];
                if (!t) return;
                window.__albyLastX = t.clientX;
                window.__albyLastY = t.clientY;
                report(t.clientX, t.clientY);
              }, {capture: true, passive: true});
              window.addEventListener('touchmove', function(e) {
                var t = e.touches[0];
                if (!t) return;
                window.__albyLastX = t.clientX;
                window.__albyLastY = t.clientY;
                report(t.clientX, t.clientY);
              }, {capture: true, passive: true});
            })();
        """
    }
}
