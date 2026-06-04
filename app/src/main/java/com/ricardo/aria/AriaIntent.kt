package com.ricardo.aria

sealed class AriaIntent {
    data object Greeting : AriaIntent()
    data object CurrentTime : AriaIntent()
    data object CurrentDate : AriaIntent()
    data object Introduction : AriaIntent()
    data class AddReminder(val text: String) : AriaIntent()
    data object ListReminders : AriaIntent()
    data class RemoveReminder(val index: Int?) : AriaIntent()
    data object Help : AriaIntent()
    data object Exit : AriaIntent()
    data class Unknown(val input: String) : AriaIntent()
}

enum class AriaUiAction {
    SHOW_REMINDERS,
    CLOSE_APP
}

data class AriaCommandResult(
    val response: String,
    val uiAction: AriaUiAction? = null
)
