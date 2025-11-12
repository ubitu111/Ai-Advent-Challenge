package ru.mirtomsk.shared.network.huggingface

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.agent.AgentTypeDto

/**
 * API service for HuggingFace Inference API
 * Returns raw response body - parsing should be done by mapper
 */
class HuggingFaceApiService(
    private val httpClient: HttpClient,
    private val apiConfig: ApiConfig,
) {

    /**
     * Request model and return the raw response body
     * Response parsing should be handled by HuggingFaceResponseMapper
     */
    suspend fun requestModel(
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

