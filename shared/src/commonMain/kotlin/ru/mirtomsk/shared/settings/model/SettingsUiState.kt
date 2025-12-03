package ru.mirtomsk.shared.settings.model

import ru.mirtomsk.shared.settings.Strings

data class SettingsUiState(
    val responseFormat: String = Strings.DEFAULT_FORMAT,
    val temperature: String = Strings.DEFAULT_TEMPERATURE,
    val maxTokens: String = Strings.DEFAULT_MAX_TOKENS,
    val isCompressionEnabled: Boolean = false,
)
