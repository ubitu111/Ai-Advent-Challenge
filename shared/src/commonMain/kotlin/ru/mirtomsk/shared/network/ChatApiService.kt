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
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.config.ApiConfig

/**
 * API service for chat-related network requests
 */
class ChatApiService(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfig,
) {

    /**
     * Request model with streaming response support
     * Returns a Flow of response body lines (NDJSON format)
     */
    fun requestModelStream(request: AiRequest): Flow<String> = flow {
        val response = httpClient.post(API_URL) {
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
     * Request model and return the full response body
     * Collects all lines from NDJSON stream and returns them as a single string
     */
    suspend fun requestModel(request: AiRequest): String {
        val lines = mutableListOf<String>()
        requestModelStream(request).collect { line ->
            lines.add(line)
        }
        return lines.joinToString("\n")
    }

    private companion object {

        const val API_URL = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
    }
}

