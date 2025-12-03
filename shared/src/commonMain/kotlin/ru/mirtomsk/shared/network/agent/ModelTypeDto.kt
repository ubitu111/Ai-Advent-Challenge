package ru.mirtomsk.shared.network.agent

/**
 * Model type enum for Yandex GPT models
 */
enum class ModelTypeDto(
    val modelId: String,
) {
    // Yandex GPT models
    LITE(
        modelId = "yandexgpt-lite",
    ),
    PRO(
        modelId = "yandexgpt",
    ),
}
