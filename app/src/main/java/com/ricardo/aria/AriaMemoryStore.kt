package com.ricardo.aria

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AriaMemory(
    val user: String,
    val reminders: List<String>,
    val chatMessages: List<AriaChatMessage>,
    val lastConnection: String
)

class AriaMemoryStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AriaMemory {
        return AriaMemory(
            user = prefs.getString(KEY_USER, "") ?: "",
            reminders = loadReminders(),
            chatMessages = loadChatMessages(),
            lastConnection = prefs.getString(KEY_LAST_CONNECTION, FIRST_CONNECTION) ?: FIRST_CONNECTION
        )
    }

    fun save(
        user: String,
        reminders: List<String>,
        chatMessages: List<AriaChatMessage> = emptyList()
    ): String {
        val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val reminderArray = JSONArray()
        reminders.forEach { reminderArray.put(it) }

        prefs.edit()
            .putString(KEY_USER, user)
            .putString(KEY_REMINDERS, reminderArray.toString())
            .putString(KEY_CHAT_MESSAGES, serializeChatMessages(chatMessages.takeLast(MAX_CHAT_MESSAGES)))
            .putString(KEY_LAST_CONNECTION, currentDate)
            .apply()

        return currentDate
    }

    private fun loadReminders(): List<String> {
        val json = prefs.getString(KEY_REMINDERS, "[]") ?: "[]"

        return try {
            val array = JSONArray(json)
            List(array.length()) { index -> array.getString(index) }
        } catch (_: JSONException) {
            prefs.edit().putString(KEY_REMINDERS, "[]").apply()
            emptyList()
        }
    }

    private fun loadChatMessages(): List<AriaChatMessage> {
        val json = prefs.getString(KEY_CHAT_MESSAGES, "[]") ?: "[]"

        return try {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                val role = item.getString(CHAT_ROLE)
                val text = item.getString(CHAT_TEXT)

                when (role) {
                    CHAT_ROLE_USER -> AriaChatMessage.SentByUser(text)
                    CHAT_ROLE_ARIA -> AriaChatMessage.SentByAria(text)
                    else -> null
                }
            }.filterNotNull()
        } catch (_: JSONException) {
            prefs.edit().putString(KEY_CHAT_MESSAGES, "[]").apply()
            emptyList()
        }
    }

    private fun serializeChatMessages(messages: List<AriaChatMessage>): String {
        val array = JSONArray()

        messages.forEach { message ->
            val role = when (message) {
                is AriaChatMessage.SentByUser -> CHAT_ROLE_USER
                is AriaChatMessage.SentByAria -> CHAT_ROLE_ARIA
            }

            array.put(
                JSONObject()
                    .put(CHAT_ROLE, role)
                    .put(CHAT_TEXT, message.text)
            )
        }

        return array.toString()
    }

    private companion object {
        const val PREFS_NAME = "aria_memoria"
        const val KEY_USER = "usuario"
        const val KEY_REMINDERS = "tareas"
        const val KEY_CHAT_MESSAGES = "mensajes"
        const val KEY_LAST_CONNECTION = "ultima_conexion"
        const val FIRST_CONNECTION = "Primera vez"
        const val CHAT_ROLE = "role"
        const val CHAT_TEXT = "text"
        const val CHAT_ROLE_USER = "user"
        const val CHAT_ROLE_ARIA = "aria"
        const val MAX_CHAT_MESSAGES = 80
    }
}
