package com.ricardo.aria

import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object PearlScenePromptApi {
    private const val DEFAULT_HUB_URL = "http://jarvis-node.local:5006"
    private const val CONNECT_TIMEOUT_MS = 2_500
    private const val READ_TIMEOUT_MS = 4_000

    fun fetchPendingPrompts(hubUrl: String = DEFAULT_HUB_URL): List<PearlScenePrompt> {
        val connection = openConnection("$hubUrl/api/v1/scene-prompts/pending", "GET")
        return try {
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
        hubUrl: String = DEFAULT_HUB_URL
    ) {
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

    private fun openConnection(url: String, method: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }

    private fun String.encodePathSegment(): String =
        java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
