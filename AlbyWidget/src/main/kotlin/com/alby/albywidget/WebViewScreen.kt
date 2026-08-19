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

    fun dispatchComposeScroll(dy: Int) {
        if (dy == 0) return
        val available = Offset(0f, dy.toFloat())
        val consumed = nestedDispatcher.dispatchPreScroll(available, NestedScrollSource.Drag)
        val remaining = available - consumed
        if (remaining.y != 0f) {
            nestedDispatcher.dispatchPostScroll(consumed, remaining, NestedScrollSource.Drag)
        }
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
            var widgetUrl =
                "https://cdn.alby.com/assets/alby_widget.html?brandId=${brandId}&productId=${productId}&component=${component}"
            if (variantId != null) {
                widgetUrl += "&variantId=${variantId}"
            }
            if (widgetId != null) {
                widgetUrl += "&widgetId=${widgetId}"
            }
            if (threadId != null) {
                widgetUrl += "&threadId=${threadId}"
            }
            if (testId != null) {
                widgetUrl += "&testId=${testId}"
            }
            if (testVersion != null) {
                widgetUrl += "&testVersion=${testVersion}"
            }
            if (testDescription != null) {
                widgetUrl += "&testDescription=${testDescription}"
            }

            if (webView.url != widgetUrl) {
                webView.loadUrl(widgetUrl)
            }
        }
    )
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