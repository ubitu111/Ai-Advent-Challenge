package ru.mirtomsk.shared.network

import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.model.AiRequest

/**
 * Factory function for creating platform-specific LocalChatApiService implementations
 */
expect fun createLocalChatApiService(
    json: Json,
    baseUrl: String? = null,
    modelName: String? = null,
): ILocalChatApiService

/**
 * Base interface for local chat API service
 * Platform-specific implementations will provide on-device model inference
 */
interface ILocalChatApiService {
    /**
     * Request local LLM model with streaming response support
     */
    fun requestLocalLlmStream(request: AiRequest): kotlinx.coroutines.flow.Flow<String>
    
    /**
     * Request local LLM model and return the full response (non-streaming)
     */
    suspend fun requestLocalLlm(request: AiRequest): String
}
