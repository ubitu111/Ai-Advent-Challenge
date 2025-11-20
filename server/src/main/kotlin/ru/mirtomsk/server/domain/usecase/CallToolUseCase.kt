package ru.mirtomsk.server.domain.usecase

import ru.mirtomsk.server.domain.model.McpToolCall
import ru.mirtomsk.server.domain.model.McpToolCallResult
import ru.mirtomsk.server.domain.repository.McpToolRepository

/**
 * Use case for calling an MCP tool
 * Encapsulates business logic for tool execution
 */
class CallToolUseCase(
    private val repository: McpToolRepository,
) {
    suspend fun execute(toolCall: McpToolCall): McpToolCallResult {
        return repository.callTool(toolCall)
    }
}
