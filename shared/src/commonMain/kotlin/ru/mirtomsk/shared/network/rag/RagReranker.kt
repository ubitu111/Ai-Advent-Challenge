package ru.mirtomsk.shared.network.rag

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import ru.mirtomsk.shared.embeddings.model.TextChunk

/**
 * Service for reranking retrieved chunks to improve relevance
 * Uses LLM to score each chunk's relevance to the user query
 */
class RagReranker(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Reranks chunks based on their relevance to the query
     * @param query User's question
     * @param chunks List of chunks to rerank
     * @param topK Number of top chunks to return after reranking (default: 5)
     * @return Reranked list of chunks sorted by relevance score (descending)
     */
    suspend fun rerank(
        query: String,
        chunks: List<TextChunk>,
        topK: Int = 5
    ): List<Pair<TextChunk, Float>> = withContext(ioDispatcher) {
        if (chunks.isEmpty()) {
            return@withContext emptyList()
        }

        // Score each chunk's relevance to the query
        val scoredChunks = coroutineScope {
            chunks.map { chunk ->
                async {
                    val score = scoreRelevance(query, chunk.text)
                    chunk to score
                }
            }.awaitAll()
        }

        // Sort by score (descending) and take top K
        scoredChunks
            .sortedByDescending { it.second }
            .take(topK)
    }

    /**
     * Scores the relevance of a chunk to the query using LLM
     * @param query User's question
     * @param chunkText Text content of the chunk
     * @return Relevance score from 0.0 to 1.0
     */
    private suspend fun scoreRelevance(query: String, chunkText: String): Float {
        val prompt = buildRerankingPrompt(query, chunkText)

        return try {
            val response: OllamaRerankResponse = httpClient.post("$baseUrl/api/generate") {
                contentType(ContentType.Application.Json)
                setBody(
                    OllamaRerankRequest(
                        model = "llama3.2", // Можно сделать настраиваемым
                        prompt = prompt,
                        stream = false,
                        options = OllamaRerankOptions(
                            temperature = 0.0, // Низкая температура для более детерминированных оценок
                            num_predict = 10, // Ограничиваем длину ответа
                        )
                    )
                )
            }.body()

            // Парсим оценку из ответа
            parseScore(response.response)
        } catch (e: Exception) {
            // В случае ошибки возвращаем средний балл
            0.5f
        }
    }

    /**
     * Builds a prompt for reranking evaluation
     */
    private fun buildRerankingPrompt(query: String, chunkText: String): String {
        return """
            Оцени релевантность следующего текстового фрагмента относительно вопроса пользователя.
            Ответь ТОЛЬКО числом от 0.0 до 1.0, где:
            - 1.0 = фрагмент полностью отвечает на вопрос
            - 0.5 = фрагмент частично релевантен
            - 0.0 = фрагмент не релевантен вопросу

            Вопрос пользователя: $query

            Текстовый фрагмент:
            $chunkText

            Оценка релевантности (только число от 0.0 до 1.0):
        """.trimIndent()
    }

    /**
     * Parses relevance score from LLM response
     */
    private fun parseScore(response: String): Float {
        // Ищем число от 0.0 до 1.0 в ответе
        val scoreRegex = Regex("""([0-1](?:\.[0-9]+)?)""")
        val match = scoreRegex.find(response.trim())

        return match?.groupValues?.get(1)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f
    }
}

@Serializable
data class OllamaRerankRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean,
    val options: OllamaRerankOptions,
)

@Serializable
data class OllamaRerankOptions(
    val temperature: Double,
    val num_predict: Int,
)

@Serializable
data class OllamaRerankResponse(
    val model: String? = null,
    val created_at: String? = null,
    val response: String,
    val done: Boolean? = null,
)
