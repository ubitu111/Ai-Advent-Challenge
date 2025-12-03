package ru.mirtomsk.shared.network.agent

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider for agent type settings
 * Manages the current agent type and provides it as a Flow
 */
class AgentTypeProvider {
    private val _agentType = MutableStateFlow(AgentTypeDto.PRO)
    val agentType: StateFlow<AgentTypeDto> = _agentType.asStateFlow()

    fun updateAgentType(agentType: AgentTypeDto) {
        _agentType.value = agentType
    }
}
