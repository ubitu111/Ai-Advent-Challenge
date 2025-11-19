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
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.mcp.model.McpJsonRpcRequest
import ru.mirtomsk.shared.network.mcp.model.McpJsonRpcResponse
import ru.mirtomsk.shared.network.mcp.model.McpTool

/**
 * API service for MCP (Model Context Protocol) server communication
 * Handles HTTP requests to MCP server using JSON-RPC 2.0 protocol
 *
 * Default server: Gismeteo MCP Server (public remote server)
 * Alternative: Can be configured to use other MCP servers
 */
class McpApiService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String,
    private val apiConfig: ApiConfig,
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

        val token = apiConfig.mcpgateToken
        val endpoint = "$baseUrl?apikey=$token"

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
