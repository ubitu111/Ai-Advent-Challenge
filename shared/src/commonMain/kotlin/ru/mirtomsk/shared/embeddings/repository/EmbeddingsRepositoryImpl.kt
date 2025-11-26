package ru.mirtomsk.shared.embeddings.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.embeddings.model.EmbeddingsResult
import ru.mirtomsk.shared.embeddings.model.Metadata
import ru.mirtomsk.shared.embeddings.model.TextChunk
import ru.mirtomsk.shared.network.rag.OllamaApiService
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class EmbeddingsRepositoryImpl(
    private val ollamaApiService: OllamaApiService,
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
            val normalizedEmbeddings = normalizeEmbeddings(embeddings)

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

    /**
     * Нормализует эмбеддинги в диапазон от -1 до 1
     * Использует L2 нормализацию для получения единичного вектора
     */
    private fun normalizeEmbeddings(embeddings: FloatArray): FloatArray {
        // Вычисляем L2 норму
        val norm = sqrt(embeddings.sumOf { it.toDouble() * it.toDouble() }).toFloat()

        if (norm == 0f || norm.isNaN() || norm.isInfinite()) {
            return FloatArray(embeddings.size)
        }

        // L2 нормализация - делим каждый элемент на норму
        // Это дает единичный вектор, где значения уже в разумном диапазоне
        // Для приведения к [-1, 1] используем min-max нормализацию
        val l2Normalized = embeddings.map { it / norm }

        val min = l2Normalized.minOrNull() ?: 0f
        val max = l2Normalized.maxOrNull() ?: 0f

        if (max == min || max.isNaN() || min.isNaN()) {
            return l2Normalized.toFloatArray()
        }

        // Масштабируем от [min, max] к [-1, 1]
        return l2Normalized.map { value ->
            if (value.isNaN() || value.isInfinite()) {
                0f
            } else {
                ((value - min) / (max - min)) * 2f - 1f
            }
        }.toFloatArray()
    }

    private companion object {
        const val TARGET_CHUNK_SIZE = 50 // Средний размер чанка (500-600 токенов)
        const val OVERLAP_SIZE = 5 // Перекрытие (50-60 токенов)
    }

    private data class ChunkInfo(
        val text: String,
        val wordCount: Int,
        val tokenCount: Int
    )
}
