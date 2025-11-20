package ru.mirtomsk.shared.network.mcp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.mirtomsk.shared.network.mcp.model.McpTool

/**
 * Provider for MCP tools
 * Manages available tools from MCP server
 */
class McpToolsProvider {
    private val _availableTools = MutableStateFlow<List<McpTool>>(emptyList())
    val availableTools: StateFlow<List<McpTool>> = _availableTools.asStateFlow()

    /**
     * Update list of available tools
     */
    fun updateAvailableTools(tools: List<McpTool>) {
        _availableTools.value = tools
    }

    /**
     * Get all available tools
     */
    fun getAvailableTools(): List<McpTool> {
        return _availableTools.value
    }
}
