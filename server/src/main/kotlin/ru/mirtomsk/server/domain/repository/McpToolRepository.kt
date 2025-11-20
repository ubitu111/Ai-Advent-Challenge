package ru.mirtomsk.server.domain.repository

import ru.mirtomsk.server.domain.model.McpTool
import ru.mirtomsk.server.domain.model.McpToolCall
import ru.mirtomsk.server.domain.model.McpToolCallResult

/**
 * Repository interface for MCP tools operations
 * Follows clean architecture principles - domain layer doesn't depend on data layer
 */
interface McpToolRepository {
    /**
     * Get list of all available MCP tools
     */
    suspend fun getAllTools(): List<McpTool>
    
    /**
     * Call a specific MCP tool with given arguments
     * @param toolCall The tool call request
     * @return Result of the tool execution
     */
    suspend fun callTool(toolCall: McpToolCall): McpToolCallResult
}
