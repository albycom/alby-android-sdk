package com.alby.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

object AlbySDK {
    private var _brandId: String? = null
    val brandId: String? get() = _brandId 
    private var isInitialized = false
    private val client = OkHttpClient()
    private const val analyticsEndpoint: String =
        "https://eks.alby.com/analytics-service/v1/api/track"
    private const val ALBY_CDN_URL = "https://cdn.alby.com"
    private var context: Context? = null
    // Initialization
    @SuppressLint("SetJavaScriptEnabled")
    fun initialize(brandId: String, context: Context) {
        if (isInitialized) {
            println("AlbySDK is already initialized.")
            return
        }

        this._brandId = brandId
        this.context = context.applicationContext  // Store application context


        // Create and load alby js in the background
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.layoutParams = ViewGroup.LayoutParams(0, 0)  // Zero size
        webView.visibility = View.GONE  // Ensure it's not visible
        webView.loadUrl("https://cdn.alby.com/assets/alby_widget.html?brandId=${brandId}")

        isInitialized = true
    }

    fun sendPurchasePixel(
        orderId: Any,
        orderTotal: Any,
        variantIds: List<Any>,
        currency: String
    ) {
        ensureInitialized()

        val orderInfo = mapOf(
            "brand_id" to brandId,
            "order_id" to orderId.toString(),
            "order_total" to orderTotal.toString(),
            "variant_ids" to variantIds.joinToString(",") { it.toString() },
            "currency" to currency
        )

        val queryString = orderInfo.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

        val userId = getUserId()
        val finalUrl = buildString {
            append("https://tr.alby.com/p?$queryString")
            userId?.let { append("&user_id=$it") }
        }

        CoroutineScope(Dispatchers.IO).launch {
            performRequest(finalUrl)
        }
    }

    fun sendAddToCartEvent(price: Any, variantId: String, currency: String, quantity: String) {
        ensureInitialized()

        val payload = JSONObject().apply {
            put("brand_id", brandId)
            put("event_type", "Click:AddToCart")
            put(
                "user_id",
                getUserId()
            )
            put("properties", JSONObject().apply {
                put("price", price.toString())
                put("variant_id", variantId)
                put("currency", currency)
                put("quantity", quantity)
            })
            put("context", JSONObject().apply {
                put("locale", "en-US")
                put("userAgent", System.getProperty("http.agent"))
                put("source", "android-sdk")
            })
            put("event_timestamp", System.currentTimeMillis() / 1000.0)
        }

        CoroutineScope(Dispatchers.IO).launch {
            performRequest(analyticsEndpoint, "POST", payload.toString())
        }
    }

    private fun getUserId(): String? {
        val cookies = CookieManager.getInstance().getCookie("https://cdn.alby.com")
        val cookieMap = parseCookies(cookies)
        return cookieMap["_alby_user"]
    }

    private fun performRequest(url: String, method: String = "GET", requestBody: String? = null) {
        val requestBuilder = Request.Builder().url(url)

        when (method.uppercase()) {
            "POST" -> {
                val body = requestBody?.toRequestBody("application/json".toMediaTypeOrNull())
                    ?: throw IllegalArgumentException("POST requests require a non-null request body")
                requestBuilder.post(body)
            }

            "GET" -> requestBuilder.get()
            else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println("alby Error: ${response.code}")
            } else {
                println("alby Success: ${response.code}")
            }
        }
    }

    private fun parseCookies(cookies: String?): Map<String, String> {
        return cookies?.split(";")?.associate { cookie ->
            val parts = cookie.split("=", limit = 2).map { it.trim() }
            parts[0] to (parts.getOrNull(1) ?: "")
        } ?: emptyMap()
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("AlbySDK has not been initialized. Please call `initialize(brandId)` first.")
        }

        if (brandId == null) {
            throw IllegalStateException("Missing brandId. Ensure `initialize` is called and brandId is set.")
        }
    }

    /**
     * Clears WebView data (cookies, cache, localStorage) specifically for Alby CDN.
     * This won't affect other domains' data in the WebView.
     */
    fun clearAlbyData(context: Context? = null) {
        val contextToUse = context ?: this.context ?: throw IllegalStateException("Context not found. Please provide a valid context.")

        CookieManager.getInstance().let { cookieManager ->
            // Remove cookies for Alby CDN domain
            cookieManager.getCookie(ALBY_CDN_URL)?.split(";")?.forEach { cookie ->
                val cookieName = cookie.split("=")[0].trim()
                cookieManager.setCookie(ALBY_CDN_URL, "$cookieName=; expires=Thu, 01 Jan 1970 00:00:00 GMT")
            }
            cookieManager.flush()
        }

        // Create temporary WebView to clear cache and localStorage
        WebView(contextToUse).apply {
            settings.javaScriptEnabled = true  // Required for localStorage access
            clearCache(true)
            
            // Clear localStorage for our domain
            loadUrl(ALBY_CDN_URL)
            evaluateJavascript("""
                localStorage.clear();
                sessionStorage.clear();
            """.trimIndent(), null)
            
            destroy()
        }
    }
}
