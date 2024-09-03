package com.alby.widget

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
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
    variantId: String? = null
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {

                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()

                settings.loadWithOverviewMode = false
                settings.useWideViewPort = true
                settings.setSupportZoom(false)
                settings.domStorageEnabled = true;
                addJavascriptInterface(javascriptInterface, "appInterface")
                webViewReference.value = this;
            }
        },
        update = { webView ->
            var widgetUrl = "https://cdn.alby.com/assets/alby_widget.html?brandId=${brandId}&productId=${productId}"
            if (variantId != null) {
                widgetUrl += "&variantId=${variantId}"
            }

            webView.loadUrl(widgetUrl)
            webView.setBackgroundColor(Color.White.toArgb())
        }
    )
}

fun publishEvent(webView: WebView?, event: String) {
    val escapedEvent = event
        .replace("\\", "\\\\") // Escape backslashes
        .replace("\"", "\\\"") // Escape double quotes
        .replace("\n", "\\n")

    val js =
        "var event = new CustomEvent('iosEvent', { detail: { data: '${escapedEvent}'}}); window.dispatchEvent(event);"
    Log.d("message", js);
    webView?.evaluateJavascript(js) {
        Log.d("widget", it);
    }
}