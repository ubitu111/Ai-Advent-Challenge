package ru.mirtomsk.shared.settings

import ru.mirtomsk.shared.settings.model.AgentType
import ru.mirtomsk.shared.settings.model.SystemPrompt

object Strings {
    // Agent selection
    const val AGENT_SELECTION_TITLE = "Выбор агента"
    const val LITE = "YandexGpt Lite"
    const val PRO = "YandexGpt Pro"
    
    // System prompt selection
    const val SYSTEM_PROMPT_SELECTION_TITLE = "Системный промпт"
    const val SPECIFYING_QUESTIONS = "Уточняющие вопросы"
    const val LOGIC_SIMPLE = "Логика - простой"
    const val LOGIC_BY_STEP = "Логика - пошаговый"
    const val LOGIC_AGENT_GROUP = "Логика - группа агентов"

    fun getAgentName(agentType: AgentType): String {
        return when (agentType) {
            AgentType.LITE -> LITE
            AgentType.PRO -> PRO
        }
    }
    
    fun getSystemPromptName(systemPrompt: SystemPrompt): String {
        return when (systemPrompt) {
            SystemPrompt.SPECIFYING_QUESTIONS -> SPECIFYING_QUESTIONS
            SystemPrompt.LOGIC_BY_STEP -> LOGIC_BY_STEP
            SystemPrompt.LOGIC_AGENT_GROUP -> LOGIC_AGENT_GROUP
            SystemPrompt.LOGIC_SIMPLE -> LOGIC_SIMPLE
        }
    }
}
