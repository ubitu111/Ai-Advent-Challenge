package ru.mirtomsk.shared.settings.model

import ru.mirtomsk.shared.settings.Strings

data class SettingsUiState(
    val responseFormat: String = Strings.DEFAULT_FORMAT,
    val selectedAgent: AgentType = AgentType.LITE,
    val selectedSystemPrompt: SystemPrompt = SystemPrompt.EMPTY,
    val temperature: String = Strings.DEFAULT_TEMPERATURE,
)
