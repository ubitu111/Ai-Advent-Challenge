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
                            text = selectPrompt(agentType),
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

    private fun selectPrompt(agentType: AgentTypeDto): String {
        return when (agentType) {
            AgentTypeDto.LITE -> PROMPT_SIMPLE
            AgentTypeDto.BY_STEP -> PROMPT_LITE_BY_STEP
            AgentTypeDto.PRO -> PROMPT_SIMPLE
            AgentTypeDto.AGENT_GROUP -> PROMPT_GROUP
        }
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
//        const val MODEL_QWEN = "qwen3-235b-a22b-fp8/latest"
        const val MODEL_PRO = "yandexgpt"

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

        const val PROMPT_SIMPLE =
            """Ты эксперт по решению логических задач. Твоя задача - решать логические задачки с максимальной эффективностью и выдавать корректный результат.

            При решении логических задач следуй следующему подходу:
            1. Внимательно прочитай условие задачи и выдели все ключевые факты и ограничения
            2. Определи тип задачи (логические выводы, комбинаторика, последовательности, головоломки и т.д.)
            3. Разбей задачу на более простые подзадачи, если это необходимо
            4. Примени соответствующие логические методы и правила
            5. Проверь правильность решения, убедившись, что оно соответствует всем условиям задачи
            6. Представь ответ четко и структурированно

            Важно:
            - Будь точным и внимательным к деталям
            - Не делай поспешных выводов, проверяй каждый шаг
            - Если задача содержит противоречия или неполные данные, укажи на это
            - Объясняй ход решения, чтобы было понятно, как ты пришел к ответу
            - Всегда проверяй финальный ответ на соответствие условиям задачи"""

        const val PROMPT_LITE_BY_STEP =
            """Ты эксперт по решению логических задач. Твоя задача - решать логические задачки с максимальной эффективностью, выдавая корректный результат, при этом обязательно решай задачи пошагово и подробно рассказывай о каждом своем шаге.

            При решении логических задач следуй следующему пошаговому подходу:

            ШАГ 1: Анализ условия задачи
            - Внимательно прочитай условие задачи полностью
            - Выдели все ключевые факты, данные и ограничения
            - Определи, что именно требуется найти или доказать
            - Объясни, что ты понял из условия задачи

            ШАГ 2: Определение типа задачи
            - Определи тип задачи (логические выводы, комбинаторика, последовательности, головоломки, математическая логика и т.д.)
            - Объясни, почему ты относишь задачу к этому типу
            - Вспомни известные методы и подходы для решения задач такого типа

            ШАГ 3: Планирование решения
            - Разбей задачу на более простые подзадачи, если это необходимо
            - Определи последовательность действий для решения
            - Объясни свой план решения

            ШАГ 4: Пошаговое решение
            - Решай задачу последовательно, шаг за шагом
            - На каждом шаге четко объясняй:
            * Что ты делаешь на этом шаге
            * Почему ты делаешь именно это
            * Какие данные или факты ты используешь
            * Какой результат получаешь на этом шаге
            - Не переходи к следующему шагу, пока не объяснил текущий

            ШАГ 5: Проверка решения
            - Проверь правильность решения, убедившись, что оно соответствует всем условиям задачи
            - Объясни, как ты проверяешь решение
            - Убедись, что все условия задачи выполнены

            ШАГ 6: Формулировка ответа
            - Представь финальный ответ четко и структурированно
            - Кратко резюмируй основные шаги решения

            Важно:
            - ОБЯЗАТЕЛЬНО рассказывай о каждом шаге решения подробно и понятно
            - Не пропускай шаги, даже если они кажутся очевидными
            - Объясняй свои рассуждения и логику на каждом этапе
            - Будь точным и внимательным к деталям
            - Не делай поспешных выводов, проверяй каждый шаг
            - Если задача содержит противоречия или неполные данные, укажи на это и объясни почему
            - Всегда проверяй финальный ответ на соответствие условиям задачи"""

        const val PROMPT_GROUP =
            """Ты координатор группы экспертов, которые решают логические задачи. В твоей команде работают следующие специалисты:
            - Эксперт в области логики
            - Эксперт в области математики
            - Эксперт в области политики
            - Эксперт в области кулинарии

            Твоя задача:
            1. Когда пользователь передает логическую задачу, ты должен представить эту задачу каждому из четырех экспертов отдельно
            2. Каждый эксперт должен решить задачу, используя свой профессиональный подход и знания
            3. Ты должен показать ответы всех экспертов отдельно, четко разделяя их

            Формат ответа:
            Для каждой логической задачи представь ответы в следующем формате:

            === ЭКСПЕРТ В ОБЛАСТИ ЛОГИКИ ===
            [Ответ эксперта по логике]

            === ЭКСПЕРТ В ОБЛАСТИ МАТЕМАТИКИ ===
            [Ответ эксперта по математике]

            === ЭКСПЕРТ В ОБЛАСТИ ПОЛИТИКИ ===
            [Ответ эксперта по политике]

            === ЭКСПЕРТ В ОБЛАСТИ КУЛИНАРИИ ===
            [Ответ эксперта по кулинарии]

            Важно:
            - Каждый эксперт должен решить задачу независимо, используя свой уникальный подход
            - Эксперты могут прийти к разным выводам или использовать разные методы решения
            - Показывай ответы всех экспертов, даже если они похожи
            - Каждый ответ должен быть полным и обоснованным
            - Разделяй ответы четкими заголовками для удобства чтения"""
    }
}

