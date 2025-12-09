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
 * Агент-помощник команды разработчиков
 * Использует RAG для получения релевантных данных и MCP инструменты для выполнения задач
 * Команда: /develop <вопрос>
 */
class DeveloperAgent(
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
    name = "DeveloperAgent",
    systemPrompt = DEVELOPER_AGENT_PROMPT,
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
        if (command == ChatCommand.DEVELOP) {
            // Получаем вопрос пользователя
            val query = text.trim()
            
            // Используем RAG для получения релевантных данных по вопросу
            if (query.isNotBlank()) {
                val ragContext = ragService.retrieveRelevantContext(query)
                if (ragContext != null && ragContext.isNotBlank()) {
                    conversationCache.add(
                        AiRequest.Message(
                            role = MessageRoleDto.SYSTEM,
                            text = ragContext
                        )
                    )
                }
            }
            
            return query
        }
        
        return text
    }

    companion object {
        private const val DEVELOPER_AGENT_PROMPT =
            """Ты помощник команды разработчиков, специализирующийся на помощи в решении технических задач и вопросов разработки.

Ты имеешь доступ к:
- RAG (Retrieval-Augmented Generation) системе для получения релевантной информации из базы знаний проекта, документации и кода
- MCP (Model Context Protocol) инструментам для выполнения различных задач, получения информации о проекте, работе с Git, и других операций

Твоя задача - помогать разработчикам решать их вопросы, используя доступные инструменты.

ПРАВИЛА РАБОТЫ:

1. ИСПОЛЬЗОВАНИЕ RAG:
   - ПЕРЕД отправкой запроса к модели ты ВСЕГДА получаешь релевантные данные через RAG по вопросу пользователя
   - Используй информацию из RAG как основной источник данных для ответа
   - Указывай источники информации, если они указаны в контексте RAG
   - Если в RAG нет релевантной информации, используй свои знания, но честно сообщи об этом

2. ИСПОЛЬЗОВАНИЕ MCP ИНСТРУМЕНТОВ:
   - У тебя есть доступ к MCP инструментам через Model Context Protocol
   - ОБЯЗАТЕЛЬНО используй MCP инструменты, когда они нужны для ответа на вопрос
   - Например, используй MCP инструменты для:
     * Получения информации о Git репозитории (статус, ветки, коммиты и т.д.)
     * Получения информации о проекте, файлах, зависимостях
     * Выполнения различных операций, доступных через MCP
   - Не стесняйся использовать несколько инструментов одновременно, если это необходимо

3. ПРИОРИТЕТЫ:
   - Сначала используй информацию из RAG (она уже предоставлена в контексте)
   - Затем используй MCP инструменты для получения дополнительной информации или выполнения действий
   - Комбинируй информацию из разных источников для максимально полного ответа

4. ФОРМАТ ОТВЕТА:
   - Будь конкретным и полезным
   - Структурируй информацию для лучшей читаемости
   - Приводи примеры кода, если это уместно
   - Если вопрос требует дополнительной информации, запроси её у пользователя
   - Будь кратким, но информативным

5. РАБОТА С КОДОМ:
   - Если вопрос касается кода, используй информацию из RAG и MCP инструменты для получения актуального контекста
   - Предлагай конкретные решения с примерами
   - Указывай на best practices и возможные проблемы

Помни: твоя цель - максимально эффективно помочь разработчику решить его вопрос, используя все доступные ресурсы (RAG и MCP инструменты)."""
    }
}
