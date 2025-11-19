package ru.mirtomsk.shared.network.mcp

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.network.mcp.model.McpTool

/**
 * Implementation of McpRepository
 * Fetches tools from MCP server using McpApiService
 */
class McpRepositoryImpl(
    private val mcpApiService: McpApiService,
    private val ioDispatcher: CoroutineDispatcher,
) : McpRepository {

    override suspend fun getTools(): List<McpTool> {
        return withContext(ioDispatcher) {
            mcpApiService.getTools()
        }
    }
}
