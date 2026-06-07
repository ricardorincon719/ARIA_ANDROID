package com.ricardo.aria

import android.content.Context

class PearlNativeSessionStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun token(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token.trim()).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun lastCoreUrl(): String = prefs.getString(KEY_LAST_CORE_URL, "") ?: ""

    fun saveLastCoreUrl(url: String) {
        val normalized = PearlCoreEndpoints.normalize(url)
        if (normalized.isNotBlank()) {
            prefs.edit().putString(KEY_LAST_CORE_URL, normalized).apply()
        }
    }

    private companion object {
        const val PREFS_NAME = "pearl_native_session"
        const val KEY_TOKEN = "token"
        const val KEY_LAST_CORE_URL = "last_core_url"
    }
}
