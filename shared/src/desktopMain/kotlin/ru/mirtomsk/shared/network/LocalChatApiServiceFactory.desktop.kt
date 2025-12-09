package ru.mirtomsk.shared.network

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

/**
 * Desktop implementation uses HTTP-based LocalChatApiService (Ollama/LM Studio)
 * Note: For desktop, we create a new HttpClient instance.
 * In production, you might want to pass HttpClient from DI, but for simplicity
 * we create a new one here since desktop apps typically don't have the same
 * resource constraints as mobile apps.
 */
actual fun createLocalChatApiService(
    json: Json,
    baseUrl: String?,
    modelName: String?,
): ILocalChatApiService {
    // For desktop, we use HTTP-based implementation (Ollama)
    // Create HttpClient using NetworkModule for consistency
    val httpClient = NetworkModule.createHttpClient(enableLogging = true)
    return LocalChatApiService(
        httpClient = httpClient,
        json = json,
        baseUrl = baseUrl ?: "http://localhost:11434",
        modelName = modelName ?: "llama3.1:8b"
    )
}
