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
) {

    /**
     * Request Yandex GPT model with streaming response support
     * Returns a Flow of response body lines (NDJSON format)
     */
    fun requestYandexGptStream(request: AiRequest): Flow<String> = flow {
        val response = httpClient.post(YANDEX_API_URL) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Api-Key ${apiConfig.apiKey}")
            setBody(request)
        }

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
     * Response parsing should be handled by HuggingFaceResponseMapper
     */
    suspend fun requestHuggingFace(
        model: AgentTypeDto,
        prompt: String,
        parameters: HuggingFaceParameters = HuggingFaceParameters(),
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
                HuggingFaceRequest(
                    inputs = prompt,
                    parameters = parameters
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
 * Request model for HuggingFace Inference API
 */
@Serializable
data class HuggingFaceRequest(
    val inputs: String,
    val parameters: HuggingFaceParameters = HuggingFaceParameters(),
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

