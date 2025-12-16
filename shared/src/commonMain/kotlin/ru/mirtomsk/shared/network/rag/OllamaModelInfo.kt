package ru.mirtomsk.shared.network.rag

import kotlinx.serialization.Serializable

/**
 * DTO для информации о модели Ollama
 * Используется для получения информации о контекстном окне модели
 */
@Serializable
data class OllamaModelInfoResponse(
    val modelfile: String? = null,
    val parameters: String? = null,
    val template: String? = null,
    val details: OllamaModelDetails? = null,
)

@Serializable
data class OllamaModelDetails(
    val format: String? = null,
    val family: String? = null,
    val families: List<String>? = null,
    val parameter_size: String? = null,
    val quantization_level: String? = null,
    val context_size: Long? = null, // Размер контекстного окна в токенах
)
