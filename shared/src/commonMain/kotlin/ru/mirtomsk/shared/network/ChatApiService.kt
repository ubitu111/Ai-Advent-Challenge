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
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.AiResponse

import ru.mirtomsk.shared.config.ApiConfig

/**
 * API service for chat-related network requests
 */
class ChatApiService(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfig,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    /**
     * Request model with streaming response support
     * Returns a Flow of AiResponse chunks as they arrive
     */
    suspend fun requestModelStream(message: String): Flow<AiResponse> = flow {
        val response = httpClient.post("https://llm.api.cloud.yandex.net/foundationModels/v1/completion") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Api-Key ${apiConfig.apiKey}")
            setBody(createRequestBody(message))
        }

        val responseText = response.bodyAsText()
        
        // Process NDJSON format (newline-delimited JSON)
        responseText.lines()
            .filter { it.isNotBlank() }
            .forEach { line ->
                try {
                    val aiResponse = json.decodeFromString<AiResponse>(line)
                    emit(aiResponse)
                } catch (e: Exception) {
                    // Log error but continue processing other lines
                    println("Error deserializing line: $line, error: ${e.message}")
                }
            }
    }

    /**
     * Request model and return only the final response
     */
    suspend fun requestModel(message: String): AiResponse {
        var finalResponse: AiResponse? = null
        
        requestModelStream(message).collect { response ->
            // Keep the last response (should be FINAL status)
            finalResponse = response
        }
        
        return finalResponse ?: throw IllegalStateException("No response received")
    }

    private fun createRequestBody(message: String): AiRequest {
        return AiRequest(
            modelUri = "gpt://${apiConfig.keyId}/yandexgpt-lite",
            completionOptions = AiRequest.CompletionOptions(
                stream = true,
                temperature = 0.6f,
                maxTokens = 2000,
            ),
            messages = listOf(
                AiRequest.Message(
                    role = "system",
                    text = "Ты добродушный ассистент, который всегда перед ответом вставляет Привет Боб!",
                ),
                AiRequest.Message(
                    role = "user",
                    text = message,
                )
            )
        )
    }
}

