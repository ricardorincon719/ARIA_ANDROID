package com.ricardo.aria

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AriaCommandProcessorTest {
    private val processor = AriaCommandProcessor(
        clock = { Date(0L) },
        locale = Locale.US,
        timeZone = TimeZone.getTimeZone("UTC")
    )

    @Test
    fun recordarAddsReminderIgnoringCommandCase() {
        val reminders = mutableListOf<String>()
        var saved = false

        val response = processor.process(
            input = "Recordar comprar pan",
            user = "Ricardo",
            reminders = reminders,
            onMemoryChanged = { saved = true }
        )

        assertEquals(listOf("comprar pan"), reminders)
        assertEquals("He recordado: 'comprar pan'. Tenés 1 recordatorio(s).", response)
        assertTrue(saved)
    }

    @Test
    fun olvidarRemovesReminderByOneBasedIndex() {
        val reminders = mutableListOf("comprar pan", "llamar taller")

        val response = processor.process(
            input = "olvidar 2",
            user = "Ricardo",
            reminders = reminders,
            onMemoryChanged = {}
        )

        assertEquals(listOf("comprar pan"), reminders)
        assertEquals("Eliminado: 'llamar taller'. Te quedan 1 recordatorio(s).", response)
    }

    @Test
    fun naturalReminderQuestionListsExistingReminders() {
        val result = processor.processDetailed(
            input = "que recordatorios tengo?",
            user = "Ricardo",
            reminders = mutableListOf("comprar pan", "llamar taller"),
            onMemoryChanged = {}
        )

        assertEquals("Tenés 2 recordatorio(s) guardado(s).", result.response)
        assertEquals(AriaUiAction.SHOW_REMINDERS, result.uiAction)
    }

    @Test
    fun naturalReminderQuestionHandlesEmptyReminders() {
        val result = processor.processDetailed(
            input = "que tareas pendientes tengo?",
            user = "Ricardo",
            reminders = mutableListOf(),
            onMemoryChanged = {}
        )

        assertEquals("No tenés recordatorios pendientes.", result.response)
        assertEquals(AriaUiAction.SHOW_REMINDERS, result.uiAction)
    }

    @Test
    fun goodbyeAliasesRequestAppClose() {
        val result = processor.processDetailed(
            input = "chau",
            user = "Ricardo",
            reminders = mutableListOf(),
            onMemoryChanged = {}
        )

        assertEquals("Memoria guardada. Hasta luego Ricardo.", result.response)
        assertEquals(AriaUiAction.CLOSE_APP, result.uiAction)
    }

    @Test
    fun naturalTimeQuestionUsesInjectedClock() {
        val response = processor.process(
            input = "que hora es?",
            user = "Ricardo",
            reminders = mutableListOf(),
            onMemoryChanged = {}
        )

        assertEquals("Son las 00:00 horas.", response)
    }

    @Test
    fun horaUsesInjectedClock() {
        val response = processor.process(
            input = "hora",
            user = "Ricardo",
            reminders = mutableListOf(),
            onMemoryChanged = {}
        )

        assertEquals("Son las 00:00 horas.", response)
    }

    @Test
    fun unknownCommandKeepsOriginalInputInResponse() {
        val response = processor.process(
            input = "enciende luces",
            user = "Ricardo",
            reminders = mutableListOf(),
            onMemoryChanged = {}
        )

        assertEquals("No entendí 'enciende luces'. Escribí 'ayuda' para ver los comandos.", response)
    }
}
