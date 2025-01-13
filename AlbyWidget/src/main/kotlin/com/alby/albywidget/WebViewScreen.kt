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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun WebViewScreen(
    javascriptInterface: Any,
    webViewReference: MutableState<WebView?>,
    brandId: String,
    productId: String,
    widgetId: String? = null,
    variantId: String? = null,
    component: String? = "alby-mobile-generative-qa"
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
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
                isFocusable = false
                isFocusableInTouchMode = false
                settings.loadWithOverviewMode = false
                settings.useWideViewPort = true
                settings.setSupportZoom(false)
                settings.domStorageEnabled = true
                addJavascriptInterface(javascriptInterface, "appInterface")
                webViewReference.value = this
            }
        },
        update = { webView ->
            var widgetUrl = "https://cdn.alby.com/assets/alby_widget.html?brandId=${brandId}&productId=${productId}&component=${component}&useBrandStyling=false"
            if (variantId != null) {
                widgetUrl += "&variantId=${variantId}"
            }
            if (widgetId != null) {
                widgetUrl += "&widgetId=${widgetId}"
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