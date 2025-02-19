package com.alby.widget

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
    focusable: Boolean = false
) {
    val brandId = AlbySDK.brandId ?: throw IllegalStateException("AlbySDK not initialized")

    AndroidView(
        factory = { context ->
            WebView(context).apply {
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
                        // Inject JavaScript to remove padding and margin from the body and html elements
                        this@apply.loadUrl(
                            "javascript:(function() { " +
                                    "document.body.style.margin='0'; " +
                                    "document.body.style.padding='0'; " +
                                    "document.documentElement.style.margin='0'; " +
                                    "document.documentElement.style.padding='0'; " +
                                    "})()"
                        )
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

            webView.loadUrl(widgetUrl)
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