package com.alby.widget

import android.webkit.CookieManager
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
    private var brandId: String? = null
    private var isInitialized = false
    private val client = OkHttpClient()
    private val analyticsEndpoint = "https://eks.alby.com/analytics-service/v1/api/track"

    // Initialization
    fun initialize(brandId: String) {
        if (isInitialized) {
            println("AlbySDK is already initialized.")
            return
        }

        this.brandId = brandId
        isInitialized = true
    }

    fun sendPurchasePixel(
        orderId: Any,
        orderTotal: Any,
        productIds: List<Any>,
        currency: String
    ) {
        ensureInitialized()

        val orderInfo = mapOf(
            "brand_id" to brandId,
            "order_id" to orderId.toString(),
            "order_total" to orderTotal.toString(),
            "product_ids" to productIds.joinToString(",") { it.toString() },
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

    fun sendAddToCartEvent(price: String, variantId: String, currency: String, quantity: String) {
        ensureInitialized()

        val payload = JSONObject().apply {
            put("brand_id", brandId)
            put("event_type", "Click:AddToCart")
            put(
                "user_id",
                getUserId()
            )
            put("properties", JSONObject().apply {
                put("price", price)
                put("variant_id", variantId)
                put("currency", currency)
                put("quantity", quantity)
            })
            put("context", JSONObject().apply {
                put("locale", "en-US")
                put("userAgent", System.getProperty("http.agent"))
                put("source", "android")
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
}
