package ru.mirtomsk.shared.settings

import ru.mirtomsk.shared.settings.model.AgentType
import ru.mirtomsk.shared.settings.model.SystemPrompt

object Strings {
    // Settings screen
    const val SETTINGS_TITLE = "Настройки"
    const val RESPONSE_FORMAT_TITLE = "Формат ответа"
    const val TEMPERATURE_TITLE = "Температура"
    const val TEMPERATURE_PLACEHOLDER = "0.0 - 1.0"
    const val CLOSE_BUTTON = "Закрыть"
    const val DROPDOWN_ARROW = "▼"
    
    // Response format options
    const val DEFAULT_FORMAT = "дефолт"
    const val JSON_FORMAT = "json"
    
    // Default values
    const val DEFAULT_TEMPERATURE = "0"
    
    // Agent selection
    const val AGENT_SELECTION_TITLE = "Выбор агента"
    
    // Yandex GPT models
    const val LITE = "Yandex GPT Lite"
    const val PRO = "Yandex GPT Pro"
    
    // HuggingFace models - Top level
    const val MISTRAL_7B_INSTRUCT = "Mistral 7B Instruct"
    const val OPENAI_OSS_120B = "OPENAI OSS 120B"
    
    // HuggingFace models - Middle level
    const val GPT_J_6B = "GPT-J 6B"
    const val DIALOGPT_MEDIUM = "DialoGPT Medium"
    
    // HuggingFace models - Basic level
    const val TINYLLAMA_1_1B = "TinyLlama 1.1B Chat"
    const val GPT2 = "GPT-2"
    const val DISTILGPT2 = "DistilGPT-2"

    const val SAO10 = "SAO-10"

    const val QWEN05B = "Qwen-0.5B"

    const val QWEN7B = "Qwen-7B"
    
    // System prompt selection
    const val DEFAULT = "Дефолт"
    const val SYSTEM_PROMPT_SELECTION_TITLE = "Системный промпт"
    const val SPECIFYING_QUESTIONS = "Уточняющие вопросы"
    const val LOGIC_SIMPLE = "Логика - простой"
    const val LOGIC_BY_STEP = "Логика - пошаговый"
    const val LOGIC_AGENT_GROUP = "Логика - группа агентов"
    
    // Context reset
    const val RESET_CONTEXT_BUTTON = "Сбросить контекст"

    fun getAgentName(agentType: AgentType): String {
        return when (agentType) {
            // Yandex GPT models
            AgentType.LITE -> LITE
            AgentType.PRO -> PRO
            
            // HuggingFace models - Top level
            AgentType.MISTRAL_7B_INSTRUCT -> MISTRAL_7B_INSTRUCT
            AgentType.OPENAI_OSS_120B -> OPENAI_OSS_120B
            
            // HuggingFace models - Middle level
            AgentType.GPT_J_6B -> GPT_J_6B
            AgentType.DIALOGPT_MEDIUM -> DIALOGPT_MEDIUM
            
            // HuggingFace models - Basic level
            AgentType.TINYLLAMA_1_1B -> TINYLLAMA_1_1B
            AgentType.GPT2 -> GPT2
            AgentType.DISTILGPT2 -> DISTILGPT2
            AgentType.SAO10 -> SAO10
            AgentType.QWEN05B -> QWEN05B
            AgentType.QWEN7B -> QWEN7B
        }
    }
    
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
