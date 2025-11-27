package ru.mirtomsk.shared.network.rag

import kotlinx.serialization.Serializable

@Serializable
data class OllamaEmbeddingsRequest(
    val model: String,
    val prompt: String,
) {
    companion object {
        const val NOMIC_MODEL = "nomic-embed-text"
        const val BGE_MODEL = "bge-m3"
    }
}

@Serializable
data class OllamaEmbeddingsResponse(
    val embedding: List<Double>,
)
