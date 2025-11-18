package ru.mirtomsk.shared.network.mcp.model

import kotlinx.serialization.Serializable

/**
 * MCP Tool model representing a tool available from MCP server
 */
@Serializable
data class McpTool(
    val name: String,
    val description: String? = null,
    val inputSchema: McpToolInputSchema? = null,
)

/**
 * Input schema for MCP tool
 */
@Serializable
data class McpToolInputSchema(
    val type: String? = null,
    val properties: Map<String, McpToolProperty>? = null,
    val required: List<String>? = null,
)

/**
 * Property definition for MCP tool input schema
 */
@Serializable
data class McpToolProperty(
    val type: String? = null,
    val description: String? = null,
)

