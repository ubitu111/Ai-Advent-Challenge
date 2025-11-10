package ru.mirtomsk.shared.settings

data class SettingsUiState(
    val responseFormat: String = "дефолт",
    val selectedAgent: AgentType = AgentType.AGENT_1,
)
