package ru.mirtomsk.server.domain.usecase

import ru.mirtomsk.server.domain.model.McpTool
import ru.mirtomsk.server.domain.repository.McpToolRepository

/**
 * Use case for getting all available MCP tools
 * Encapsulates business logic for tools retrieval
 */
class GetToolsUseCase(
    private val repository: McpToolRepository,
) {
    suspend fun execute(): List<McpTool> {
        return repository.getAllTools()
    }
}
