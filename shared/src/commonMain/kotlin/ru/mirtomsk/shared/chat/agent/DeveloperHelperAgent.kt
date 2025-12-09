package ru.mirtomsk.shared.chat.agent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.cache.ChatCache
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.mapper.OpenAiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.LocalChatApiService
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.rag.RagService
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Агент-помощник разработчика
 * Поддерживает команды /help (с RAG) и /git (через MCP)
 */
class DeveloperHelperAgent(
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
    localChatApiService: LocalChatApiService,
    openAiResponseMapper: OpenAiResponseMapper,
) : BaseAiAgent(
    name = "DeveloperHelperAgent",
    systemPrompt = DEVELOPER_HELPER_PROMPT,
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
    localChatApiService = localChatApiService,
    openAiResponseMapper = openAiResponseMapper,
) {

    override suspend fun preprocessMessage(
        text: String,
        command: ChatCommand,
        conversationCache: MutableList<AiRequest.Message>
    ): String {
        when (command) {
            ChatCommand.HELP -> {
                // Для /help используем RAG с вопросом пользователя
                val query = text.trim()
                if (query.isNotBlank()) {
                    val ragContext = ragService.retrieveRelevantContext(query)
                    if (ragContext != null && ragContext.isNotBlank()) {
                        conversationCache.add(
                            AiRequest.Message(
                                role = MessageRoleDto.SYSTEM,
                                text = "Релевантная информация из проекта (RAG):\n$ragContext"
                            )
                        )
                    }
                }
                return query
            }

            ChatCommand.GIT -> {
                // Для /git просто возвращаем команду без изменений
                // MCP инструменты будут использованы автоматически через базовый класс
                return text.trim()
            }

            else -> return text
        }
    }

    companion object {
        private const val DEVELOPER_HELPER_PROMPT =
            """Ты помощник разработчика, который помогает в работе с проектом.

Ты имеешь доступ к:
- RAG (Retrieval-Augmented Generation) системе для получения информации о проекте, коде, документации
- MCP (Model Context Protocol) инструментам для выполнения Git команд и получения информации о проекте

Твои возможности:

1. Команда /help <вопрос>:
   - Используй RAG для поиска релевантной информации в проекте по заданному вопросу
   - Помогай разработчику находить нужную информацию в коде, документации, конфигурации
   - Отвечай на вопросы о структуре проекта, использовании API, паттернах и т.д.

2. Команда /git <команда>:
   - Выполняй Git команды через MCP инструменты
   - Поддерживай команды типа: статус, ветка, branch, status и т.д.
   - Помогай разработчику работать с Git репозиторием
   - Если команда не распознана, попробуй найти похожую или предложи альтернативу

Правила работы:
- Для /help ВСЕГДА используй RAG для получения контекста проекта перед ответом
- Для /git используй доступные MCP инструменты для выполнения Git команд
- Будь полезным и конкретным в ответах
- Если информация не найдена, честно об этом сообщи
- Предлагай альтернативные способы получения информации, если прямой ответ недоступен

Формат ответа:
- Структурируй информацию для лучшей читаемости
- Приводи примеры, если это уместно
- Будь кратким, но информативным"""
    }
}

