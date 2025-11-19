package ru.mirtomsk.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.agent.AgentTypeDto

/**
 * Unified API service for all AI models (Yandex GPT and HuggingFace)
 * Provides a single point of access for all model requests
 */
class ChatApiService(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfig,
    private val json: Json,
) {

    /**
     * Request Yandex GPT model with streaming response support
     * Returns a Flow of response body lines (NDJSON format)
     */
    fun requestYandexGptStream(request: AiRequest): Flow<String> = flow {
        // Явная сериализация для гарантии правильного формата с encodeDefaults = true
        val requestBody = json.encodeToString(AiRequest.serializer(), request)
        println("Yandex GPT Request: $requestBody")
        
        val response = httpClient.post(YANDEX_API_URL) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Api-Key ${apiConfig.apiKey}")
            // Используем setBody с String, чтобы избежать повторной сериализации через ContentNegotiation
            setBody(requestBody)
        }
        
        // Логируем статус ответа для отладки
        println("Yandex GPT Response Status: ${response.status}")

        val responseText = response.bodyAsText()

        // Process NDJSON format (newline-delimited JSON)
        responseText.lines()
            .filter { it.isNotBlank() }
            .forEach { line ->
                emit(line)
            }
    }

    /**
     * Request Yandex GPT model and return the full response body
     * Collects all lines from NDJSON stream and returns them as a single string
     */
    suspend fun requestYandexGpt(request: AiRequest): String {
        val lines = mutableListOf<String>()
        requestYandexGptStream(request).collect { line ->
            lines.add(line)
        }
        return lines.joinToString("\n")
    }

    /**
     * Request HuggingFace model and return the raw response body
     * Uses new Chat API format with messages array
     * Response parsing should be handled by HuggingFaceResponseMapper
     */
    suspend fun requestHuggingFace(
        model: AgentTypeDto,
        messages: List<HuggingFaceMessage>,
        parameters: HuggingFaceParameters = HuggingFaceParameters(),
        tools: List<HuggingFaceTool>? = null,
    ): String {
        val apiUrl = model.huggingFaceApiUrl
            ?: throw IllegalArgumentException("Model ${model.name} is not a HuggingFace model")
        
        val token = apiConfig.huggingFaceToken
        val response = httpClient.post(apiUrl) {
            contentType(ContentType.Application.Json)
            if (token.isNotBlank()) {
                header("Authorization", "Bearer $token")
            }
            setBody(
                HuggingFaceChatRequest(
                    messages = messages,
                    model = model.modelId,
                    stream = false,
                    temperature = parameters.temperature,
                    max_tokens = parameters.max_new_tokens,
                    tools = tools,
                )
            )
        }

        return response.bodyAsText()
    }

    private companion object {
        const val YANDEX_API_URL = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
    }
}

/**
 * Request model for HuggingFace Chat API (new format)
 */
@Serializable
data class HuggingFaceChatRequest(
    val messages: List<HuggingFaceMessage>,
    val model: String,
    val stream: Boolean = false,
    val temperature: Double? = null,
    val max_tokens: Int? = null,
    val tools: List<HuggingFaceTool>? = null,
)

/**
 * Message in HuggingFace Chat API format
 */
@Serializable
data class HuggingFaceMessage(
    val role: String, // "user", "assistant", "system"
    val content: String,
)

/**
 * Parameters for HuggingFace API request
 */
@Serializable
data class HuggingFaceParameters(
    val max_new_tokens: Int = 100,
    val temperature: Double = 0.7,
    val top_p: Double = 0.9,
    val top_k: Int? = null,
    val repetition_penalty: Double? = null,
    val return_full_text: Boolean = false,
    val do_sample: Boolean = true,
)

/**
 * Tool model for HuggingFace API
 */
@Serializable
data class HuggingFaceTool(
    val type: String = "function",
    val function: HuggingFaceToolFunction,
)

/**
 * Tool function for HuggingFace API
 */
@Serializable
data class HuggingFaceToolFunction(
    val name: String,
    val description: String? = null,
    val parameters: HuggingFaceToolParameters? = null,
)

/**
 * Tool parameters for HuggingFace API
 */
@Serializable
data class HuggingFaceToolParameters(
    val type: String = "object",
    val properties: Map<String, HuggingFaceToolProperty>? = null,
    val required: List<String>? = null,
)

/**
 * Tool property for HuggingFace API
 */
@Serializable
data class HuggingFaceToolProperty(
    val type: String? = null,
    val description: String? = null,
)

