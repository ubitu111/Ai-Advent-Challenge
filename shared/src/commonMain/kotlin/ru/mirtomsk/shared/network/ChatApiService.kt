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

/**
 * API service for Yandex GPT model
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

    private companion object {
        const val YANDEX_API_URL = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
    }
}

