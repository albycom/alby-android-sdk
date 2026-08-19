package com.alby.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewParent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.view.ViewCompat
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chat content usually scrolls in a CSS overflow container, not the WebView document.
 * By default the widget keeps vertical drags. When [handoffScrollToParent] is true,
 * leftover movement at the chat edges is passed to the host scroller.
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
internal class NestedWebView(context: Context) : WebView(context) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val scrollConsumed = IntArray(2)
    private val scrollOffset = IntArray(2)

    @Volatile private var jsReady = false
    @Volatile private var canScrollUpJs = false
    @Volatile private var canScrollDownJs = false

    private var startY = 0f
    private var lastY = 0f
    private var handedOffToParent = false
    private var scrollBridgeInstalled = false

    var handoffScrollToParent: Boolean = false
        set(value) {
            field = value
            isNestedScrollingEnabled = value
            if (value) ensureScrollBridge()
        }

    fun installScrollDetection() {
        jsReady = false
        canScrollUpJs = false
        canScrollDownJs = false
        if (!handoffScrollToParent) return
        ensureScrollBridge()
        evaluateJavascript(SCROLL_DETECTION_JS, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                lastY = event.y
                handedOffToParent = false
                requestParentsDisallowIntercept(true)
                if (handoffScrollToParent) {
                    ViewCompat.startNestedScroll(this, ViewCompat.SCROLL_AXIS_VERTICAL)
                    reportScrollState(event)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (handoffScrollToParent) {
                    reportScrollState(event)
                    val dy = lastY - event.y
                    lastY = event.y
                    if (handedOffToParent || shouldHandoff(dy, event.y)) {
                        handedOffToParent = true
                        requestParentsDisallowIntercept(false)
                        dispatchUnconsumedToParent(dy.roundToInt())
                        return true
                    }
                }
                requestParentsDisallowIntercept(true)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                requestParentsDisallowIntercept(false)
                if (handoffScrollToParent) ViewCompat.stopNestedScroll(this)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun shouldHandoff(dy: Float, y: Float): Boolean {
        if (abs(y - startY) <= touchSlop || dy == 0f) return false
        if (!jsReady) return false
        val canScroll = if (dy > 0) {
            canScrollDownJs || canScrollVertically(1)
        } else {
            canScrollUpJs || canScrollVertically(-1)
        }
        return !canScroll
    }

    private fun dispatchUnconsumedToParent(dy: Int) {
        if (dy == 0) return
        scrollConsumed.fill(0)
        ViewCompat.dispatchNestedPreScroll(this, 0, dy, scrollConsumed, scrollOffset)
        val unconsumed = dy - scrollConsumed[1]
        if (unconsumed != 0) {
            ViewCompat.dispatchNestedScroll(this, 0, 0, 0, unconsumed, scrollOffset)
        }
    }

    private fun reportScrollState(event: MotionEvent) {
        val density = resources.displayMetrics.density
        evaluateJavascript(
            "window.__albyReportScroll&&window.__albyReportScroll(${event.x / density},${event.y / density})",
            null
        )
    }

    private fun ensureScrollBridge() {
        if (scrollBridgeInstalled) return
        addJavascriptInterface(ScrollBridge(), JS_INTERFACE)
        scrollBridgeInstalled = true
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

    private companion object {
        const val JS_INTERFACE = "albyNestedScroll"

        const val SCROLL_DETECTION_JS = """
            (function() {
              window.__albyReportScroll = function(x, y) {
                if (!window.albyNestedScroll) return;
                var el = document.elementFromPoint(x, y);
                while (el && el.shadowRoot) {
                  var inner = el.shadowRoot.elementFromPoint(x, y);
                  if (!inner || inner === el) break;
                  el = inner;
                }
                var up = false, down = false;
                while (el) {
                  var oy = getComputedStyle(el).overflowY;
                  var isRoot = el === document.scrollingElement ||
                    el === document.documentElement || el === document.body;
                  if ((isRoot || oy === 'auto' || oy === 'scroll' || oy === 'overlay') &&
                      el.scrollHeight > el.clientHeight + 1) {
                    up = up || el.scrollTop > 1;
                    down = down || el.scrollTop < el.scrollHeight - el.clientHeight - 1;
                  }
                  el = el.parentElement || (el.getRootNode && el.getRootNode().host) || null;
                }
                window.albyNestedScroll.update(up, down);
              };
            })();
        """
    }
}
