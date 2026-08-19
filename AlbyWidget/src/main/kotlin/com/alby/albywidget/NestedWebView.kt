package com.alby.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewParent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.AbsListView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Keep vertical drags on the widget when the chat or the WebView document can
 * still scroll (the document often moves the input into view). A new gesture
 * that starts at the top/bottom of both, or on an empty chat with no document
 * overflow, is passed to the host page. Reaching an edge mid-gesture does not
 * hand off.
 */
@SuppressLint("JavascriptInterface")
internal class NestedWebView(context: Context) : WebView(context) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    @Volatile private var hasOverflow = true
    @Volatile private var canScrollUp = true
    @Volatile private var canScrollDown = true
    @Volatile private var jsReported = false

    private var gestureLocked = false
    private var passThisGesture = false
    private var handedOffToParent = false
    private var startY = 0f
    private var lastRawY = 0f
    var composeScrollBy: ((Int) -> Unit)? = null

    init {
        addJavascriptInterface(ScrollBridge(), JS_INTERFACE)
    }

    fun installScrollDetection() {
        jsReported = false
        evaluateJavascript(SCROLLABLE_JS, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                lastRawY = event.rawY
                gestureLocked = false
                passThisGesture = false
                handedOffToParent = false
                requestParentsDisallowIntercept(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = lastRawY - event.rawY
                lastRawY = event.rawY
                if (!gestureLocked && jsReported && abs(event.y - startY) > touchSlop) {
                    gestureLocked = true
                    passThisGesture = shouldPassToParent(startY - event.y)
                }
                if (gestureLocked && passThisGesture) {
                    if (!handedOffToParent) {
                        handedOffToParent = true
                        cancelWebViewGesture(event)
                    }
                    passToParent(dy.roundToInt())
                    requestParentsDisallowIntercept(true)
                    return true
                }
                requestParentsDisallowIntercept(true)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                requestParentsDisallowIntercept(false)
                if (handedOffToParent) return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun shouldPassToParent(dy: Float): Boolean {
        if (dy == 0f) return false
        if (documentCanScroll(dy)) return false
        if (!hasOverflow) return true
        return if (dy > 0) !canScrollDown else !canScrollUp
    }

    private fun documentCanScroll(dy: Float): Boolean {
        val range = computeVerticalScrollRange()
        val extent = computeVerticalScrollExtent()
        val offset = computeVerticalScrollOffset()
        val maxOffset = range - extent
        if (maxOffset <= DOCUMENT_SCROLL_SLOP) return false
        return if (dy > 0) offset < maxOffset - DOCUMENT_SCROLL_SLOP else offset > DOCUMENT_SCROLL_SLOP
    }

    private fun cancelWebViewGesture(event: MotionEvent) {
        val cancel = MotionEvent.obtain(event)
        cancel.action = MotionEvent.ACTION_CANCEL
        super.onTouchEvent(cancel)
        cancel.recycle()
        evaluateJavascript("window.getSelection&&window.getSelection().removeAllRanges()", null)
    }

    private fun passToParent(dy: Int) {
        if (dy == 0) return
        val scroller = findViewScroller()
        if (scroller != null) {
            scroller.scrollBy(0, dy)
            return
        }
        composeScrollBy?.invoke(dy)
    }

    private fun findViewScroller(): View? {
        var ancestor = parent
        while (ancestor != null) {
            when (ancestor) {
                is NestedScrollView, is ScrollView, is AbsListView -> return ancestor as View
                is View -> if (ancestor.javaClass.name.contains("RecyclerView")) return ancestor
            }
            ancestor = ancestor.parent
        }
        return null
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
        fun update(hasOverflowScroller: Boolean, scrollUp: Boolean, scrollDown: Boolean) {
            hasOverflow = hasOverflowScroller
            canScrollUp = scrollUp
            canScrollDown = scrollDown
            jsReported = true
        }
    }

    private companion object {
        const val JS_INTERFACE = "albyNestedScroll"
        const val DOCUMENT_SCROLL_SLOP = 8

        const val SCROLLABLE_JS = """
            (function() {
              if (window.__albyScrollInstalled) return;
              window.__albyScrollInstalled = true;
              function scrollerInfo(el) {
                if (!el || el.nodeType !== 1) return null;
                if (el === document.documentElement || el === document.body) return null;
                var oy;
                try { oy = getComputedStyle(el).overflowY; } catch (err) { return null; }
                if (oy !== 'auto' && oy !== 'scroll' && oy !== 'overlay') return null;
                if (el.scrollHeight <= el.clientHeight + 1) return null;
                return {
                  up: el.scrollTop > 1,
                  down: el.scrollTop < el.scrollHeight - el.clientHeight - 1
                };
              }
              function documentInfo() {
                var el = document.scrollingElement || document.documentElement;
                if (!el || el.scrollHeight <= el.clientHeight + 8) return null;
                return {
                  up: el.scrollTop > 8,
                  down: el.scrollTop < el.scrollHeight - el.clientHeight - 8
                };
              }
              function report(e) {
                if (!window.albyNestedScroll) return;
                var has = false, up = false, down = false;
                if (e && e.composedPath) {
                  var path = e.composedPath();
                  for (var i = 0; i < path.length; i++) {
                    var info = scrollerInfo(path[i]);
                    if (info) {
                      has = true;
                      up = up || info.up;
                      down = down || info.down;
                    }
                  }
                }
                var doc = documentInfo();
                if (doc) {
                  up = up || doc.up;
                  down = down || doc.down;
                }
                window.albyNestedScroll.update(has, up, down);
              }
              document.addEventListener('touchstart', report, {capture: true, passive: true});
              document.addEventListener('touchmove', report, {capture: true, passive: true});
            })();
        """
    }
}
