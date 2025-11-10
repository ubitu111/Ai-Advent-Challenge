package ru.mirtomsk.shared.settings

import ru.mirtomsk.shared.settings.model.AgentType

object Strings {
    // Agent selection
    const val AGENT_SELECTION_TITLE = "Выбор агента"
    const val LITE = "YandexGpt Lite"
    const val BY_STEP = "YandexGpt Lite пошаговый"
    const val PRO = "PRO"
    const val AGENT_GROUP = "Группа агентов"

    fun getAgentName(agentType: AgentType): String {
        return when (agentType) {
            AgentType.LITE -> LITE
            AgentType.BY_STEP -> BY_STEP
            AgentType.PRO -> PRO
            AgentType.AGENT_GROUP -> AGENT_GROUP
        }
    }
}
