package com.ricardo.aria

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AriaCommandProcessor(
    private val clock: () -> Date = { Date() },
    private val locale: Locale = Locale.getDefault(),
    private val timeZone: TimeZone = TimeZone.getDefault(),
    private val router: AriaCommandRouter = AriaCommandRouter(locale)
) {
    fun process(
        input: String,
        user: String,
        reminders: MutableList<String>,
        onMemoryChanged: () -> Unit
    ): String {
        return when (val intent = router.route(input)) {
            AriaIntent.Greeting -> {
                "¡Hola $user! ¿Cómo puedo ayudarte hoy?"
            }

            AriaIntent.CurrentTime -> {
                val time = SimpleDateFormat("HH:mm", locale).apply {
                    timeZone = this@AriaCommandProcessor.timeZone
                }.format(clock())
                "Son las $time horas."
            }

            AriaIntent.CurrentDate -> {
                val date = SimpleDateFormat("dd/MM/yyyy", locale).apply {
                    timeZone = this@AriaCommandProcessor.timeZone
                }.format(clock())
                "Hoy es $date."
            }

            AriaIntent.Introduction -> {
                "Soy ARIA, tu asistente personal. Tengo ${reminders.size} recordatorio(s) en memoria."
            }

            is AriaIntent.AddReminder -> {
                if (intent.text.isBlank()) {
                    "¿Qué querés que recuerde?"
                } else {
                    reminders.add(intent.text)
                    onMemoryChanged()
                    "He recordado: '${intent.text}'. Tenés ${reminders.size} recordatorio(s)."
                }
            }

            AriaIntent.ListReminders -> {
                if (reminders.isEmpty()) {
                    "No tenés recordatorios pendientes."
                } else {
                    "Tenés ${reminders.size} recordatorio(s) guardado(s)."
                }
            }

            is AriaIntent.RemoveReminder -> {
                if (intent.index == null) {
                    "Decime el número del recordatorio. Ejemplo: olvidar 2"
                } else if (intent.index < 1 || intent.index > reminders.size) {
                    "Número inválido. Usá un número del 1 al ${reminders.size}."
                } else {
                    val removed = reminders.removeAt(intent.index - 1)
                    onMemoryChanged()
                    "Eliminado: '$removed'. Te quedan ${reminders.size} recordatorio(s)."
                }
            }

            AriaIntent.Help -> HELP_TEXT

            AriaIntent.Exit -> {
                onMemoryChanged()
                "Memoria guardada. Hasta luego $user."
            }

            is AriaIntent.Unknown -> {
                "No entendí '${intent.input}'. Escribí 'ayuda' para ver los comandos."
            }
        }
    }

    private companion object {
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
