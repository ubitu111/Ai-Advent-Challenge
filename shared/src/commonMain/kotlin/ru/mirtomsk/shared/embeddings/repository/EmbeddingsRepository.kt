package ru.mirtomsk.shared.embeddings.repository

import ru.mirtomsk.shared.embeddings.model.EmbeddingsResult

/**
 * Repository interface for embeddings operations
 */
interface EmbeddingsRepository {
    suspend fun processText(
        text: String,
        fileName: String,
        filePath: String
    ): EmbeddingsResult
}
