package ru.mirtomsk.shared.network.mcp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.mirtomsk.shared.network.mcp.model.McpTool

/**
 * Provider for MCP tools settings
 * Manages available tools and selected tools
 */
class McpToolsProvider {
    private val _availableTools = MutableStateFlow<List<McpTool>>(emptyList())
    val availableTools: StateFlow<List<McpTool>> = _availableTools.asStateFlow()

    private val _selectedTools = MutableStateFlow<Set<String>>(emptySet())
    val selectedTools: StateFlow<Set<String>> = _selectedTools.asStateFlow()

    /**
     * Update list of available tools
     */
    fun updateAvailableTools(tools: List<McpTool>) {
        _availableTools.value = tools
    }

    /**
     * Toggle tool selection
     */
    fun toggleTool(toolName: String) {
        _selectedTools.value = if (_selectedTools.value.contains(toolName)) {
            _selectedTools.value - toolName
        } else {
            _selectedTools.value + toolName
        }
    }

    /**
     * Set selected tools
     */
    fun setSelectedTools(toolNames: Set<String>) {
        _selectedTools.value = toolNames
    }

    /**
     * Check if tool is selected
     */
    fun isToolSelected(toolName: String): Boolean {
        return _selectedTools.value.contains(toolName)
    }
}
