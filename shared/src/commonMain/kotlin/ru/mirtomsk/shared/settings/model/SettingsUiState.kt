package ru.mirtomsk.shared.settings.model

data class SettingsUiState(
    val responseFormat: String = "дефолт",
    val selectedAgent: AgentType = AgentType.LITE,
)
