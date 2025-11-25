package ru.mirtomsk.shared.embeddings.model

import kotlinx.serialization.Serializable

@Serializable
data class EmbeddingsResult(
    val metadata: Metadata,
    val chunks: List<TextChunk>
)

@Serializable
data class Metadata(
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val totalChunks: Int,
    val tokensPerChunk: Int
)

@Serializable
data class TextChunk(
    val id: String,
    val text: String,
    val wordCount: Int,
    val tokenCount: Int,
    val embeddings: List<Float>
)
