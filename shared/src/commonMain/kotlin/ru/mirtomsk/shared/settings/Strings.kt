package ru.mirtomsk.shared.settings

object Strings {
    // Agent selection
    const val AGENT_SELECTION_TITLE = "Выбор агента"
    const val AGENT_1 = "агент 1"
    const val AGENT_1_STEP_BY_STEP = "агент 1 пошаговый"
    const val AGENT_2 = "агент 2"
    const val AGENT_GROUP = "группа агентов"

    fun getAgentName(agentType: AgentType): String {
        return when (agentType) {
            AgentType.AGENT_1 -> AGENT_1
            AgentType.AGENT_1_STEP_BY_STEP -> AGENT_1_STEP_BY_STEP
            AgentType.AGENT_2 -> AGENT_2
            AgentType.AGENT_GROUP -> AGENT_GROUP
        }
    }
}
