package ru.mirtomsk.server.presentation.routing

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import ru.mirtomsk.server.data.model.McpJsonRpcError
import ru.mirtomsk.server.data.model.McpJsonRpcRequest
import ru.mirtomsk.server.data.model.McpJsonRpcResponse
import ru.mirtomsk.server.presentation.controller.McpController

/**
 * Routing configuration for MCP protocol endpoints
 */
fun Application.configureMcpRouting(mcpController: McpController) {
    val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    routing {
        /**
         * MCP protocol endpoint
         * Accepts JSON-RPC 2.0 requests
         * Returns response in SSE format if client requests it, otherwise returns plain JSON
         */
        post("/mcp") {
            try {
                val request = call.receive<McpJsonRpcRequest>()
                val response = mcpController.handleRequest(request)
                
                // Check if client expects SSE format
                val acceptHeader = call.request.header(HttpHeaders.Accept) ?: ""
                val expectsSse = acceptHeader.contains("text/event-stream")
                
                if (expectsSse) {
                    // Return response in SSE format
                    val jsonResponse = json.encodeToString(McpJsonRpcResponse.serializer(), response)
                    call.respondText(
                        text = "data: $jsonResponse\n\n",
                        contentType = ContentType.parse("text/event-stream")
                    )
                } else {
                    // Return plain JSON response
                    call.respond(response)
                }
            } catch (e: Exception) {
                val errorResponse = McpJsonRpcResponse(
                    id = null,
                    error = McpJsonRpcError(
                        code = -32700,
                        message = "Parse error: ${e.message}"
                    )
                )
                
                val acceptHeader = call.request.header(HttpHeaders.Accept) ?: ""
                val expectsSse = acceptHeader.contains("text/event-stream")
                
                if (expectsSse) {
                    val jsonResponse = json.encodeToString(McpJsonRpcResponse.serializer(), errorResponse)
                    call.respondText(
                        text = "data: $jsonResponse\n\n",
                        contentType = ContentType.parse("text/event-stream")
                    )
                } else {
                    call.respond(errorResponse)
                }
            }
        }
    }
}

