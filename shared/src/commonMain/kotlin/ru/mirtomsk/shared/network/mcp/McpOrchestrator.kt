package ru.mirtomsk.shared.network.mcp

import ru.mirtomsk.shared.network.mcp.model.McpTool

/**
 * Orchestrator for managing multiple MCP services
 * Handles tool discovery and tool calls across multiple MCP servers
 */
class McpOrchestrator(
    private val services: List<McpService>
) {
    // Map tool name to service that provides it
    private val toolToServiceMap: MutableMap<String, McpService> = mutableMapOf()

    /**
     * Get all available tools from all registered MCP services
     * Aggregates tools from all services and builds internal mapping
     * @return Combined list of all tools from all services
     */
    suspend fun getTools(): List<McpTool> {
        val allTools = mutableListOf<McpTool>()
        toolToServiceMap.clear()

        for (service in services) {
            try {
                val tools = service.getTools()
                allTools.addAll(tools)
                // Map each tool to its service
                tools.forEach { tool ->
                    toolToServiceMap[tool.name] = service
                }
            } catch (e: Exception) {
                println("Error getting tools from MCP service ${service.serviceId}: ${e.message}")
            }
        }

        return allTools
    }

    /**
     * Call a tool by finding the appropriate service and executing the call
     * @param toolName Name of the tool to call
     * @param arguments Arguments for the tool call as JSON string
     * @return Result of the tool call as JSON string
     * @throws Exception if tool is not found or service call fails
     */
    suspend fun callTool(toolName: String, arguments: String): String {
        val service = toolToServiceMap[toolName]
            ?: throw Exception("Tool '$toolName' not found in any MCP service")

        return try {
            service.callTool(toolName, arguments)
        } catch (e: Exception) {
            throw Exception("Error calling tool '$toolName' on service ${service.serviceId}: ${e.message}", e)
        }
    }

    /**
     * Get the service ID that provides a specific tool
     * @param toolName Name of the tool
     * @return Service ID or null if tool not found
     */
    fun getServiceIdForTool(toolName: String): String? {
        return toolToServiceMap[toolName]?.serviceId
    }
}
