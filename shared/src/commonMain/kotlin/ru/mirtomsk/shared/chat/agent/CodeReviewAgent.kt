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
import ru.mirtomsk.shared.network.ILocalChatApiService
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.rag.RagService
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Агент для ревью кода на Kotlin
 * Использует RAG для получения контекста проекта и MCP для дополнительной информации
 */
class CodeReviewAgent(
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
    localChatApiService: ILocalChatApiService,
    openAiResponseMapper: OpenAiResponseMapper,
) : BaseAiAgent(
    name = "CodeReviewAgent",
    systemPrompt = CODE_REVIEW_PROMPT,
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
        // Для ревью с параметрами используем RAG для получения контекста проекта
        val ragContext = text
            .takeIf { it.isNotBlank() }
            ?.let { ragService.retrieveRelevantContext(it) }
        if (ragContext != null && ragContext.isNotBlank()) {
            conversationCache.add(
                AiRequest.Message(
                    role = MessageRoleDto.SYSTEM,
                    text = "Контекст проекта из RAG:\n$ragContext"
                )
            )
        }

        // Если команда /review без параметров, добавляем промпт для ревью измененных файлов Git
        return text.ifBlank {
            conversationCache.add(
                AiRequest.Message(
                    role = MessageRoleDto.SYSTEM,
                    text = """
                    |Пользователь запросил ревью измененных файлов в Git репозитории.
                    |
                    |Твоя задача:
                    |1. Используй MCP инструменты для получения измененных файлов Git
                    |2. Используй при доступности MCP инструменты для получения содержимого каждого измененного файла
                    |3. Проведи детальный ревью каждого измененного файла согласно правилам, указанным в твоем системном промпте
                    |
                    |Важно: Используй доступные MCP инструменты для получения информации о Git статусе и содержимом файлов.
                    """.trimMargin()
                )
            )
            return "Проведи ревью всех измененных файлов в Git репозитории. Используй MCP инструменты для получения статуса Git и содержимого файлов."
        }
    }

    companion object {
        private const val CODE_REVIEW_PROMPT =
            """Ты опытный code reviewer, специализирующийся на языке программирования Kotlin.

Твоя задача - проводить качественный и детальный код-ревью, выявляя:
1. Потенциальные баги и ошибки
2. Проблемы с производительностью
3. Нарушения best practices и code style для Kotlin
4. Проблемы с архитектурой и дизайном кода
5. Отсутствие обработки ошибок
6. Проблемы с безопасностью
7. Нарушения принципов SOLID и других принципов проектирования
8. Проблемы с читаемостью и поддерживаемостью кода

Ты имеешь доступ к:
- RAG (Retrieval-Augmented Generation) системе, которая предоставляет контекст проекта, документацию и примеры кода
- MCP (Model Context Protocol) инструментам для получения дополнительной информации о проекте, файлах, зависимостях и т.д.

Правила работы:
1. ВСЕГДА используй RAG для получения контекста проекта перед началом ревью
2. Используй MCP инструменты для получения информации о структуре проекта, зависимостях, конфигурации и т.д., если это необходимо для качественного ревью
3. Будь конструктивным и конкретным в своих замечаниях
4. Предлагай конкретные улучшения и альтернативные решения
5. Обращай внимание на соответствие кода принятым в проекте стандартам и конвенциям
6. Проверяй соответствие кода архитектуре проекта, если она известна из контекста

Формат ответа:
- Структурируй замечания по категориям (критичные, важные, рекомендации)
- Для каждого замечания указывай конкретное место в коде (если возможно)
- Предлагай конкретные исправления или улучшения
- Будь вежливым и профессиональным

Начни ревью с получения контекста проекта через RAG, затем проанализируй предоставленный код."""
    }
}

