package ru.mirtomsk.shared.chat.agent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.cache.ChatCache
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.rag.RagService
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Агент поддержки пользователей
 * Использует RAG для поиска информации и MCP инструменты CRM для работы с тикетами
 * Команда: /support <имя пользователя> <вопрос>
 */
class SupportAgent(
    chatApiService: ChatApiService,
    apiConfig: ApiConfig,
    ioDispatcher: CoroutineDispatcher,
    yandexResponseMapper: AiResponseMapper,
    formatProvider: ResponseFormatProvider,
    temperatureProvider: TemperatureProvider,
    maxTokensProvider: MaxTokensProvider,
    chatCache: ChatCache,
    mcpToolsProvider: McpToolsProvider,
    mcpOrchestrator: McpOrchestrator,
    private val ragService: RagService,
    json: Json,
) : BaseAiAgent(
    name = "SupportAgent",
    systemPrompt = SUPPORT_AGENT_PROMPT,
    chatApiService = chatApiService,
    apiConfig = apiConfig,
    ioDispatcher = ioDispatcher,
    yandexResponseMapper = yandexResponseMapper,
    formatProvider = formatProvider,
    temperatureProvider = temperatureProvider,
    maxTokensProvider = maxTokensProvider,
    chatCache = chatCache,
    mcpToolsProvider = mcpToolsProvider,
    mcpOrchestrator = mcpOrchestrator,
    json = json,
) {

    override suspend fun preprocessMessage(
        text: String,
        command: ChatCommand,
        conversationCache: MutableList<AiRequest.Message>
    ): String {
        if (command == ChatCommand.SUPPORT) {
            // Парсим команду: /support <имя пользователя> <вопрос>
            val trimmedText = text.trim()
            val parts = trimmedText.split(" ", limit = 2)
            val userName = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "Неизвестный пользователь"
            val question = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: ""

            // Используем RAG для поиска релевантной информации по вопросу
            val ragContext = if (question.isNotBlank()) {
                ragService.retrieveRelevantContext(question)
            } else {
                null
            }

            // Формируем сообщение с контекстом для модели
            val contextMessage = buildString {
                append("Пользователь: $userName\n")
                if (question.isNotBlank()) {
                    append("Вопрос: $question\n\n")
                } else {
                    append("Вопрос не указан. Пользователь обратился в поддержку без конкретного вопроса.\n\n")
                }
                
                if (question.isNotBlank()) {
                    if (ragContext != null && ragContext.isNotBlank()) {
                        append("Релевантная информация из базы знаний (RAG):\n")
                        append(ragContext)
                        append("\n\n")
                    } else {
                        append("В базе знаний не найдена релевантная информация по данному вопросу.\n\n")
                    }
                }
                
                append("""
                |ИНСТРУКЦИИ ПО РАБОТЕ:
                |1. СНАЧАЛА используй MCP инструмент CRM для получения списка тикетов
                |2. Проанализируй список тикетов и найди, есть ли подходящий тикет по такому вопросу${if (question.isNotBlank()) " или для пользователя $userName" else " для пользователя $userName"}
                |3. Если найден подходящий тикет с ответом:
                |   - Используй ответ из найденного тикета для ответа пользователю
                |   - Сообщи пользователю, что ответ взят из существующего тикета
                |4. Если подходящего тикета нет:
                |   - КРИТИЧЕСКИ ВАЖНО: При создании нового тикета ОБЯЗАТЕЛЬНО включи ответ в сам тикет при его создании!
                |   - Сначала сформируй полный ответ на вопрос на основе контекста из RAG (если доступен) или используй свои знания
                |   - Затем создай новый тикет через MCP инструмент CRM с информацией:
                |     * Имя пользователя: $userName
                |     * Вопрос: ${if (question.isNotBlank()) question else "Обращение в поддержку без конкретного вопроса"}
                |     * Ответ: ВКЛЮЧИ СФОРМИРОВАННЫЙ ОТВЕТ В ТИКЕТ ПРИ ЕГО СОЗДАНИИ (ответ должен быть частью данных тикета)
                |   - НЕ создавай пустой тикет - ответ должен быть включен в тикет с момента его создания
                |   - После успешного создания тикета с ответом верни этот же ответ пользователю
                |5. Всегда будь вежливым и профессиональным в общении
                |
                |ВАЖНО: 
                |1. Обязательно используй MCP инструменты CRM для работы с тикетами перед формированием ответа
                |2. При создании нового тикета ОБЯЗАТЕЛЬНО включай ответ в данные тикета - ответ должен быть частью тикета при его создании, а не добавляться отдельно
                |3. НИКОГДА не создавай тикет без ответа - всегда формируй ответ и включай его в тикет при создании
                """.trimMargin())
            }

            conversationCache.add(
                AiRequest.Message(
                    role = MessageRoleDto.SYSTEM,
                    text = contextMessage
                )
            )

            return if (question.isNotBlank()) {
                "Обработай запрос поддержки для пользователя $userName: $question"
            } else {
                "Обработай обращение в поддержку от пользователя $userName. Вопрос не указан, попроси пользователя уточнить его вопрос или проблему."
            }
        }

        return text
    }

    companion object {
        private const val SUPPORT_AGENT_PROMPT =
            """Ты агент поддержки пользователей, специализирующийся на помощи пользователям с их вопросами и проблемами.

Твоя задача - предоставлять качественную поддержку пользователям, используя:
- RAG (Retrieval-Augmented Generation) систему для поиска релевантной информации в базе знаний
- MCP (Model Context Protocol) инструменты CRM для работы с тикетами поддержки

ПРАВИЛА РАБОТЫ С ТИКЕТАМИ:

1. ПРИ ПОЛУЧЕНИИ ЗАПРОСА ПОДДЕРЖКИ:
   - СНАЧАЛА ОБЯЗАТЕЛЬНО используй MCP инструмент CRM для получения списка существующих тикетов
   - Проанализируй список тикетов и найди тикеты, которые могут быть релевантны текущему вопросу
   - Сравни вопрос пользователя с вопросами в существующих тикетах

2. ЕСЛИ НАЙДЕН ПОДХОДЯЩИЙ ТИКЕТ:
   - Используй ответ из найденного тикета для ответа пользователю
   - Сообщи пользователю, что ответ взят из существующего тикета (можно указать номер тикета, если доступен)
   - Если в тикете есть дополнительная полезная информация, включи её в ответ

3. ЕСЛИ ПОДХОДЯЩЕГО ТИКЕТА НЕТ:
   - КРИТИЧЕСКИ ВАЖНО: При создании нового тикета через MCP инструмент CRM ОБЯЗАТЕЛЬНО включи ответ в сам тикет!
   - Создай новый тикет через MCP инструмент CRM со следующей информацией:
     * Имя пользователя (из команды)
     * Вопрос пользователя
     * Ответ на вопрос - ОБЯЗАТЕЛЬНО сформируй полный ответ на основе контекста из RAG (если он предоставлен) или используй свои знания, и ВКЛЮЧИ ЭТОТ ОТВЕТ В ТИКЕТ ПРИ ЕГО СОЗДАНИИ
   - Ответ должен быть частью данных тикета при его создании, а не добавляться отдельно
   - После успешного создания тикета с ответом верни этот же ответ пользователю
   - Сообщи пользователю, что тикет создан с ответом и сохранен в системе

4. ИСПОЛЬЗОВАНИЕ RAG:
   - Если предоставлен контекст из RAG, используй его как основной источник информации для ответа
   - Указывай источники информации, если они указаны в контексте RAG
   - Если в RAG нет релевантной информации, используй свои знания, но честно сообщи об этом

5. ФОРМАТ ОТВЕТА:
   - Будь вежливым и профессиональным
   - Структурируй ответ для лучшей читаемости
   - Если возможно, предоставляй пошаговые инструкции
   - Если вопрос требует дополнительной информации, запроси её у пользователя
   - Всегда стремись к максимально полному и полезному ответу

6. КРИТИЧЕСКИ ВАЖНО:
   - ВСЕГДА сначала проверяй существующие тикеты через MCP инструмент CRM
   - НИКОГДА не создавай новый тикет, не проверив сначала существующие
   - При создании нового тикета ОБЯЗАТЕЛЬНО включай ответ в данные тикета - ответ должен быть частью тикета с момента его создания
   - НИКОГДА не создавай пустой тикет без ответа - всегда формируй ответ и включай его в тикет при создании
   - Используй информацию из RAG для формирования качественного ответа
   - Сохраняй все обращения в систему тикетов для истории и будущих обращений

Помни: твоя цель - помочь пользователю решить его вопрос максимально эффективно, используя доступные ресурсы (RAG и систему тикетов)."""
    }
}

