package ru.mirtomsk.server.domain.model

/**
 * Domain model representing an MCP tool
 */
data class McpTool(
    val name: String,
    val description: String? = null,
    val inputSchema: McpToolInputSchema? = null,
)

/**
 * Input schema for MCP tool
 */
data class McpToolInputSchema(
    val type: String? = null,
    val properties: Map<String, McpToolProperty>? = null,
    val required: List<String>? = null,
)

/**
 * Property definition for MCP tool input schema
 */
data class McpToolProperty(
    val type: String? = null,
    val description: String? = null,
)
