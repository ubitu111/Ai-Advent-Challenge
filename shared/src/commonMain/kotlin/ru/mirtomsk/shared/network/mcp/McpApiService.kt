package ru.mirtomsk.shared.network.mcp

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.mirtomsk.shared.network.mcp.model.McpJsonRpcRequest
import ru.mirtomsk.shared.network.mcp.model.McpJsonRpcResponse
import ru.mirtomsk.shared.network.mcp.model.McpTool

/**
 * API service for MCP (Model Context Protocol) server communication
 * Handles HTTP requests to MCP server using JSON-RPC 2.0 protocol
 */
class McpApiService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String,
) {
    private var requestIdCounter = 1

    /**
     * Get list of available tools from MCP server
     * Uses JSON-RPC 2.0 protocol with method "tools/list"
     */
    suspend fun getTools(): List<McpTool> {
        val request = McpJsonRpcRequest(
            id = requestIdCounter++,
            method = "tools/list",
        )

        val endpoint = baseUrl

        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            // MCP server requires Accept header with both application/json and text/event-stream
            header(HttpHeaders.Accept, "application/json, text/event-stream")
            setBody(request)
        }

        val responseText = response.bodyAsText()
        if (responseText.isBlank()) {
            println("Response body is blank")
        }

        // MCP server returns Server-Sent Events (SSE) format
        // Need to extract JSON from "data: " lines
        val jsonText = extractJsonFromSse(responseText)
        if (jsonText.isBlank()) {
            println("No JSON data found in SSE response")
        }

        // Parse JSON-RPC response
        val mcpResponse = json.decodeFromString<McpJsonRpcResponse>(jsonText)

        // Check for errors in response
        if (mcpResponse.error != null) {
            println("MCP server error: ${mcpResponse.error.message}")
        }

        return mcpResponse.result?.tools ?: emptyList()
    }

    /**
     * Call a tool on MCP server
     * Uses JSON-RPC 2.0 protocol with method "tools/call"
     * 
     * @param toolName Name of the tool to call
     * @param arguments Arguments for the tool call as JSON string
     * @return Result of the tool call as JSON string
     */
    suspend fun callTool(toolName: String, arguments: String): String {
        // Parse arguments JSON
        val argumentsJson = try {
            json.parseToJsonElement(arguments).jsonObject
        } catch (e: Exception) {
            // If arguments is not valid JSON, wrap it as a string parameter
            buildJsonObject { put("input", arguments) }
        }

        // Build request body manually for proper JSON-RPC format
        val requestBody = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", requestIdCounter++)
            put("method", "tools/call")
            put("params", buildJsonObject {
                put("name", toolName)
                put("arguments", argumentsJson)
            })
        }

        val endpoint = baseUrl

        val response = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, "application/json, text/event-stream")
            setBody(requestBody.toString())
        }

        val responseText = response.bodyAsText()
        if (responseText.isBlank()) {
            throw Exception("Empty response from MCP server")
        }

        // MCP server returns Server-Sent Events (SSE) format
        val jsonText = extractJsonFromSse(responseText)
        if (jsonText.isBlank()) {
            throw Exception("No JSON data found in SSE response")
        }

        // Parse JSON-RPC response manually to handle different result formats
        val responseJson = json.parseToJsonElement(jsonText).jsonObject

        // Check for errors
        val error = responseJson["error"]?.jsonObject
        if (error != null) {
            val errorMessage = error["message"]?.jsonPrimitive?.content ?: "Unknown error"
            throw Exception("MCP server error: $errorMessage")
        }

        // Extract result - MCP returns result with content/text fields
        val resultObj = responseJson["result"]?.jsonObject
        if (resultObj != null) {
            // Try to get content field - it can be an array or a primitive
            val contentElement = resultObj["content"]
            val content = when {
                // Content is an array of objects (e.g., [{"type":"text","text":"..."}])
                contentElement?.jsonArray != null -> {
                    val contentArray = contentElement.jsonArray
                    // Extract text from all content items
                    contentArray.mapNotNull { item ->
                        val itemObj = item.jsonObject
                        itemObj["text"]?.jsonPrimitive?.content
                            ?: itemObj["content"]?.jsonPrimitive?.content
                    }.joinToString("\n")
                }
                // Content is a primitive string
                contentElement?.jsonPrimitive != null -> {
                    contentElement.jsonPrimitive.content
                }
                // Try text field as fallback
                resultObj["text"]?.jsonPrimitive?.content != null -> {
                    resultObj["text"]?.jsonPrimitive?.content
                }
                // Last resort: convert entire result to string
                else -> resultObj.toString()
            }
            return content ?: throw Exception("No content found in MCP response")
        }

        throw Exception("No result in MCP response")
    }

    /**
     * Extract JSON from Server-Sent Events (SSE) format
     * SSE format: lines starting with "data: " contain JSON data
     * Multiple data lines can be concatenated
     */
    private fun extractJsonFromSse(sseText: String): String {
        val lines = sseText.lines()
        val jsonParts = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            // SSE data lines start with "data: "
            if (trimmed.startsWith("data: ")) {
                val jsonPart = trimmed.substring(6) // Remove "data: " prefix
                if (jsonPart.isNotBlank()) {
                    jsonParts.add(jsonPart)
                }
            }
        }

        // If multiple data lines, they should be concatenated
        // For single JSON object, return the first part
        return jsonParts.joinToString("")
    }
}
