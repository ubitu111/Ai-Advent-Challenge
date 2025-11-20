package ru.mirtomsk.server.data.model

import kotlinx.serialization.Serializable

/**
 * JSON-RPC 2.0 Request DTO for MCP protocol
 */
@Serializable
data class McpJsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val method: String,
    val params: Map<String, kotlinx.serialization.json.JsonElement>? = null,
)

/**
 * JSON-RPC 2.0 Response DTO for MCP protocol
 */
@Serializable
data class McpJsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: kotlinx.serialization.json.JsonElement? = null,
    val error: McpJsonRpcError? = null,
)

/**
 * JSON-RPC Error DTO
 */
@Serializable
data class McpJsonRpcError(
    val code: Int,
    val message: String,
    val data: String? = null,
)

/**
 * Tools list result DTO
 */
@Serializable
data class McpToolsListResult(
    val tools: List<McpToolDto>,
)

/**
 * Tool call result DTO
 */
@Serializable
data class McpToolCallResultDto(
    val content: List<McpToolCallContentDto>,
    val isError: Boolean = false,
)

/**
 * Tool call content DTO
 */
@Serializable
data class McpToolCallContentDto(
    val type: String,
    val text: String? = null,
    val data: String? = null,
    val mimeType: String? = null,
)
