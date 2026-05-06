package com.ricardo.aria

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AriaCommandProcessor(
    private val clock: () -> Date = { Date() },
    private val locale: Locale = Locale.getDefault(),
    private val timeZone: TimeZone = TimeZone.getDefault()
) {
    fun process(
        input: String,
        user: String,
        reminders: MutableList<String>,
        onMemoryChanged: () -> Unit
    ): String {
        val command = input.lowercase(locale).trim()

        return when {
            command == "hola" -> {
                "¡Hola $user! ¿Cómo puedo ayudarte hoy?"
            }

            command == "hora" -> {
                val time = SimpleDateFormat("HH:mm", locale).apply {
                    timeZone = this@AriaCommandProcessor.timeZone
                }.format(clock())
                "Son las $time horas."
            }

            command == "fecha" -> {
                val date = SimpleDateFormat("dd/MM/yyyy", locale).apply {
                    timeZone = this@AriaCommandProcessor.timeZone
                }.format(clock())
                "Hoy es $date."
            }

            command == "presentate" -> {
                "Soy ARIA, tu asistente personal. Tengo ${reminders.size} recordatorio(s) en memoria."
            }

            command.startsWith("recordar") -> {
                val task = input.drop(input.indexOf("recordar", ignoreCase = true) + RECORDAR.length).trim()

                if (task.isBlank()) {
                    "¿Qué querés que recuerde?"
                } else {
                    reminders.add(task)
                    onMemoryChanged()
                    "He recordado: '$task'. Tenés ${reminders.size} recordatorio(s)."
                }
            }

            command == "recordatorios" -> {
                if (reminders.isEmpty()) {
                    "No tenés recordatorios pendientes."
                } else {
                    "Tenés ${reminders.size} recordatorio(s) guardado(s)."
                }
            }

            command.startsWith("olvidar") -> {
                val numberText = command.removePrefix("olvidar").trim()
                val number = numberText.toIntOrNull()

                if (number == null) {
                    "Decime el número del recordatorio. Ejemplo: olvidar 2"
                } else if (number < 1 || number > reminders.size) {
                    "Número inválido. Usá un número del 1 al ${reminders.size}."
                } else {
                    val removed = reminders.removeAt(number - 1)
                    onMemoryChanged()
                    "Eliminado: '$removed'. Te quedan ${reminders.size} recordatorio(s)."
                }
            }

            command == "ayuda" -> HELP_TEXT

            command == "salir" -> {
                onMemoryChanged()
                "Memoria guardada. Hasta luego $user."
            }

            else -> {
                "No entendí '$input'. Escribí 'ayuda' para ver los comandos."
            }
        }
    }

    private companion object {
        const val RECORDAR = "recordar"

        val HELP_TEXT = """
            Comandos disponibles:
            hola
            hora
            fecha
            presentate
            recordar comprar pan
            recordatorios
            olvidar 1
            ayuda
            salir
            """.trimIndent()
    }
}
