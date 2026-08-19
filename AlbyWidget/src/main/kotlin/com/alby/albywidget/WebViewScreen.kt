package com.alby.widget

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun WebViewScreen(
    javascriptInterface: Any,
    webViewReference: MutableState<WebView?>,
    productId: String,
    widgetId: String? = null,
    variantId: String? = null,
    component: String? = "alby-mobile-generative-qa",
    threadId: String? = null,
    testId: String? = null,
    testVersion: String? = null,
    testDescription: String? = null,
    focusable: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val brandId = AlbySDK.brandId ?: throw IllegalStateException("AlbySDK not initialized")
    val nestedDispatcher = remember { NestedScrollDispatcher() }
    val nestedConnection = remember { object : NestedScrollConnection {} }

    fun dispatchComposeScroll(dy: Int): Float {
        if (dy == 0) return 0f
        val available = Offset(0f, dy.toFloat())
        val preConsumed = nestedDispatcher.dispatchPreScroll(available, NestedScrollSource.Drag)
        val remaining = available - preConsumed
        val unconsumed = if (remaining.y != 0f) {
            nestedDispatcher.dispatchPostScroll(preConsumed, remaining, NestedScrollSource.Drag)
        } else {
            remaining
        }
        return available.y - unconsumed.y
    }

    AndroidView(
        modifier = modifier.nestedScroll(nestedConnection, nestedDispatcher),
        factory = { context ->
            NestedWebView(context).apply {
                composeScrollBy = ::dispatchComposeScroll
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest
                    ): Boolean {
                        // Open the URL in an external browser
                        url?.let {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            context.startActivity(intent)
                        }
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(
                            "document.body.style.margin='0';" +
                                "document.body.style.padding='0';" +
                                "document.documentElement.style.margin='0';" +
                                "document.documentElement.style.padding='0';",
                            null
                        )
                        (view as? NestedWebView)?.installScrollDetection()
                    }
                }
                isFocusable = focusable
                isFocusableInTouchMode = focusable
                setOverScrollMode(WebView.OVER_SCROLL_NEVER);

                settings.loadWithOverviewMode = false
                settings.useWideViewPort = true
                settings.setSupportZoom(false)
                addJavascriptInterface(javascriptInterface, "appInterface")
                webViewReference.value = this
            }
        },
        update = { webView ->
            (webView as NestedWebView).composeScrollBy = ::dispatchComposeScroll
            val widgetUrl = buildWidgetUrl(
                brandId, productId, widgetId, variantId, component,
                threadId, testId, testVersion, testDescription
            )
            if (!isSameWidgetUrl(webView.url, widgetUrl)) {
                webView.loadUrl(widgetUrl)
            }
        }
    )
}

private fun buildWidgetUrl(
    brandId: String,
    productId: String,
    widgetId: String?,
    variantId: String?,
    component: String?,
    threadId: String?,
    testId: String?,
    testVersion: String?,
    testDescription: String?,
): String {
    val builder = Uri.parse("https://cdn.alby.com/assets/alby_widget.html").buildUpon()
        .appendQueryParameter("brandId", brandId)
        .appendQueryParameter("productId", productId)
        .appendQueryParameter("component", component ?: "alby-mobile-generative-qa")
    if (variantId != null) builder.appendQueryParameter("variantId", variantId)
    if (widgetId != null) builder.appendQueryParameter("widgetId", widgetId)
    if (threadId != null) builder.appendQueryParameter("threadId", threadId)
    if (testId != null) builder.appendQueryParameter("testId", testId)
    if (testVersion != null) builder.appendQueryParameter("testVersion", testVersion)
    if (testDescription != null) builder.appendQueryParameter("testDescription", testDescription)
    return builder.build().toString()
}

private fun isSameWidgetUrl(loaded: String?, target: String): Boolean {
    if (loaded.isNullOrEmpty()) return false
    if (loaded == target) return true
    val a = Uri.parse(loaded)
    val b = Uri.parse(target)
    if (a.scheme != b.scheme || a.authority != b.authority || a.path != b.path) return false
    val names = a.queryParameterNames
    if (names != b.queryParameterNames) return false
    return names.all { a.getQueryParameter(it) == b.getQueryParameter(it) }
}

fun publishEvent(webView: WebView?, event: String) {
    val escapedEvent = event
        .replace("\\", "\\\\") // Escape backslashes
        .replace("\"", "\\\"") // Escape double quotes
        .replace("\n", "\\n")

    val js =
        "var event = new CustomEvent('albyNativeEvent', { detail: { data: '${escapedEvent}'}}); window.dispatchEvent(event);"
    Log.d("event", js);
    webView?.evaluateJavascript(js) {
        Log.d("widget", it);
    }
}