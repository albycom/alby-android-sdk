package com.alby.widget

import android.webkit.CookieManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class AlbyPurchasePixel {

    private val client = OkHttpClient()

    fun sendPurchasePixel(
        brandId: String,
        orderId: Any,
        orderTotal: Any,
        productIds: List<Any>,
        currency: String
    ) {
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

        val cookies = CookieManager.getInstance().getCookie("https://cdn.alby.com")
        val cookieMap = parseCookies(cookies)

        var finalUrl = "https://tr.alby.com/p?$queryString"

        cookieMap["_alby_user"]?.let {
            finalUrl += "&user_id=$it"
        }

        CoroutineScope(Dispatchers.IO).launch {
            performRequest(finalUrl)
        }
    }

    private fun performRequest(url: String) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response: okhttp3.Response->
            if (!response.isSuccessful) {
                println("alby Purchase Pixel Error: ${response.code}")
            } else {
                println("alby Purchase Pixel Response code: ${response.code}")
            }
        }
    }

    private fun parseCookies(cookies: String?): Map<String, String> {
        return cookies?.split(";")?.associate { cookie ->
            val parts = cookie.split("=", limit = 2).map { it.trim() }
            parts[0] to (parts.getOrNull(1) ?: "")
        } ?: emptyMap()
    }
}
