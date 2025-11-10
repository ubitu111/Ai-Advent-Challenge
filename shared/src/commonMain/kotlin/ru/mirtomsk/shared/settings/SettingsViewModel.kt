package ru.mirtomsk.shared.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.mirtomsk.shared.network.agent.AgentTypeDto
import ru.mirtomsk.shared.network.agent.AgentTypeProvider
import ru.mirtomsk.shared.network.format.ResponseFormat
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.settings.model.AgentType
import ru.mirtomsk.shared.settings.model.SettingsUiState

class SettingsViewModel(
    private val formatProvider: ResponseFormatProvider,
    private val agentTypeProvider: AgentTypeProvider,
    mainDispatcher: CoroutineDispatcher,
) {
    private val viewmodelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        // Initialize UI state with current format and agent type from providers
        viewmodelScope.launch {
            val currentFormat = formatProvider.responseFormat.first()
            val currentAgentType = agentTypeProvider.agentType.first()
            uiState = uiState.copy(
                responseFormat = formatToString(currentFormat),
                selectedAgent = agentTypeDtoToAgentType(currentAgentType)
            )
        }
    }

    fun setResponseFormat(formatString: String) {
        uiState = uiState.copy(responseFormat = formatString)
        val format = stringToFormat(formatString)
        formatProvider.updateFormat(format)
    }

    fun setSelectedAgent(agentType: AgentType) {
        uiState = uiState.copy(selectedAgent = agentType)
        val agentTypeDto = agentTypeToAgentTypeDto(agentType)
        agentTypeProvider.updateAgentType(agentTypeDto)
    }

    private fun formatToString(format: ResponseFormat): String {
        return when (format) {
            ResponseFormat.DEFAULT -> "дефолт"
            ResponseFormat.JSON -> "json"
        }
    }

    private fun stringToFormat(formatString: String): ResponseFormat {
        return when (formatString) {
            "дефолт" -> ResponseFormat.DEFAULT
            "json" -> ResponseFormat.JSON
            else -> ResponseFormat.DEFAULT
        }
    }

    private fun agentTypeToAgentTypeDto(agentType: AgentType): AgentTypeDto {
        return when (agentType) {
            AgentType.LITE -> AgentTypeDto.LITE
            AgentType.LITE_BY_STEP -> AgentTypeDto.LITE_BY_STEP
            AgentType.QWEN -> AgentTypeDto.QWEN
            AgentType.AGENT_GROUP -> AgentTypeDto.AGENT_GROUP
        }
    }

    private fun agentTypeDtoToAgentType(agentTypeDto: AgentTypeDto): AgentType {
        return when (agentTypeDto) {
            AgentTypeDto.LITE -> AgentType.LITE
            AgentTypeDto.LITE_BY_STEP -> AgentType.LITE_BY_STEP
            AgentTypeDto.QWEN -> AgentType.QWEN
            AgentTypeDto.AGENT_GROUP -> AgentType.AGENT_GROUP
        }
    }
}

