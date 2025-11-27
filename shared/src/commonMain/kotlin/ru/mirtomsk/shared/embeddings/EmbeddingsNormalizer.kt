package ru.mirtomsk.shared.embeddings

import kotlin.math.sqrt

/**
 * Utility class for normalizing embeddings
 * Provides unified normalization logic across the application
 */
class EmbeddingsNormalizer {
    /**
     * Нормализует эмбеддинги в диапазон от -1 до 1
     * Использует L2 нормализацию для получения единичного вектора,
     * затем применяет min-max нормализацию для приведения к диапазону [-1, 1]
     *
     * @param embeddings Массив эмбеддингов для нормализации
     * @return Нормализованный массив эмбеддингов
     */
    fun normalize(embeddings: FloatArray): FloatArray {
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

    /**
     * Нормализует эмбеддинги используя только L2 нормализацию
     * Используется для косинусного сходства, где важна только направленность вектора
     *
     * @param embeddings Массив эмбеддингов для нормализации
     * @return L2 нормализованный массив эмбеддингов (единичный вектор)
     */
    fun normalizeL2(embeddings: FloatArray): FloatArray {
        val norm = sqrt(embeddings.sumOf { it.toDouble() * it.toDouble() }).toFloat()

        if (norm == 0f || norm.isNaN() || norm.isInfinite()) {
            return FloatArray(embeddings.size)
        }

        return embeddings.map { it / norm }.toFloatArray()
    }
}

