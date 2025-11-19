package ru.mirtomsk.shared.network.mcp.model

import kotlinx.serialization.Serializable

/**
 * JSON-RPC 2.0 Request for MCP protocol
 */
@Serializable
data class McpJsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String,
    val params: Map<String, String> = emptyMap(),
)

/**
 * JSON-RPC 2.0 Response for MCP protocol
 */
@Serializable
data class McpJsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: McpToolsListResult? = null,
    val error: McpJsonRpcError? = null,
)

/**
 * Result containing list of tools
 */
@Serializable
data class McpToolsListResult(
    val tools: List<McpTool>,
)

/**
 * JSON-RPC Error
 */
@Serializable
data class McpJsonRpcError(
    val code: Int,
    val message: String,
    val data: String? = null,
)
