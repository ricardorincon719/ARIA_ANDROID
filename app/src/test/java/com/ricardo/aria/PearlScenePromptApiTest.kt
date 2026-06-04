package com.ricardo.aria

import org.junit.Assert.assertEquals
import org.junit.Test

class PearlScenePromptApiTest {
    @Test
    fun parsePendingPromptsReturnsValidPrompts() {
        val prompts = PearlScenePromptApi.parsePendingPrompts(
            """
            {
              "status": "ok",
              "prompts": [
                {
                  "id": "scene-1",
                  "title": "Escena detectada",
                  "message": "¿Aceptas apagar luces?"
                },
                {
                  "id": "scene-2"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(2, prompts.size)
        assertEquals(PearlScenePrompt("scene-1", "Escena detectada", "¿Aceptas apagar luces?"), prompts[0])
        assertEquals("scene-2", prompts[1].id)
        assertEquals("PEARL detectó una escena", prompts[1].title)
    }

    @Test
    fun parsePendingPromptsIgnoresMalformedEntries() {
        val prompts = PearlScenePromptApi.parsePendingPrompts(
            """
            {
              "prompts": [
                {},
                {"id": "   "},
                {"id": "scene-ok", "title": "Lista", "message": "Pendiente"}
              ]
            }
            """.trimIndent()
        )

        assertEquals(listOf(PearlScenePrompt("scene-ok", "Lista", "Pendiente")), prompts)
    }

    @Test
    fun decisionPayloadContainsDecisionAndIdempotencyKey() {
        assertEquals(
            """{"decision":"accept","idempotency_key":"key-1"}""",
            PearlScenePromptApi.decisionPayload("accept", "key-1")
        )
    }
}
