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
    private val ragReranker: RagReranker,
    private val ragRerankingProvider: RagRerankingProvider,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Data class to store chunk with its source file information
     */
    private data class ChunkWithSource(
        val chunk: TextChunk,
        val fileName: String,
        val similarity: Float
    )

    /**
     * Retrieves relevant context for the user query
     * Uses vector search first, then applies reranking for better accuracy
     * @param query User's question
     * @param initialTopK Number of top chunks to retrieve before reranking (default: 20)
     * @param finalTopK Number of top chunks to return after reranking (default: 5)
     * @return Formatted text with relevant chunks, or null if no chunks found
     */
    suspend fun retrieveRelevantContext(
        query: String,
        initialTopK: Int = 20,
        finalTopK: Int = 5
    ): String? = withContext(ioDispatcher) {
        // Get embeddings for the query
        val queryEmbeddings = ollamaApiService.embed(query)
        // Use the same normalization as stored embeddings for consistency
        val normalizedQueryEmbeddings = embeddingsNormalizer.normalize(queryEmbeddings)

        // Get all cached embeddings
        val allResults = embeddingsCache.getAllResults()
        if (allResults.isEmpty()) {
            return@withContext null
        }

        // Step 1: Vector search - Calculate similarity for all chunks
        // Store chunks with their source file information
        val chunksWithSimilarity = mutableListOf<ChunkWithSource>()

        for (result in allResults) {
            for (chunk in result.chunks) {
                val similarity = cosineSimilarity(
                    normalizedQueryEmbeddings,
                    chunk.embeddings.toFloatArray()
                )
                chunksWithSimilarity.add(
                    ChunkWithSource(
                        chunk,
                        result.metadata.fileName,
                        similarity
                    )
                )
            }
        }

        // Sort by similarity (descending) and take initial top K
        val initialTopChunks = chunksWithSimilarity
            .filter { it.similarity > 0.7 }
            .sortedByDescending { it.similarity }
            .take(initialTopK)

        if (initialTopChunks.isEmpty()) {
            return@withContext null
        }

        // Step 2: Reranking - Re-rank the initial top chunks using LLM (if enabled)
        val finalChunks = if (ragRerankingProvider.isRerankingEnabled.value) {
            val chunksToRerank = initialTopChunks.map { it.chunk }
            val rerankedChunks = ragReranker.rerank(query, chunksToRerank, finalTopK)

            if (rerankedChunks.isEmpty()) {
                return@withContext null
            }

            // Map reranked chunks back to ChunkWithSource and sort by reranking score
            val rerankedChunkIds = rerankedChunks.map { it.first.id }.toSet()
            initialTopChunks.filter { it.chunk.id in rerankedChunkIds }
                .sortedByDescending { chunkWithSource ->
                    rerankedChunks.find { it.first.id == chunkWithSource.chunk.id }?.second ?: 0f
                }
                .take(finalTopK)
        } else {
            // If reranking is disabled, just take top K from initial results
            initialTopChunks.take(finalTopK)
        }

        // Format the context
        formatContext(finalChunks)
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
    private fun formatContext(chunks: List<ChunkWithSource>): String {
        val contextBuilder = StringBuilder()
        contextBuilder.append("=== РЕЛЕВАНТНЫЙ КОНТЕКСТ ИЗ БАЗЫ ЗНАНИЙ ===\n\n")
        contextBuilder.append("Используй следующую информацию из базы знаний для ответа на вопрос пользователя:\n\n")

        chunks.forEachIndexed { index, chunkWithSource ->
            val chunk = chunkWithSource.chunk
            val fileName = chunkWithSource.fileName
            // Extract chunk number from ID (e.g., "chunk_3" -> "3")
            val chunkNumber = chunk.id.replace("chunk_", "")

            contextBuilder.append("--- Фрагмент ${index + 1} ---\n")
            contextBuilder.append("Источник: файл $fileName, чанк №$chunkNumber\n")
            contextBuilder.append(chunk.text)
            contextBuilder.append("\n\n")
        }

        contextBuilder.append("=== КОНЕЦ КОНТЕКСТА ===\n\n")
        contextBuilder.append("КРИТИЧЕСКИ ВАЖНО: При ответе на вопрос пользователя ОБЯЗАТЕЛЬНО указывай источники информации. ")
        contextBuilder.append("Для каждого факта или информации, взятой из предоставленного контекста, ты ДОЛЖЕН указать источник в формате: ")
        contextBuilder.append("\"Ответ взят из файла [имя файла], чанк №[номер чанка]\". ")
        contextBuilder.append("Например: \"Ответ взят из файла document.txt, чанк №3\". ")
        contextBuilder.append("Если ты используешь информацию из нескольких источников, укажи все использованные источники. ")
        contextBuilder.append("Используй информацию из предоставленного контекста для ответа на вопрос пользователя. ")
        contextBuilder.append("Если в контексте нет информации, необходимой для полного ответа, можешь использовать свои знания, но приоритет отдавай информации из контекста.")

        return contextBuilder.toString()
    }
}
