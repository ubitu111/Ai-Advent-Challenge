package ru.mirtomsk.shared.chat.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.chat.context.ContextResetProvider
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.mapper.HuggingFaceResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.HuggingFaceParameters
import ru.mirtomsk.shared.network.agent.AgentTypeDto
import ru.mirtomsk.shared.network.agent.AgentTypeProvider
import ru.mirtomsk.shared.network.format.ResponseFormat
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.prompt.SystemPromptDto
import ru.mirtomsk.shared.network.prompt.SystemPromptProvider
import ru.mirtomsk.shared.network.temperature.TemperatureProvider

/**
 * Unified implementation of ChatRepository for all AI models (Yandex GPT and HuggingFace)
 * Provides a single point of access for all model requests
 */
class ChatRepositoryImpl(
    private val chatApiService: ChatApiService,
    private val apiConfig: ApiConfig,
    private val ioDispatcher: CoroutineDispatcher,
    private val yandexResponseMapper: AiResponseMapper,
    private val huggingFaceResponseMapper: HuggingFaceResponseMapper,
    private val formatProvider: ResponseFormatProvider,
    private val agentTypeProvider: AgentTypeProvider,
    private val systemPromptProvider: SystemPromptProvider,
    private val contextResetProvider: ContextResetProvider,
    private val temperatureProvider: TemperatureProvider,
) : ChatRepository {

    // Единый кеш истории общения для всех моделей
    private val conversationCache = mutableListOf<AiRequest.Message>()
    
    private val cacheMutex = Mutex()
    private var lastAgentType: AgentTypeDto? = null
    private var lastSystemPrompt: SystemPromptDto? = null
    private var lastResetCounter: Long = 0L

    override suspend fun sendMessage(text: String): AiMessage? {
        return withContext(ioDispatcher) {
            val agentType = agentTypeProvider.agentType.first()
            
            return@withContext if (agentType.isYandexGpt) {
                sendMessageYandexGpt(text)
            } else {
                sendMessageHuggingFace(text)
            }
        }
    }

    /**
     * Отправка сообщения для Yandex GPT моделей
     */
    private suspend fun sendMessageYandexGpt(text: String): AiMessage? {
        val format = formatProvider.responseFormat.first()
        val agentType = agentTypeProvider.agentType.first()
        val systemPrompt = systemPromptProvider.systemPrompt.first()
        val temperature = temperatureProvider.temperature.first()
        val resetCounter = contextResetProvider.resetCounter.first()

        return cacheMutex.withLock {
            // Управление кешем
            manageCache(
                agentType = agentType,
                systemPrompt = systemPrompt,
                resetCounter = resetCounter
            )

            // Добавляем системное сообщение, если кеш пуст
            if (conversationCache.isEmpty()) {
                conversationCache.add(
                    AiRequest.Message(
                        role = MessageRoleDto.SYSTEM,
                        text = selectPrompt(systemPrompt, format),
                    )
                )
            }

            // Добавляем текущее сообщение пользователя в кеш
            addUserMessage(text)

            // Формируем и отправляем запрос
            val request = AiRequest(
                modelUri = "gpt://${apiConfig.keyId}/${getYandexModel(agentType)}",
                completionOptions = AiRequest.CompletionOptions(
                    stream = true,
                    temperature = temperature,
                    maxTokens = 2000,
                ),
                messages = conversationCache,
            )
            val responseBody = chatApiService.requestYandexGpt(request)
            val response = yandexResponseMapper.mapResponseBody(responseBody, format)

            // Обрабатываем ответ и добавляем в кеш
            processYandexResponse(response, format)
        }
    }

    /**
     * Отправка сообщения для HuggingFace моделей
     */
    private suspend fun sendMessageHuggingFace(text: String): AiMessage? {
        val agentType = agentTypeProvider.agentType.first()
        val temperature = temperatureProvider.temperature.first()
        val resetCounter = contextResetProvider.resetCounter.first()

        return cacheMutex.withLock {
            // Управление кешем
            manageCache(
                agentType = agentType,
                systemPrompt = null,
                resetCounter = resetCounter
            )

            // Добавляем текущее сообщение пользователя в кеш
            addUserMessage(text)

            // Формируем промпт из истории разговора
            val prompt = buildHuggingFacePrompt(conversationCache)

            // Формируем параметры запроса
            val parameters = HuggingFaceParameters(
                max_new_tokens = 200,
                temperature = temperature.toDouble(),
                top_p = 0.9,
                return_full_text = false,
            )

            // Отправляем запрос и получаем сырой ответ
            val rawResponse = chatApiService.requestHuggingFace(
                model = agentType,
                prompt = prompt,
                parameters = parameters,
            )

            // Парсим ответ через маппер
            val responseText = huggingFaceResponseMapper.mapResponseBody(rawResponse)

            // Извлекаем сгенерированный текст
            val generatedText = extractGeneratedText(responseText, prompt)

            // Добавляем сообщение ассистента в кеш
            addAssistantMessage(generatedText)

            // Возвращаем сообщение в формате AiMessage
            AiMessage(
                role = MessageRoleDto.ASSISTANT,
                text = AiMessage.MessageContent.Text(generatedText),
            )
        }
    }

    /**
     * Управление кешем разговора (общий метод для обоих типов моделей)
     */
    private fun manageCache(
        agentType: AgentTypeDto,
        systemPrompt: SystemPromptDto?,
        resetCounter: Long
    ) {
        // Check if context was reset
        if (resetCounter != lastResetCounter) {
            clearCache()
            lastAgentType = null
            lastSystemPrompt = null
            lastResetCounter = resetCounter
        }

        // Check if model changed
        if (lastAgentType != null && lastAgentType != agentType) {
            clearCache()
        }

        // Check if system prompt changed (только для Yandex GPT)
        if (systemPrompt != null && lastSystemPrompt != null && lastSystemPrompt != systemPrompt) {
            clearCache()
        }

        lastAgentType = agentType
        lastSystemPrompt = systemPrompt
    }

    /**
     * Очистка кеша
     */
    private fun clearCache() {
        conversationCache.clear()
    }

    /**
     * Добавление сообщения пользователя в кеш
     */
    private fun addUserMessage(text: String) {
        conversationCache.add(
            AiRequest.Message(
                role = MessageRoleDto.USER,
                text = text,
            )
        )
    }

    /**
     * Добавление сообщения ассистента в кеш
     */
    private fun addAssistantMessage(text: String) {
        conversationCache.add(
            AiRequest.Message(
                role = MessageRoleDto.ASSISTANT,
                text = text,
            )
        )
    }

    /**
     * Обработка ответа Yandex GPT и добавление в кеш
     */
    private fun processYandexResponse(
        response: ru.mirtomsk.shared.chat.repository.model.AiResponse,
        format: ResponseFormat
    ): AiMessage? {
        val assistantMessage = response.result.alternatives
            .find { it.message.role == MessageRoleDto.ASSISTANT }
            ?.message
            ?: return null

        // Преобразуем AiMessage в AiRequest.Message для кеша
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

        return assistantMessage
    }

    private fun selectPrompt(systemPrompt: SystemPromptDto, format: ResponseFormat): String {
        val basePrompt = when (systemPrompt) {
            SystemPromptDto.DEFAULT -> Prompts.DEFAULT
            SystemPromptDto.SPECIFYING_QUESTIONS -> Prompts.SPECIFYING_QUESTIONS
            SystemPromptDto.LOGIC_BY_STEP -> Prompts.LOGIC_BY_STEP
            SystemPromptDto.LOGIC_AGENT_GROUP -> Prompts.LOGIC_GROUP
            SystemPromptDto.LOGIC_SIMPLE -> Prompts.LOGIC_SIMPLE
        }
        val formatPrompt = when (format) {
            ResponseFormat.DEFAULT -> Prompts.DEFAULT_FORMAT_RESPONSE
            ResponseFormat.JSON -> Prompts.JSON_FORMAT_RESPONSE
        }
        return "$basePrompt$formatPrompt"
    }

    /**
     * Получение имени модели Yandex GPT
     */
    private fun getYandexModel(agentType: AgentTypeDto): String {
        return when (agentType) {
            AgentTypeDto.LITE -> MODEL_LITE
            AgentTypeDto.PRO -> MODEL_PRO
            else -> throw IllegalArgumentException("Model ${agentType.name} is not a Yandex GPT model")
        }
    }

    /**
     * Формирует промпт из истории разговора для HuggingFace
     */
    private fun buildHuggingFacePrompt(messages: List<AiRequest.Message>): String {
        return messages.joinToString("\n") { message ->
            when (message.role) {
                MessageRoleDto.USER -> "User: ${message.text}"
                MessageRoleDto.ASSISTANT -> "Assistant: ${message.text}"
                MessageRoleDto.SYSTEM -> "System: ${message.text}"
            }
        } + "\nAssistant:"
    }

    /**
     * Извлекает сгенерированный текст из ответа HuggingFace
     */
    private fun extractGeneratedText(responseText: String, prompt: String): String {
        // Если ответ начинается с промпта, удаляем его
        return if (responseText.startsWith(prompt)) {
            responseText.removePrefix(prompt).trim()
        } else {
            responseText.trim()
        }
    }

    private companion object {
        const val MODEL_LITE = "yandexgpt-lite"
        const val MODEL_PRO = "yandexgpt"
    }
}

