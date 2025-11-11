package ru.mirtomsk.shared.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.mirtomsk.shared.chat.context.ContextResetProvider
import ru.mirtomsk.shared.network.agent.AgentTypeDto
import ru.mirtomsk.shared.network.agent.AgentTypeProvider
import ru.mirtomsk.shared.network.format.ResponseFormat
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.prompt.SystemPromptDto
import ru.mirtomsk.shared.network.prompt.SystemPromptProvider
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.settings.model.AgentType
import ru.mirtomsk.shared.settings.model.SettingsUiState
import ru.mirtomsk.shared.settings.model.SystemPrompt

class SettingsViewModel(
    private val formatProvider: ResponseFormatProvider,
    private val agentTypeProvider: AgentTypeProvider,
    private val systemPromptProvider: SystemPromptProvider,
    private val contextResetProvider: ContextResetProvider,
    private val temperatureProvider: TemperatureProvider,
    mainDispatcher: CoroutineDispatcher,
) {
    private val viewmodelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        // Initialize UI state with current format, agent type, system prompt and temperature from providers
        viewmodelScope.launch {
            val currentFormat = formatProvider.responseFormat.first()
            val currentAgentType = agentTypeProvider.agentType.first()
            val currentSystemPrompt = systemPromptProvider.systemPrompt.first()
            val currentTemperature = temperatureProvider.temperature.first()
            uiState = uiState.copy(
                responseFormat = formatToString(currentFormat),
                selectedAgent = agentTypeDtoToAgentType(currentAgentType),
                selectedSystemPrompt = systemPromptDtoToSystemPrompt(currentSystemPrompt),
                temperature = currentTemperature.toString()
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

    fun setSelectedSystemPrompt(systemPrompt: SystemPrompt) {
        uiState = uiState.copy(selectedSystemPrompt = systemPrompt)
        val systemPromptDto = systemPromptToSystemPromptDto(systemPrompt)
        systemPromptProvider.updateSystemPrompt(systemPromptDto)
    }

    fun resetContext() {
        contextResetProvider.resetContext()
    }

    fun setTemperature(temperatureString: String) {
        uiState = uiState.copy(temperature = temperatureString)
        val temperature = temperatureString.toFloatOrNull() ?: 0f
        temperatureProvider.updateTemperature(temperature)
    }

    private fun formatToString(format: ResponseFormat): String {
        return when (format) {
            ResponseFormat.DEFAULT -> Strings.DEFAULT_FORMAT
            ResponseFormat.JSON -> Strings.JSON_FORMAT
        }
    }

    private fun stringToFormat(formatString: String): ResponseFormat {
        return when (formatString) {
            Strings.DEFAULT_FORMAT -> ResponseFormat.DEFAULT
            Strings.JSON_FORMAT -> ResponseFormat.JSON
            else -> ResponseFormat.DEFAULT
        }
    }

    private fun agentTypeToAgentTypeDto(agentType: AgentType): AgentTypeDto {
        return when (agentType) {
            AgentType.LITE -> AgentTypeDto.LITE
            AgentType.PRO -> AgentTypeDto.PRO
        }
    }

    private fun agentTypeDtoToAgentType(agentTypeDto: AgentTypeDto): AgentType {
        return when (agentTypeDto) {
            AgentTypeDto.LITE -> AgentType.LITE
            AgentTypeDto.PRO -> AgentType.PRO
        }
    }

    private fun systemPromptToSystemPromptDto(systemPrompt: SystemPrompt): SystemPromptDto {
        return when (systemPrompt) {
            SystemPrompt.DEFAULT -> SystemPromptDto.DEFAULT
            SystemPrompt.SPECIFYING_QUESTIONS -> SystemPromptDto.SPECIFYING_QUESTIONS
            SystemPrompt.LOGIC_BY_STEP -> SystemPromptDto.LOGIC_BY_STEP
            SystemPrompt.LOGIC_AGENT_GROUP -> SystemPromptDto.LOGIC_AGENT_GROUP
            SystemPrompt.LOGIC_SIMPLE -> SystemPromptDto.LOGIC_SIMPLE
        }
    }

    private fun systemPromptDtoToSystemPrompt(systemPromptDto: SystemPromptDto): SystemPrompt {
        return when (systemPromptDto) {
            SystemPromptDto.DEFAULT -> SystemPrompt.DEFAULT
            SystemPromptDto.SPECIFYING_QUESTIONS -> SystemPrompt.SPECIFYING_QUESTIONS
            SystemPromptDto.LOGIC_BY_STEP -> SystemPrompt.LOGIC_BY_STEP
            SystemPromptDto.LOGIC_AGENT_GROUP -> SystemPrompt.LOGIC_AGENT_GROUP
            SystemPromptDto.LOGIC_SIMPLE -> SystemPrompt.LOGIC_SIMPLE
        }
    }
}

