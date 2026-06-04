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
        return processDetailed(
            input = input,
            user = user,
            reminders = reminders,
            onMemoryChanged = onMemoryChanged
        ).response
    }

    fun processDetailed(
        input: String,
        user: String,
        reminders: MutableList<String>,
        onMemoryChanged: () -> Unit
    ): AriaCommandResult {
        return when (val intent = router.route(input)) {
            AriaIntent.Greeting -> {
                AriaCommandResult("¡Hola $user! ¿Cómo puedo ayudarte hoy?")
            }

            AriaIntent.CurrentTime -> {
                val time = SimpleDateFormat("HH:mm", locale).apply {
                    timeZone = this@AriaCommandProcessor.timeZone
                }.format(clock())
                AriaCommandResult("Son las $time horas.")
            }

            AriaIntent.CurrentDate -> {
                val date = SimpleDateFormat("dd/MM/yyyy", locale).apply {
                    timeZone = this@AriaCommandProcessor.timeZone
                }.format(clock())
                AriaCommandResult("Hoy es $date.")
            }

            AriaIntent.Introduction -> {
                AriaCommandResult("Soy ARIA, tu asistente personal. Tengo ${reminders.size} recordatorio(s) en memoria.")
            }

            is AriaIntent.AddReminder -> {
                if (intent.text.isBlank()) {
                    AriaCommandResult("¿Qué querés que recuerde?")
                } else {
                    reminders.add(intent.text)
                    onMemoryChanged()
                    AriaCommandResult(
                        response = "He recordado: '${intent.text}'. Tenés ${reminders.size} recordatorio(s).",
                        uiAction = AriaUiAction.SHOW_REMINDERS
                    )
                }
            }

            AriaIntent.ListReminders -> {
                if (reminders.isEmpty()) {
                    AriaCommandResult(
                        response = "No tenés recordatorios pendientes.",
                        uiAction = AriaUiAction.SHOW_REMINDERS
                    )
                } else {
                    AriaCommandResult(
                        response = "Tenés ${reminders.size} recordatorio(s) guardado(s).",
                        uiAction = AriaUiAction.SHOW_REMINDERS
                    )
                }
            }

            is AriaIntent.RemoveReminder -> {
                if (intent.index == null) {
                    AriaCommandResult("Decime el número del recordatorio. Ejemplo: olvidar 2")
                } else if (intent.index < 1 || intent.index > reminders.size) {
                    AriaCommandResult("Número inválido. Usá un número del 1 al ${reminders.size}.")
                } else {
                    val removed = reminders.removeAt(intent.index - 1)
                    onMemoryChanged()
                    AriaCommandResult(
                        response = "Eliminado: '$removed'. Te quedan ${reminders.size} recordatorio(s).",
                        uiAction = AriaUiAction.SHOW_REMINDERS
                    )
                }
            }

            AriaIntent.Help -> AriaCommandResult(HELP_TEXT)

            AriaIntent.Exit -> {
                onMemoryChanged()
                AriaCommandResult(
                    response = "Memoria guardada. Hasta luego $user.",
                    uiAction = AriaUiAction.CLOSE_APP
                )
            }

            is AriaIntent.Unknown -> {
                AriaCommandResult("No entendí '${intent.input}'. Escribí 'ayuda' para ver los comandos.")
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
            salir / adiós / chau
            """.trimIndent()
    }
}
