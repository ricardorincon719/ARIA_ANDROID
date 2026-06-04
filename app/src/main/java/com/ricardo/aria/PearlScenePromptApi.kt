package com.ricardo.aria

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object PearlScenePromptApi {
    private val DEFAULT_HUB_URLS = listOf(
        "http://jarvis-node.local:5006",
        "http://jarvis.node.local:5006"
    )
    private const val CONNECT_TIMEOUT_MS = 2_500
    private const val READ_TIMEOUT_MS = 4_000

    fun fetchPendingPrompts(hubUrls: List<String> = DEFAULT_HUB_URLS): List<PearlScenePrompt> =
        withHubFallback(hubUrls) { hubUrl ->
            val connection = openConnection("$hubUrl/api/v1/scene-prompts/pending", "GET")
            try {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parsePendingPrompts(response)
            } finally {
                connection.disconnect()
            }
        }

    fun sendDecision(
        promptId: String,
        decision: String,
        idempotencyKey: String,
        hubUrls: List<String> = DEFAULT_HUB_URLS
    ) {
        withHubFallback(hubUrls) { hubUrl ->
            val connection = openConnection(
                "$hubUrl/api/v1/scene-prompts/${promptId.encodePathSegment()}/decision",
                "POST"
            )
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.doOutput = true
            try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(decisionPayload(decision, idempotencyKey))
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

    private fun <T> withHubFallback(hubUrls: List<String>, request: (String) -> T): T {
        var lastException: Exception? = null
        hubUrls.forEach { hubUrl ->
            try {
                return request(hubUrl)
            } catch (exception: Exception) {
                lastException = exception
            }
        }
        throw lastException ?: IllegalStateException("No PEARL Hub URL configured")
    }

    private fun openConnection(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }

    private fun String.encodePathSegment(): String =
        java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
