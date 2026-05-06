package com.ricardo.aria

sealed class AriaChatMessage(open val text: String) {
    data class SentByUser(override val text: String) : AriaChatMessage(text)
    data class SentByAria(override val text: String) : AriaChatMessage(text)
}
