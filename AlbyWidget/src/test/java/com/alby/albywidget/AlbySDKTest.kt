package com.alby.widget

import org.junit.Test

class AlbySDKTest {
    @Test
    fun performRequest_doesNotThrowOnNetworkFailure() {
        AlbySDK.performRequest("http://127.0.0.1:1/track/widget", "POST", "{}")
        AlbySDK.performRequest("http://127.0.0.1:1/p")
    }
}
