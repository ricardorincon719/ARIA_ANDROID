package com.ricardo.aria

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AriaMemory(
    val user: String,
    val reminders: List<String>,
    val lastConnection: String
)

class AriaMemoryStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AriaMemory {
        return AriaMemory(
            user = prefs.getString(KEY_USER, "") ?: "",
            reminders = loadReminders(),
            lastConnection = prefs.getString(KEY_LAST_CONNECTION, FIRST_CONNECTION) ?: FIRST_CONNECTION
        )
    }

    fun save(user: String, reminders: List<String>): String {
        val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val array = JSONArray()
        reminders.forEach { array.put(it) }

        prefs.edit()
            .putString(KEY_USER, user)
            .putString(KEY_REMINDERS, array.toString())
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

    private companion object {
        const val PREFS_NAME = "aria_memoria"
        const val KEY_USER = "usuario"
        const val KEY_REMINDERS = "tareas"
        const val KEY_LAST_CONNECTION = "ultima_conexion"
        const val FIRST_CONNECTION = "Primera vez"
    }
}
