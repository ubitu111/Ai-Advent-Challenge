package ru.mirtomsk.shared.chat.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.agent.AgentTypeDto
import ru.mirtomsk.shared.network.format.ResponseFormat

/**
 * Implementation of ChatRepository using ChatApiService
 */
class ChatRepositoryImpl(
    private val chatApiService: ChatApiService,
    private val apiConfig: ApiConfig,
    private val ioDispatcher: CoroutineDispatcher,
    private val responseMapper: AiResponseMapper,
) : ChatRepository {

    // Кеш истории общения в оперативной памяти
    private val conversationCache = mutableListOf<AiRequest.Message>()
    private val cacheMutex = Mutex()
    private var lastAgentType: AgentTypeDto? = null

    override suspend fun sendMessage(
        text: String,
        format: ResponseFormat,
        agentType: AgentTypeDto
    ): AiMessage? {
        return withContext(ioDispatcher) {
            cacheMutex.withLock {
                if (lastAgentType != null && lastAgentType != agentType) {
                    conversationCache.clear()
                }

                if (conversationCache.isEmpty()) {
                    conversationCache.add(
                        AiRequest.Message(
                            role = MessageRoleDto.SYSTEM,
                            text = selectPrompt(agentType, format),
                        )
                    )
                }
                lastAgentType = agentType

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
                        temperature = 0.6f,
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

    private fun selectPrompt(agentType: AgentTypeDto, format: ResponseFormat): String {
        val basePrompt = when (agentType) {
            AgentTypeDto.LITE -> Prompts.LOGIC_SIMPLE
            AgentTypeDto.BY_STEP -> Prompts.LOGIC_BY_STEP
            AgentTypeDto.PRO -> Prompts.LOGIC_SIMPLE
            AgentTypeDto.AGENT_GROUP -> Prompts.LOGIC_GROUP
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
            AgentTypeDto.BY_STEP -> MODEL_LITE
            AgentTypeDto.PRO -> MODEL_PRO
            AgentTypeDto.AGENT_GROUP -> MODEL_PRO
        }
    }

    private companion object {

        const val MODEL_LITE = "yandexgpt-lite"
        const val MODEL_PRO = "yandexgpt"
    }
}

