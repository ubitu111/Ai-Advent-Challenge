package ru.mirtomsk.shared.network.rag

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.embeddings.EmbeddingsNormalizer
import ru.mirtomsk.shared.embeddings.cache.EmbeddingsCache
import ru.mirtomsk.shared.embeddings.model.TextChunk
import kotlin.math.sqrt

/**
 * Service for Retrieval-Augmented Generation (RAG)
 * Finds the most relevant text chunks based on user query using embeddings similarity
 */
class RagService(
    private val ollamaApiService: OllamaApiService,
    private val embeddingsCache: EmbeddingsCache,
    private val embeddingsNormalizer: EmbeddingsNormalizer,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Retrieves relevant context for the user query
     * @param query User's question
     * @param topK Number of top chunks to retrieve (default: 5)
     * @return Formatted text with relevant chunks, or null if no chunks found
     */
    suspend fun retrieveRelevantContext(query: String, topK: Int = 5): String? =
        withContext(ioDispatcher) {
            // Get embeddings for the query
            val queryEmbeddings = ollamaApiService.embed(query)
            // Use the same normalization as stored embeddings for consistency
            val normalizedQueryEmbeddings = embeddingsNormalizer.normalize(queryEmbeddings)

            // Get all cached embeddings
            val allResults = embeddingsCache.getAllResults()
            if (allResults.isEmpty()) {
                return@withContext null
            }

            // Calculate similarity for all chunks
            val chunksWithSimilarity = mutableListOf<Pair<TextChunk, Float>>()

            for (result in allResults) {
                for (chunk in result.chunks) {
                    val similarity = cosineSimilarity(
                        normalizedQueryEmbeddings,
                        chunk.embeddings.toFloatArray()
                    )
                    chunksWithSimilarity.add(chunk to similarity)
                }
            }

            // Sort by similarity (descending) and take top K
            val topChunks = chunksWithSimilarity
                .sortedByDescending { it.second }
                .take(topK)
                .map { it.first }

            if (topChunks.isEmpty()) {
                return@withContext null
            }

            // Format the context
            formatContext(topChunks)
        }

    /**
     * Calculates cosine similarity between two embedding vectors
     */
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size) {
            return 0f
        }

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        val denominator = sqrt(norm1) * sqrt(norm2)
        if (denominator == 0f) {
            return 0f
        }

        return dotProduct / denominator
    }

    /**
     * Formats the retrieved chunks into a context string
     */
    private fun formatContext(chunks: List<TextChunk>): String {
        val contextBuilder = StringBuilder()
        contextBuilder.append("=== РЕЛЕВАНТНЫЙ КОНТЕКСТ ИЗ БАЗЫ ЗНАНИЙ ===\n\n")
        contextBuilder.append("Используй следующую информацию из базы знаний для ответа на вопрос пользователя:\n\n")

        chunks.forEachIndexed { index, chunk ->
            contextBuilder.append("--- Фрагмент ${index + 1} ---\n")
            contextBuilder.append(chunk.text)
            contextBuilder.append("\n\n")
        }

        contextBuilder.append("=== КОНЕЦ КОНТЕКСТА ===\n\n")
        contextBuilder.append("ВАЖНО: Используй информацию из предоставленного контекста для ответа на вопрос пользователя. ")
        contextBuilder.append("Если в контексте нет информации, необходимой для полного ответа, можешь использовать свои знания, но приоритет отдавай информации из контекста.")

        return contextBuilder.toString()
    }
}
