package ru.mirtomsk.shared.network.mcp

import ru.mirtomsk.shared.network.mcp.model.McpTool

/**
 * Repository interface for MCP operations
 */
interface McpRepository {
    suspend fun getTools(): List<McpTool>
}
