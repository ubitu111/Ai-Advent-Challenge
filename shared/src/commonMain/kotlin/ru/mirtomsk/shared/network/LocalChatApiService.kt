package ru.mirtomsk.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto

/**
 * API service for local LLM models (Ollama/LM Studio)
 * Uses OpenAI-compatible API format
 */
class LocalChatApiService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String,
    private val modelName: String,
) {
    
    /**
     * Request local LLM model with streaming response support
     * Note: Локальная модель не использует streaming, возвращает полный ответ
     */
    fun requestLocalLlmStream(request: AiRequest): Flow<String> = flow {
        // Локальная модель не поддерживает streaming, получаем полный ответ
        val fullResponse = requestLocalLlm(request)
        emit(fullResponse)
    }
    
    /**
     * Request local LLM model and return the full response (non-streaming)
     */
    suspend fun requestLocalLlm(request: AiRequest): String {
        // Convert Yandex format to OpenAI format
        val openAiRequest = convertToOpenAiFormat(request)
        val requestBody = json.encodeToString(
            OpenAiChatRequest.serializer(),
            openAiRequest
        )
        
        println("Local LLM Request: $requestBody")
        
        val response = httpClient.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            // Ollama doesn't require API key, but some clients expect it
            header("Authorization", "Bearer ollama")
            setBody(requestBody)
        }
        
        // Получаем полный ответ (не streaming)
        val responseText = response.bodyAsText()
        
        // Возвращаем ответ как есть (один JSON объект)
        return responseText.trim()
    }
    
    /**
     * Convert Yandex GPT format to OpenAI format
     */
    private fun convertToOpenAiFormat(yandexRequest: AiRequest): OpenAiChatRequest {
        val messages = yandexRequest.messages.map { msg ->
            OpenAiMessage(
                role = when (msg.role) {
                    MessageRoleDto.SYSTEM -> "system"
                    MessageRoleDto.USER -> "user"
                    MessageRoleDto.ASSISTANT -> "assistant"
                },
                content = msg.text
            )
        }
        
        // Convert tools if present
        val tools = yandexRequest.tools?.map { tool ->
            OpenAiTool(
                type = tool.type,
                function = OpenAiFunction(
                    name = tool.function.name,
                    description = tool.function.description,
                    parameters = tool.function.parameters?.let { params ->
                        buildJsonObject {
                            put("type", params.type)
                            params.properties?.forEach { (key, prop) ->
                                put(key, buildJsonObject {
                                    prop.type?.let { put("type", it) }
                                    prop.description?.let { put("description", it) }
                                })
                            }
                            params.required?.let { requiredList ->
                                put("required", buildJsonArray {
                                    requiredList.forEach { add(JsonPrimitive(it)) }
                                })
                            }
                        }
                    }
                )
            )
        }
        
        return OpenAiChatRequest(
            model = modelName,
            messages = messages,
            temperature = yandexRequest.completionOptions.temperature,
            max_tokens = yandexRequest.completionOptions.maxTokens,
            stream = false, // Локальная модель всегда использует non-streaming
//            tools = tools
        )
    }
    
    @Serializable
    data class OpenAiChatRequest(
        val model: String,
        val messages: List<OpenAiMessage>,
        val temperature: Float,
        val max_tokens: Int,
        val stream: Boolean,
        val tools: List<OpenAiTool>? = null,
    )
    
    @Serializable
    data class OpenAiMessage(
        val role: String,
        val content: String,
    )
    
    @Serializable
    data class OpenAiTool(
        val type: String,
        val function: OpenAiFunction,
    )
    
    @Serializable
    data class OpenAiFunction(
        val name: String,
        val description: String? = null,
        val parameters: kotlinx.serialization.json.JsonObject? = null,
    )
}
