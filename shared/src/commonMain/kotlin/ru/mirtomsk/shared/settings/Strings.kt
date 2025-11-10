package ru.mirtomsk.shared.settings

import ru.mirtomsk.shared.settings.model.AgentType

object Strings {
    // Agent selection
    const val AGENT_SELECTION_TITLE = "Выбор агента"
    const val LITE = "YandexGpt Lite"
    const val LITE_BY_STEP = "YandexGpt Lite пошаговый"
    const val QWEN = "QWEN"
    const val AGENT_GROUP = "Группа агентов"

    fun getAgentName(agentType: AgentType): String {
        return when (agentType) {
            AgentType.LITE -> LITE
            AgentType.LITE_BY_STEP -> LITE_BY_STEP
            AgentType.QWEN -> QWEN
            AgentType.AGENT_GROUP -> AGENT_GROUP
        }
    }
}
