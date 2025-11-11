package ru.mirtomsk.shared.chat.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.chat.context.ContextResetProvider
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.agent.AgentTypeDto
import ru.mirtomsk.shared.network.agent.AgentTypeProvider
import ru.mirtomsk.shared.network.format.ResponseFormat
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.prompt.SystemPromptDto
import ru.mirtomsk.shared.network.prompt.SystemPromptProvider
import ru.mirtomsk.shared.network.temperature.TemperatureProvider

/**
 * Implementation of ChatRepository using ChatApiService
 */
class ChatRepositoryImpl(
    private val chatApiService: ChatApiService,
    private val apiConfig: ApiConfig,
    private val ioDispatcher: CoroutineDispatcher,
    private val responseMapper: AiResponseMapper,
    private val formatProvider: ResponseFormatProvider,
    private val agentTypeProvider: AgentTypeProvider,
    private val systemPromptProvider: SystemPromptProvider,
    private val contextResetProvider: ContextResetProvider,
    private val temperatureProvider: TemperatureProvider,
) : ChatRepository {

    // Кеш истории общения в оперативной памяти
    private val conversationCache = mutableListOf<AiRequest.Message>()
    private val cacheMutex = Mutex()
    private var lastAgentType: AgentTypeDto? = null
    private var lastSystemPrompt: SystemPromptDto? = null
    private var lastResetCounter: Long = 0L

    override suspend fun sendMessage(
        text: String
    ): AiMessage? {
        return withContext(ioDispatcher) {
            // Get current format, agent type, system prompt, temperature and reset counter from providers
            val format = formatProvider.responseFormat.first()
            val agentType = agentTypeProvider.agentType.first()
            val systemPrompt = systemPromptProvider.systemPrompt.first()
            val temperature = temperatureProvider.temperature.first()
            val resetCounter = contextResetProvider.resetCounter.first()

            cacheMutex.withLock {
                // Check if context was reset
                if (resetCounter != lastResetCounter) {
                    conversationCache.clear()
                    lastAgentType = null
                    lastSystemPrompt = null
                    lastResetCounter = resetCounter
                }

                if (lastAgentType != null && lastAgentType != agentType) {
                    conversationCache.clear()
                }

                if (lastSystemPrompt != null && lastSystemPrompt != systemPrompt) {
                    conversationCache.clear()
                }

                if (conversationCache.isEmpty()) {
                    conversationCache.add(
                        AiRequest.Message(
                            role = MessageRoleDto.SYSTEM,
                            text = selectPrompt(systemPrompt, format),
                        )
                    )
                }
                lastAgentType = agentType
                lastSystemPrompt = systemPrompt

                // Добавляем текущее сообщение пользователя в кеш
                conversationCache.add(
                    AiRequest.Message(
                        role = MessageRoleDto.USER,
                        text = text,
                    )
                )

                // Формируем запрос: системное сообщение + все сообщения пользователя + только последнее сообщение ассистента
                val request = AiRequest(
                    modelUri = "gpt://${apiConfig.keyId}/${getModel(agentType)}",
                    completionOptions = AiRequest.CompletionOptions(
                        stream = true,
                        temperature = temperature,
                        maxTokens = 2000,
                    ),
                    messages = conversationCache,
                )
                val responseBody = chatApiService.requestModel(request)
                val response = responseMapper.mapResponseBody(responseBody, format)

                // Добавляем сообщение ассистента из response в кеш
                val assistantMessage = response.result.alternatives
                    .find { it.message.role == MessageRoleDto.ASSISTANT }
                    ?.message
                    ?: return@withLock null

                // Преобразуем AiMessage в AiRequest.Message
                val messageText = when (val content = assistantMessage.text) {
                    is AiMessage.MessageContent.Text -> content.value
                    is AiMessage.MessageContent.Json -> {
                        // Для JSON формата преобразуем в текстовое представление
                        val jsonResponse = content.value
                        val linksText = if (jsonResponse.resource.isNotEmpty()) {
                            "\nСсылки:\n${jsonResponse.resource.joinToString("\n") { it.link }}"
                        } else ""
                        "${jsonResponse.title}\n${jsonResponse.text}$linksText"
                    }
                }
                conversationCache.add(
                    AiRequest.Message(
                        role = MessageRoleDto.ASSISTANT,
                        text = messageText
                    )
                )

                assistantMessage
            }
        }
    }

    private fun selectPrompt(systemPrompt: SystemPromptDto, format: ResponseFormat): String {
        val basePrompt = when (systemPrompt) {
            SystemPromptDto.EMPTY -> Prompts.EMPTY
            SystemPromptDto.SPECIFYING_QUESTIONS -> Prompts.SPECIFYING_QUESTIONS
            SystemPromptDto.LOGIC_BY_STEP -> Prompts.LOGIC_BY_STEP
            SystemPromptDto.LOGIC_AGENT_GROUP -> Prompts.LOGIC_GROUP
            SystemPromptDto.LOGIC_SIMPLE -> Prompts.LOGIC_SIMPLE
        }
        val formatPrompt = when (format) {
            ResponseFormat.DEFAULT -> Prompts.DEFAULT_FORMAT_RESPONSE
            ResponseFormat.JSON -> Prompts.JSON_FORMAT_RESPONSE
        }
        return "$basePrompt\n\n$formatPrompt"
    }

    private fun getModel(agentType: AgentTypeDto): String {
        return when (agentType) {
            AgentTypeDto.LITE -> MODEL_LITE
            AgentTypeDto.PRO -> MODEL_PRO
        }
    }

    private companion object {

        const val MODEL_LITE = "yandexgpt-lite"
        const val MODEL_PRO = "yandexgpt"
    }
}

