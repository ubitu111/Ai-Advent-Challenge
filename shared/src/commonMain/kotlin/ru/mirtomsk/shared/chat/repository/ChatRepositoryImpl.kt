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
    private val conversationCache = mutableListOf(
        AiRequest.Message(
            role = MessageRoleDto.SYSTEM,
            text = DEFAULT_FORMAT_SYSTEM_MESSAGE,
        )
    )
    private val cacheMutex = Mutex()

    override suspend fun sendMessage(text: String, format: ResponseFormat): AiMessage? {
        return withContext(ioDispatcher) {
            cacheMutex.withLock {
                // Добавляем текущее сообщение пользователя в кеш
                conversationCache.add(
                    AiRequest.Message(
                        role = MessageRoleDto.USER,
                        text = text,
                    )
                )

                // Формируем запрос: системное сообщение + все сообщения пользователя + только последнее сообщение ассистента
                val request = AiRequest(
                    modelUri = "gpt://${apiConfig.keyId}/yandexgpt-lite",
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

    private companion object {

        const val JSON_FORMAT_SYSTEM_MESSAGE = """
                        Ты виртуальный помощник. Отвечай в формате JSON.
                        Ответ должен быть валидным JSON объектом со следующей структурой:
                        {
                            "title": "краткий заголовок ответа",
                            "text": "развернутый ответ на вопрос",
                            "resource": "массив JSON объектов, в котором поле "link" с типом строка со ссылками, связанных с вопросом. Если ссылок нет, верни массив с одним объектом, внутри которого вместо ссылки слово 'отсутствуют'"
                        }
                        Всегда возвращай валидный JSON, даже если ответ короткий.
                        """

        const val DEFAULT_FORMAT_SYSTEM_MESSAGE =
            """Ты виртуальный помощник, специалист во многих областях знаний. 
            При получении вопроса от пользователя, если вопрос недостаточно ясен, неполон или требует дополнительной информации для точного ответа, обязательно задай уточняющие вопросы. 
            Уточняющие вопросы должны помочь тебе лучше понять намерения пользователя, контекст вопроса и получить необходимые детали для более точного и полезного ответа. 
            Задавай вопросы вежливо и по делу, не более 1-2 уточняющих вопросов за раз."""
    }
}

