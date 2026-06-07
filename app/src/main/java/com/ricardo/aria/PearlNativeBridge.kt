package com.ricardo.aria

import android.content.Context
import android.webkit.JavascriptInterface
import org.json.JSONObject

class PearlNativeBridge(context: Context) {
    private val appContext = context.applicationContext
    private val sessionStore = PearlNativeSessionStore(appContext)

    @JavascriptInterface
    fun getDeviceIdentity(): String {
        val identity = PearlDeviceIdentity.get(appContext)
        return JSONObject()
            .put("device_id", identity.deviceId)
            .put("device_name", identity.deviceName)
            .put("device_public_key", identity.publicKey)
            .toString()
    }

    @JavascriptInterface
    fun onSessionToken(token: String) {
        if (token.isBlank()) {
            sessionStore.clearToken()
        } else {
            sessionStore.saveToken(token)
        }
    }

    @JavascriptInterface
    fun onCoreUrlLoaded(url: String) {
        sessionStore.saveLastCoreUrl(url)
    }
}
