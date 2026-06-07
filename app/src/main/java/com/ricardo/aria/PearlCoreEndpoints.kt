package com.ricardo.aria

import android.content.Context
import java.net.URI

object PearlCoreEndpoints {
    private val DEFAULT_CORE_URLS = listOf(
        "http://jarvis.node.local:5004",
        "http://jarvis-node.local:5004",
        "http://127.0.0.1:5004",
        "https://pliable-rely-recital.ngrok-free.dev"
    )

    fun urls(context: Context): List<String> {
        val lastGood = PearlNativeSessionStore(context).lastCoreUrl()
        return (listOf(lastGood) + DEFAULT_CORE_URLS)
            .map(::normalize)
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun normalize(url: String): String = url.trim().trimEnd('/')

    fun originFromUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        return try {
            val uri = URI(url)
            val scheme = uri.scheme ?: return ""
            val host = uri.host ?: return ""
            val port = if (uri.port > 0) ":${uri.port}" else ""
            "$scheme://$host$port"
        } catch (_: Exception) {
            ""
        }
    }
}
