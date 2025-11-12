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

    // HuggingFace models - Top level
    MISTRAL_7B_INSTRUCT(
        modelId = "mistralai/Mistral-7B-Instruct-v0.2",
        level = ModelLevel.TOP,
    ),

    BLOOM_7B1(
        modelId = "bigscience/bloom-7b1",
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
     * URL для HuggingFace Inference API (только для HuggingFace моделей)
     * Использует новый router API endpoint вместо устаревшего api-inference.huggingface.co
     */
    val huggingFaceApiUrl: String?
        get() = if (isHuggingFace) {
            "https://router.huggingface.co/hf-inference/models/$modelId"
        } else {
            null
        }

    /**
     * Получить модели по уровню
     */
    companion object {

        fun getByLevel(level: ModelLevel): List<AgentTypeDto> {
            return entries.filter { it.level == level }
        }

        /**
         * Рекомендуемая тройка для сравнения:
         * - Топ: Mistral или BLOOM
         * - Средний: GPT-J или DialoGPT
         * - Базовый: TinyLlama или GPT-2
         */
        fun getRecommendedForComparison(): List<AgentTypeDto> {
            return listOf(
                MISTRAL_7B_INSTRUCT,  // Топ
                GPT_J_6B,              // Средний
                TINYLLAMA_1_1B         // Базовый
            )
        }

        /**
         * Получить все HuggingFace модели
         */
        fun getHuggingFaceModels(): List<AgentTypeDto> {
            return entries.filter { it.isHuggingFace }
        }

        /**
         * Получить все Yandex GPT модели
         */
        fun getYandexGptModels(): List<AgentTypeDto> {
            return entries.filter { it.isYandexGpt }
        }
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
