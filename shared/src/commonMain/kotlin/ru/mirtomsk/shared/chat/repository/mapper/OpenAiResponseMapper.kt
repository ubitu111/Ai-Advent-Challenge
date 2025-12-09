package ru.mirtomsk.shared.chat.repository.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import ru.mirtomsk.shared.chat.repository.model.AiAlternative
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.AiResponse
import ru.mirtomsk.shared.chat.repository.model.AiResult
import ru.mirtomsk.shared.chat.repository.model.AiToolCall
import ru.mirtomsk.shared.chat.repository.model.AiToolCallFunction
import ru.mirtomsk.shared.chat.repository.model.AiUsage
import ru.mirtomsk.shared.chat.repository.model.CompletionTokensDetails
import ru.mirtomsk.shared.chat.repository.model.FunctionCall
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.chat.repository.model.ToolCallList
import ru.mirtomsk.shared.network.format.ResponseFormat

/**
 * Mapper for converting OpenAI-compatible API response to AiResponse
 */
class OpenAiResponseMapper(
    private val json: Json
) {
    
    /**
     * Parse OpenAI response format to AiResponse format
     */
    fun mapResponseBody(
        responseBody: String,
        format: ResponseFormat
    ): AiResponse {
        val lines = responseBody.lines().filter { it.isNotBlank() }
        
        var finalResponse: OpenAiChatResponse? = null
        
        // Process streaming response
        lines.forEach { line ->
            try {
                val response = json.decodeFromString<OpenAiChatResponse>(line)
                // Keep the last response
                if (response.choices.isNotEmpty()) {
                    finalResponse = response
                }
            } catch (e: Exception) {
                println("Error deserializing OpenAI response line: $line, error: ${e.message}")
            }
        }
        
        val response = finalResponse ?: throw IllegalStateException("No valid response received")
        
        // Convert to AiResponse format
        val choice = response.choices.firstOrNull()
            ?: throw IllegalStateException("No choices in response")
        
        val message = choice.message
        val role = when (message.role) {
            "system" -> MessageRoleDto.SYSTEM
            "user" -> MessageRoleDto.USER
            "assistant" -> MessageRoleDto.ASSISTANT
            else -> MessageRoleDto.ASSISTANT
        }
        
        // Handle tool calls if present
        val toolCalls = message.tool_calls?.map { toolCall ->
            // Parse function arguments from JSON string
            val argumentsMap = try {
                json.parseToJsonElement(toolCall.function?.arguments ?: "{}").jsonObject
            } catch (e: Exception) {
                buildJsonObject {}
            }
            
            AiToolCall(
                id = toolCall.id,
                type = toolCall.type,
                function = toolCall.function?.let { func ->
                    AiToolCallFunction(
                        name = func.name,
                        arguments = func.arguments
                    )
                },
                functionCall = toolCall.function?.let { func ->
                    FunctionCall(
                        name = func.name,
                        arguments = argumentsMap
                    )
                }
            )
        }
        
        val messageContent = message.content?.let {
            AiMessage.MessageContent.Text(it)
        }
        
        val aiMessage = AiMessage(
            role = role,
            text = messageContent,
            toolCalls = toolCalls,
            toolCallList = toolCalls?.let { 
                ToolCallList(it) 
            }
        )
        
        return AiResponse(
            result = AiResult(
                alternatives = listOf(
                    AiAlternative(
                        message = aiMessage,
                        status = "FINAL"
                    )
                ),
                usage = AiUsage(
                    inputTextTokens = response.usage?.prompt_tokens?.toString() ?: "0",
                    completionTokens = response.usage?.completion_tokens?.toString() ?: "0",
                    totalTokens = response.usage?.total_tokens?.toString() ?: "0",
                    completionTokensDetails = CompletionTokensDetails(
                        reasoningTokens = "0"
                    )
                ),
                modelVersion = response.model ?: "local-model"
            )
        )
    }
    
    @kotlinx.serialization.Serializable
    data class OpenAiChatResponse(
        val id: String? = null,
        val model: String? = null,
        val choices: List<OpenAiChoice>,
        val usage: OpenAiUsage? = null,
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiChoice(
        val index: Int = 0,
        val message: OpenAiMessage,
        val finish_reason: String? = null,
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiMessage(
        val role: String,
        val content: String? = null,
        val tool_calls: List<OpenAiToolCall>? = null,
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiToolCall(
        val id: String,
        val type: String,
        val function: OpenAiFunctionCall? = null,
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiFunctionCall(
        val name: String,
        val arguments: String, // JSON string
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiUsage(
        val prompt_tokens: Int = 0,
        val completion_tokens: Int = 0,
        val total_tokens: Int = 0,
    )
}
