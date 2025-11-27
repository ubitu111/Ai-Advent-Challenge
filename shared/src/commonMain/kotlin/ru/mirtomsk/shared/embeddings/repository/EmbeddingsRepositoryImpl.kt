package ru.mirtomsk.shared.embeddings.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.embeddings.EmbeddingsNormalizer
import ru.mirtomsk.shared.embeddings.model.EmbeddingsResult
import ru.mirtomsk.shared.embeddings.model.Metadata
import ru.mirtomsk.shared.embeddings.model.TextChunk
import ru.mirtomsk.shared.network.rag.OllamaApiService
import kotlin.math.max
import kotlin.math.min

class EmbeddingsRepositoryImpl(
    private val ollamaApiService: OllamaApiService,
    private val embeddingsNormalizer: EmbeddingsNormalizer,
    private val ioDispatcher: CoroutineDispatcher,
) : EmbeddingsRepository {

    override suspend fun processText(
        text: String,
        fileName: String,
        filePath: String
    ): EmbeddingsResult = withContext(ioDispatcher) {
        // Разбиваем текст на чанки
        val chunks = splitIntoChunks(text)

        // Запрашиваем эмбеддинги для каждого чанка
        val chunksWithEmbeddings = chunks.mapIndexed { index, chunk ->
            val embeddings = ollamaApiService.embed(chunk.text)
            val normalizedEmbeddings = embeddingsNormalizer.normalize(embeddings)

            TextChunk(
                id = "chunk_${index + 1}",
                text = chunk.text,
                wordCount = chunk.wordCount,
                tokenCount = chunk.tokenCount,
                embeddings = normalizedEmbeddings.toList()
            )
        }

        // Создаем метаданные
        val metadata = Metadata(
            fileName = fileName,
            filePath = filePath,
            timestamp = System.currentTimeMillis(),
            totalChunks = chunksWithEmbeddings.size,
            tokensPerChunk = TARGET_CHUNK_SIZE
        )

        EmbeddingsResult(
            metadata = metadata,
            chunks = chunksWithEmbeddings
        )
    }

    /**
     * Разбивает текст на чанки с перекрытием
     */
    private fun splitIntoChunks(text: String): List<ChunkInfo> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val chunks = mutableListOf<ChunkInfo>()
        var currentIndex = 0

        while (currentIndex < words.size) {
            // Определяем размер чанка (500-600 токенов)
            val chunkSize = TARGET_CHUNK_SIZE
            val overlapSize = OVERLAP_SIZE

            // Берем слова для текущего чанка
            val endIndex = min(currentIndex + estimateWordsForTokens(chunkSize), words.size)
            val chunkWords = words.subList(currentIndex, endIndex)
            val chunkText = chunkWords.joinToString(" ")

            val wordCount = chunkWords.size
            val tokenCount = estimateTokens(chunkText)

            chunks.add(ChunkInfo(chunkText, wordCount, tokenCount))

            // Перемещаемся вперед с учетом перекрытия
            if (endIndex >= words.size) break
            val overlapWords = estimateWordsForTokens(overlapSize)
            currentIndex = max(currentIndex + 1, endIndex - overlapWords)
        }

        return chunks
    }

    /**
     * Оценивает количество слов для заданного количества токенов
     */
    private fun estimateWordsForTokens(tokens: Int): Int {
        // Примерно 0.75 слова на токен
        return (tokens * 0.75).toInt()
    }

    /**
     * Оценивает количество токенов в тексте
     */
    private fun estimateTokens(text: String): Int {
        // Примерно 1 токен на 4 символа или 0.75 слова
        val wordCount = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val charCount = text.length
        return max((wordCount / 0.75).toInt(), charCount / 4)
    }


    private companion object {
        const val TARGET_CHUNK_SIZE = 100 // Средний размер чанка (500-600 токенов)
        const val OVERLAP_SIZE = 10 // Перекрытие (50-60 токенов)
    }

    private data class ChunkInfo(
        val text: String,
        val wordCount: Int,
        val tokenCount: Int
    )
}
