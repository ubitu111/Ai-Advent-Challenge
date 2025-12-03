package ru.mirtomsk.shared.settings

import ru.mirtomsk.shared.settings.model.SystemPrompt

object Strings {
    // Settings screen
    const val SETTINGS_TITLE = "Настройки"
    const val RESPONSE_FORMAT_TITLE = "Формат ответа"
    const val TEMPERATURE_TITLE = "Температура"
    const val TEMPERATURE_PLACEHOLDER = "0.0 - 1.0"
    const val MAX_TOKENS_TITLE = "Максимальное количество токенов"
    const val MAX_TOKENS_PLACEHOLDER = "1 - 100000"
    const val CONTEXT_COMPRESSION_TITLE = "Сжатие контекста"
    const val COMPRESSION_ENABLED = "Включено"
    const val COMPRESSION_DISABLED = "Выключено"
    const val CLOSE_BUTTON = "Закрыть"
    const val DROPDOWN_ARROW = "▼"

    // Response format options
    const val DEFAULT_FORMAT = "дефолт"
    const val JSON_FORMAT = "json"

    // Default values
    const val DEFAULT_TEMPERATURE = "0"
    const val DEFAULT_MAX_TOKENS = "2000"

    // System prompt selection
    const val DEFAULT = "Дефолт"
    const val SYSTEM_PROMPT_SELECTION_TITLE = "Системный промпт"
    const val SPECIFYING_QUESTIONS = "Уточняющие вопросы"
    const val LOGIC_SIMPLE = "Логика - простой"
    const val LOGIC_BY_STEP = "Логика - пошаговый"
    const val LOGIC_AGENT_GROUP = "Логика - группа агентов"

    // Context reset
    const val RESET_CONTEXT_BUTTON = "Сбросить контекст"

    fun getSystemPromptName(systemPrompt: SystemPrompt): String {
        return when (systemPrompt) {
            SystemPrompt.DEFAULT -> DEFAULT
            SystemPrompt.SPECIFYING_QUESTIONS -> SPECIFYING_QUESTIONS
            SystemPrompt.LOGIC_BY_STEP -> LOGIC_BY_STEP
            SystemPrompt.LOGIC_AGENT_GROUP -> LOGIC_AGENT_GROUP
            SystemPrompt.LOGIC_SIMPLE -> LOGIC_SIMPLE
        }
    }
}
