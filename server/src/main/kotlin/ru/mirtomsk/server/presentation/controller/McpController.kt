package ru.mirtomsk.server.presentation.controller

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import ru.mirtomsk.server.data.model.McpJsonRpcError
import ru.mirtomsk.server.data.model.McpJsonRpcRequest
import ru.mirtomsk.server.data.model.McpJsonRpcResponse
import ru.mirtomsk.server.data.model.McpToolCallContentDto
import ru.mirtomsk.server.data.model.McpToolCallResultDto
import ru.mirtomsk.server.data.model.McpToolsListResult
import ru.mirtomsk.server.data.model.toDto
import ru.mirtomsk.server.domain.model.McpToolCall
import ru.mirtomsk.server.domain.usecase.CallToolUseCase
import ru.mirtomsk.server.domain.usecase.GetToolsUseCase

/**
 * Controller for handling MCP protocol requests
 * Handles JSON-RPC 2.0 requests and routes them to appropriate use cases
 */
class McpController(
    private val getToolsUseCase: GetToolsUseCase,
    private val callToolUseCase: CallToolUseCase,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Handle JSON-RPC request and return appropriate response
     */
    suspend fun handleRequest(request: McpJsonRpcRequest): McpJsonRpcResponse {
        return try {
            when (request.method) {
                "tools/list" -> handleToolsList(request.id)
                "tools/call" -> handleToolCall(request.id, request.params)
                "initialize" -> handleInitialize(request.id)
                else -> createErrorResponse(
                    request.id,
                    -32601,
                    "Method not found: ${request.method}"
                )
            }
        } catch (e: Exception) {
            createErrorResponse(
                request.id,
                -32603,
                "Internal error: ${e.message}"
            )
        }
    }
    
    private suspend fun handleToolsList(id: Int?): McpJsonRpcResponse {
        val tools = getToolsUseCase.execute()
        val toolsListResult = McpToolsListResult(
            tools = tools.map { it.toDto() }
        )
        
        return McpJsonRpcResponse(
            id = id,
            result = json.encodeToJsonElement(McpToolsListResult.serializer(), toolsListResult) as JsonElement
        )
    }
    
    private suspend fun handleToolCall(id: Int?, params: Map<String, JsonElement>?): McpJsonRpcResponse {
        if (params == null) {
            return createErrorResponse(id, -32602, "Invalid params: params is required")
        }
        
        val nameElement = params["name"] as? JsonPrimitive
            ?: return createErrorResponse(id, -32602, "Invalid params: 'name' is required")
        
        val name = nameElement.content
        
        val argumentsElement = params["arguments"] as? JsonObject
        val arguments = argumentsElement?.entries?.associate { entry ->
            entry.key to parseJsonElementToAny(entry.value)
        } ?: emptyMap()
        
        val toolCall = McpToolCall(
            toolName = name,
            arguments = arguments
        )
        
        val result = callToolUseCase.execute(toolCall)
        val resultDto = McpToolCallResultDto(
            content = result.content.map { content ->
                McpToolCallContentDto(
                    type = content.type,
                    text = content.text,
                    data = content.data,
                    mimeType = content.mimeType
                )
            },
            isError = result.isError
        )
        
        return McpJsonRpcResponse(
            id = id,
            result = json.encodeToJsonElement(McpToolCallResultDto.serializer(), resultDto) as JsonElement
        )
    }
    
    private fun handleInitialize(id: Int?): McpJsonRpcResponse {
        val result = buildJsonObject {
            put("protocolVersion", JsonPrimitive("2024-11-05"))
            put("capabilities", buildJsonObject {
                put("tools", buildJsonObject {
                    put("listChanged", JsonPrimitive(false))
                })
            })
            put("serverInfo", buildJsonObject {
                put("name", JsonPrimitive("ai-advent-challenge-server"))
                put("version", JsonPrimitive("1.0.0"))
            })
        }
        
        return McpJsonRpcResponse(
            id = id,
            result = result
        )
    }
    
    private fun createErrorResponse(id: Int?, code: Int, message: String): McpJsonRpcResponse {
        return McpJsonRpcResponse(
            id = id,
            error = McpJsonRpcError(
                code = code,
                message = message
            )
        )
    }
    
    /**
     * Parse JsonElement to Any for tool arguments
     */
    private fun parseJsonElementToAny(element: JsonElement): Any {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" || element.content == "false" -> element.content.toBoolean()
                    element.content.toLongOrNull() != null -> element.content.toLong()
                    element.content.toDoubleOrNull() != null -> element.content.toDouble()
                    else -> element.content
                }
            }
            is JsonObject -> element.jsonObject.entries.associate { entry ->
                entry.key to parseJsonElementToAny(entry.value)
            }
            else -> element.toString()
        }
    }
}
