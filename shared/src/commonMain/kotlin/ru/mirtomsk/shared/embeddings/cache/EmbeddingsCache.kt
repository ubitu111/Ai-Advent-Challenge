package ru.mirtomsk.shared.embeddings.cache

import ru.mirtomsk.shared.embeddings.model.EmbeddingsResult

/**
 * Интерфейс для кеша результатов обработки эмбеддингов
 */
interface EmbeddingsCache {
    /**
     * Сохранить результат обработки в кеш
     */
    suspend fun saveResult(result: EmbeddingsResult)

    /**
     * Получить результат обработки по имени файла
     */
    suspend fun getResult(fileName: String): EmbeddingsResult?

    /**
     * Получить все сохраненные результаты
     */
    suspend fun getAllResults(): List<EmbeddingsResult>

    /**
     * Удалить результат по имени файла
     */
    suspend fun deleteResult(fileName: String)
}
