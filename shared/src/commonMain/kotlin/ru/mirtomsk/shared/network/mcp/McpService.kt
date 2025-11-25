package ru.mirtomsk.shared.network.mcp

import ru.mirtomsk.shared.network.mcp.model.McpTool

/**
 * Interface for MCP service
 * Represents a single MCP server that can provide tools and execute tool calls
 */
interface McpService {
    /**
     * Get unique identifier for this MCP service
     * Used to distinguish between different services
     */
    val serviceId: String

    /**
     * Get list of available tools from this MCP service
     * @return List of tools available from this service
     */
    suspend fun getTools(): List<McpTool>

    /**
     * Call a tool on this MCP service
     * @param toolName Name of the tool to call
     * @param arguments Arguments for the tool call as JSON string
     * @return Result of the tool call as JSON string
     */
    suspend fun callTool(toolName: String, arguments: String): String
}
