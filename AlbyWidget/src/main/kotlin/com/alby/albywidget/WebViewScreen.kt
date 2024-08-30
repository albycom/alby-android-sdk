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
fun WebViewScreen(javascriptInterface: Any, webViewReference: MutableState<WebView?>) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
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
            webView.loadUrl("https://cdn.alby.com/assets/alby_widget.html")
            webView.setBackgroundColor(Color.White.toArgb())
        }
    )
}

fun publishEvent(webView: WebView?, event: String) {
    val js =
        "var event = new CustomEvent('iosEvent', { detail: { data: '${event}'}}); window.dispatchEvent(event);"
    webView?.evaluateJavascript(js) {
        Log.d("widget", it);
    }
}