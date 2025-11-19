package ru.mirtomsk.server.domain.model

/**
 * Domain model for MCP tool call request
 */
data class McpToolCall(
    val toolName: String,
    val arguments: Map<String, Any> = emptyMap(),
)

/**
 * Domain model for MCP tool call result
 */
data class McpToolCallResult(
    val content: List<McpToolCallContent>,
    val isError: Boolean = false,
)

/**
 * Content of tool call result
 */
data class McpToolCallContent(
    val type: String, // "text" or "image"
    val text: String? = null,
    val data: String? = null, // base64 encoded data for images
    val mimeType: String? = null,
)
