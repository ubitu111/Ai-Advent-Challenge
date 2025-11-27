package ru.mirtomsk.shared.network.rag

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class OllamaApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {

    suspend fun embed(text: String): FloatArray {
        val response: OllamaEmbeddingsResponse = httpClient.post("$baseUrl/api/embeddings") {
            contentType(ContentType.Application.Json)
            setBody(
                OllamaEmbeddingsRequest(
                    model = OllamaEmbeddingsRequest.BGE_MODEL,
                    prompt = text,
                )
            )
        }.body()
        return response.embedding.map { it.toFloat() }.toFloatArray()
    }
}
