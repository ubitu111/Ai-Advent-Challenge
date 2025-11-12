package ru.mirtomsk.shared.network.agent

/**
 * Unified model type enum for all AI models (Yandex GPT and HuggingFace)
 * Supports both Yandex GPT models and HuggingFace models
 */
enum class AgentTypeDto(
    val modelId: String,
    val level: ModelLevel,
) {
    // Yandex GPT models
    LITE(
        modelId = "yandexgpt-lite",
        level = ModelLevel.MIDDLE,
    ),
    PRO(
        modelId = "yandexgpt",
        level = ModelLevel.TOP,
    ),

    SAO10(
        modelId = "Sao10K/L3-8B-Stheno-v3.2",
        level = ModelLevel.TOP,
    ),

    QWEN05B(
        modelId = "sakalaka/Qwen2.5-0.5B-Instruct-Gensyn-Swarm-snappy_burrowing_skunk",
        level = ModelLevel.TOP,
    ),

    QWEN7B(
        modelId = "Qwen/Qwen2.5-7B-Instruct",
        level = ModelLevel.TOP,
    ),

    // HuggingFace models - Top level
    MISTRAL_7B_INSTRUCT(
        modelId = "mistralai/Mistral-7B-Instruct-v0.2",
        level = ModelLevel.TOP,
    ),

    OPENAI_OSS_120B(
        modelId = "openai/gpt-oss-120b:fastest",
        level = ModelLevel.TOP,
    ),

    // HuggingFace models - Middle level
    GPT_J_6B(
        modelId = "EleutherAI/gpt-j-6b",
        level = ModelLevel.MIDDLE,
    ),

    DIALOGPT_MEDIUM(
        modelId = "microsoft/DialoGPT-medium",
        level = ModelLevel.MIDDLE,
    ),

    // HuggingFace models - Basic level
    TINYLLAMA_1_1B(
        modelId = "TinyLlama/TinyLlama-1.1B-Chat-v1.0",
        level = ModelLevel.BASIC,
    ),

    GPT2(
        modelId = "gpt2",
        level = ModelLevel.BASIC,
    ),

    DISTILGPT2(
        modelId = "distilgpt2",
        level = ModelLevel.BASIC,
    );

    /**
     * Проверяет, является ли модель HuggingFace моделью
     */
    val isHuggingFace: Boolean
        get() = this != LITE && this != PRO

    /**
     * Проверяет, является ли модель Yandex GPT моделью
     */
    val isYandexGpt: Boolean
        get() = this == LITE || this == PRO

    /**
     * URL для HuggingFace Chat API (только для HuggingFace моделей)
     * Использует новый router API endpoint с Chat API форматом
     * Model указывается в теле запроса, а не в URL
     */
    val huggingFaceApiUrl: String?
        get() = if (isHuggingFace) {
            "https://router.huggingface.co/v1/chat/completions"
        } else {
            null
        }
}

/**
 * Уровень натренированности модели
 */
enum class ModelLevel {
    TOP,     // Топ-уровень (высокое качество, большие модели)
    MIDDLE,  // Средний уровень (баланс качества и скорости)
    BASIC    // Базовый уровень (компактные модели)
}
