package com.ricardo.aria

import java.util.Locale

class AriaCommandRouter(
    private val locale: Locale = Locale.getDefault()
) {
    fun route(input: String): AriaIntent {
        val original = input.trim()
        val normalized = original.lowercase(locale)

        return when {
            normalized == "hola" || normalized.startsWith("buenas") -> AriaIntent.Greeting
            normalized == "hora" || ("hora" in normalized && isQuestion(normalized)) -> AriaIntent.CurrentTime
            normalized == "fecha" || ("fecha" in normalized && isQuestion(normalized)) -> AriaIntent.CurrentDate
            normalized == "presentate" || "quien eres" in normalized || "quién eres" in normalized -> AriaIntent.Introduction
            normalized.startsWith(RECORDAR) -> AriaIntent.AddReminder(extractAfterKeyword(original, RECORDAR))
            normalized == "recordatorios" || asksForReminders(normalized) -> AriaIntent.ListReminders
            normalized.startsWith(OLVIDAR) -> AriaIntent.RemoveReminder(extractAfterKeyword(normalized, OLVIDAR).toIntOrNull())
            normalized == "ayuda" || "que puedo decir" in normalized || "qué puedo decir" in normalized -> AriaIntent.Help
            isExit(normalized) -> AriaIntent.Exit
            else -> AriaIntent.Unknown(original)
        }
    }

    private fun isExit(text: String): Boolean {
        return text == "salir" ||
            text == "adios" ||
            text == "adiós" ||
            text == "chau" ||
            text == "chao" ||
            text == "hasta luego" ||
            text == "nos vemos"
    }

    private fun asksForReminders(text: String): Boolean {
        val mentionsReminder = "recordatorio" in text || "pendiente" in text || "tarea" in text
        val asksToList = isQuestion(text) ||
            "lista" in text ||
            "listar" in text ||
            "mostrar" in text ||
            "muestra" in text ||
            "tengo" in text

        return mentionsReminder && asksToList
    }

    private fun isQuestion(text: String): Boolean {
        return "?" in text ||
            "que " in text ||
            "qué " in text ||
            "cual " in text ||
            "cuál " in text ||
            "cuando " in text ||
            "cuándo " in text
    }

    private fun extractAfterKeyword(input: String, keyword: String): String {
        val index = input.indexOf(keyword, ignoreCase = true)
        return if (index == -1) {
            ""
        } else {
            input.drop(index + keyword.length).trim()
        }
    }

    private companion object {
        const val RECORDAR = "recordar"
        const val OLVIDAR = "olvidar"
    }
}
