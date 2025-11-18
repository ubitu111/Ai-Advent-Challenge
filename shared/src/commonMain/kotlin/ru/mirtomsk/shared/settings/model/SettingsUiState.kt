package ru.mirtomsk.shared.settings.model

import ru.mirtomsk.shared.network.mcp.model.McpTool
import ru.mirtomsk.shared.settings.Strings

data class SettingsUiState(
    val responseFormat: String = Strings.DEFAULT_FORMAT,
    val selectedAgent: AgentType = AgentType.LITE,
    val selectedSystemPrompt: SystemPrompt = SystemPrompt.DEFAULT,
    val temperature: String = Strings.DEFAULT_TEMPERATURE,
    val maxTokens: String = Strings.DEFAULT_MAX_TOKENS,
    val isCompressionEnabled: Boolean = false,
    val mcpTools: List<McpTool> = emptyList(),
    val selectedMcpTools: Set<String> = emptySet(),
    val isLoadingMcpTools: Boolean = false,
)
