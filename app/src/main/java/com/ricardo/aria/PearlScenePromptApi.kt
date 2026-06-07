package com.ricardo.aria

import android.content.Context
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

object PearlScenePromptApi {
    private const val CONNECT_TIMEOUT_MS = 2_500
    private const val READ_TIMEOUT_MS = 4_000

    fun fetchPendingPrompts(
        context: Context,
        coreUrls: List<String> = PearlCoreEndpoints.urls(context)
    ): List<PearlScenePrompt> {
        if (PearlNativeSessionStore(context).token().isBlank()) return emptyList()
        return withCoreFallback(context, coreUrls) { coreUrl ->
            val path = "/api/v1/scene-prompts/pending"
            val connection = openConnection("$coreUrl$path", "GET")
            applySignedHeaders(context, connection, "GET", path, "")
            try {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parsePendingPrompts(response)
            } finally {
                connection.disconnect()
            }
        }
    }

    fun sendDecision(
        context: Context,
        promptId: String,
        decision: String,
        idempotencyKey: String,
        coreUrls: List<String> = PearlCoreEndpoints.urls(context)
    ) {
        if (PearlNativeSessionStore(context).token().isBlank()) {
            throw IllegalStateException("PEARL Core session is not available")
        }
        withCoreFallback(context, coreUrls) { coreUrl ->
            val path = "/api/v1/scene-prompts/${promptId.encodePathSegment()}/decision"
            val body = decisionPayload(decision, idempotencyKey)
            val connection = openConnection("$coreUrl$path", "POST")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            applySignedHeaders(context, connection, "POST", path, body)
            connection.doOutput = true
            try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
                connection.inputStream.close()
            } finally {
                connection.disconnect()
            }
        }
    }

    fun parsePendingPrompts(body: String): List<PearlScenePrompt> {
        val root = JSONObject(body)
        val prompts = root.optJSONArray("prompts") ?: return emptyList()
        return buildList {
            for (index in 0 until prompts.length()) {
                val prompt = prompts.optJSONObject(index) ?: continue
                val id = prompt.optString("id").trim()
                if (id.isEmpty()) continue
                add(
                    PearlScenePrompt(
                        id = id,
                        title = prompt.optString("title", "PEARL detectó una escena"),
                        message = prompt.optString(
                            "message",
                            "Hay una escena aprendida pendiente de aprobación."
                        )
                    )
                )
            }
        }
    }

    fun decisionPayload(decision: String, idempotencyKey: String): String =
        JSONObject()
            .put("decision", decision)
            .put("idempotency_key", idempotencyKey)
            .toString()

    private fun applySignedHeaders(
        context: Context,
        connection: HttpURLConnection,
        method: String,
        pathWithQuery: String,
        body: String
    ) {
        val store = PearlNativeSessionStore(context)
        val identity = PearlDeviceIdentity.get(context)
        val timestamp = (System.currentTimeMillis() / 1000L).toString()
        val nonce = UUID.randomUUID().toString()
        val bodyHash = sha256Hex(body.toByteArray(Charsets.UTF_8))
        val signingPayload = listOf(
            method.uppercase(),
            pathWithQuery,
            timestamp,
            nonce,
            bodyHash
        ).joinToString("\n")

        connection.setRequestProperty("Authorization", store.token())
        connection.setRequestProperty("X-PEARL-Device-Id", identity.deviceId)
        connection.setRequestProperty("X-PEARL-Timestamp", timestamp)
        connection.setRequestProperty("X-PEARL-Nonce", nonce)
        connection.setRequestProperty("X-PEARL-Body-SHA256", bodyHash)
        connection.setRequestProperty("X-PEARL-Signature", PearlDeviceIdentity.sign(signingPayload))
    }

    private fun <T> withCoreFallback(
        context: Context,
        coreUrls: List<String>,
        request: (String) -> T
    ): T {
        var lastException: Exception? = null
        coreUrls.forEach { coreUrl ->
            try {
                val normalized = PearlCoreEndpoints.normalize(coreUrl)
                val result = request(normalized)
                PearlNativeSessionStore(context).saveLastCoreUrl(normalized)
                return result
            } catch (exception: Exception) {
                lastException = exception
            }
        }
        throw lastException ?: IllegalStateException("No PEARL Core URL configured")
    }

    private fun openConnection(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }

    private fun String.encodePathSegment(): String =
        java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
}
