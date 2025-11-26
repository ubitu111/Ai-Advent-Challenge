package ru.mirtomsk.shared.network.rag

import kotlinx.serialization.Serializable

@Serializable
data class OllamaEmbeddingsRequest(
    val model: String = "nomic-embed-text",
    val prompt: String,
)

@Serializable
data class OllamaEmbeddingsResponse(
    val embedding: List<Double>,
)
