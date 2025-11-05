package ru.mirtomsk.shared.chat.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.AiResponse
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
) : ChatRepository {

    override suspend fun sendMessage(text: String, format: ResponseFormat): AiResponse {
        return withContext(ioDispatcher) {
            val request = createRequestBody(text, format)
            chatApiService.requestModel(request)
        }
    }

    private fun createRequestBody(message: String, format: ResponseFormat): AiRequest {
        val systemMessage = when (format) {
            ResponseFormat.JSON -> """
            Ты виртуальный помощник наподобие Алисы от Яндекса. Отвечай в формате JSON.
            Ответ должен быть валидным JSON объектом со следующей структурой:
            {
                "title": "краткий заголовок ответа",
                "text": "развернутый ответ на вопрос",
                "resource": "массив JSON объектов, в котором поле "link" с типом строка со ссылками, связанных с вопросом. Если ссылок нет, верни массив с одним объектом, внутри которого вместо ссылки слово 'отсутствуют'"
            }
            Всегда возвращай валидный JSON, даже если ответ короткий.
            """

            ResponseFormat.DEFAULT -> "Ты виртуальный помощник наподобие Алисы от Яндекса"
        }

        return AiRequest(
            modelUri = "gpt://${apiConfig.keyId}/yandexgpt-lite",
            completionOptions = AiRequest.CompletionOptions(
                stream = true,
                temperature = 0.6f,
                maxTokens = 2000,
            ),
            messages = listOf(
                AiRequest.Message(
                    role = "system",
                    text = systemMessage,
                ),
                AiRequest.Message(
                    role = "user",
                    text = message,
                )
            )
        )
    }
}

