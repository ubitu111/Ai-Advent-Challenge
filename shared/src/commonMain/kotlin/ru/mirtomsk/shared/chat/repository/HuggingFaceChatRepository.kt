package ru.mirtomsk.shared.chat.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.chat.context.ContextResetProvider
import ru.mirtomsk.shared.chat.repository.mapper.HuggingFaceResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.network.agent.AgentTypeDto
import ru.mirtomsk.shared.network.agent.AgentTypeProvider
import ru.mirtomsk.shared.network.huggingface.HuggingFaceApiService
import ru.mirtomsk.shared.network.temperature.TemperatureProvider

/**
 * Repository implementation for HuggingFace models
 */
class HuggingFaceChatRepository(
    private val huggingFaceApiService: HuggingFaceApiService,
    private val responseMapper: HuggingFaceResponseMapper,
    private val ioDispatcher: CoroutineDispatcher,
    private val agentTypeProvider: AgentTypeProvider,
    private val contextResetProvider: ContextResetProvider,
    private val temperatureProvider: TemperatureProvider,
) : ChatRepository {

    // Кеш истории общения в оперативной памяти
    private val conversationCache = mutableListOf<ConversationMessage>()
    private val cacheMutex = Mutex()
    private var lastModel: AgentTypeDto? = null
    private var lastResetCounter: Long = 0L

    override suspend fun sendMessage(text: String): AiMessage? {
        return withContext(ioDispatcher) {
            val agentType = agentTypeProvider.agentType.first()
            val temperature = temperatureProvider.temperature.first()
            val resetCounter = contextResetProvider.resetCounter.first()

            // Проверяем, что выбрана HuggingFace модель
            if (!agentType.isHuggingFace) {
                throw IllegalStateException("HuggingFaceChatRepository can only work with HuggingFace models")
            }

            cacheMutex.withLock {
                // Check if context was reset
                if (resetCounter != lastResetCounter) {
                    conversationCache.clear()
                    lastModel = null
                    lastResetCounter = resetCounter
                }

                // Check if model changed
                if (lastModel != null && lastModel != agentType) {
                    conversationCache.clear()
                }

                lastModel = agentType

                // Добавляем текущее сообщение пользователя в кеш
                conversationCache.add(
                    ConversationMessage(
                        role = MessageRoleDto.USER,
                        text = text,
                    )
                )

                // Формируем промпт из истории разговора
                val prompt = buildPrompt(conversationCache)

                // Формируем параметры запроса
                val parameters = ru.mirtomsk.shared.network.huggingface.HuggingFaceParameters(
                    max_new_tokens = 200,
                    temperature = temperature.toDouble(),
                    top_p = 0.9,
                    return_full_text = false,
                )

                // Отправляем запрос и получаем сырой ответ
                val rawResponse = huggingFaceApiService.requestModel(
                    model = agentType,
                    prompt = prompt,
                    parameters = parameters,
                )

                // Парсим ответ через маппер
                val responseText = responseMapper.mapResponseBody(rawResponse)

                // Извлекаем сгенерированный текст (убираем исходный промпт, если return_full_text=false)
                val generatedText = extractGeneratedText(responseText, prompt)

                // Добавляем сообщение ассистента в кеш
                conversationCache.add(
                    ConversationMessage(
                        role = MessageRoleDto.ASSISTANT,
                        text = generatedText,
                    )
                )

                // Возвращаем сообщение в формате AiMessage
                AiMessage(
                    role = MessageRoleDto.ASSISTANT,
                    text = AiMessage.MessageContent.Text(generatedText),
                )
            }
        }
    }

    /**
     * Формирует промпт из истории разговора
     * Для разных моделей может потребоваться разный формат
     */
    private fun buildPrompt(messages: List<ConversationMessage>): String {
        return messages.joinToString("\n") { message ->
            when (message.role) {
                MessageRoleDto.USER -> "User: ${message.text}"
                MessageRoleDto.ASSISTANT -> "Assistant: ${message.text}"
                MessageRoleDto.SYSTEM -> "System: ${message.text}"
            }
        } + "\nAssistant:"
    }

    /**
     * Извлекает сгенерированный текст из ответа
     * HuggingFace может вернуть полный текст с промптом или только новую часть
     */
    private fun extractGeneratedText(responseText: String, prompt: String): String {
        // Если ответ начинается с промпта, удаляем его
        return if (responseText.startsWith(prompt)) {
            responseText.removePrefix(prompt).trim()
        } else {
            responseText.trim()
        }
    }

    /**
     * Внутренняя модель сообщения для кеша
     */
    private data class ConversationMessage(
        val role: MessageRoleDto,
        val text: String,
    )
}

