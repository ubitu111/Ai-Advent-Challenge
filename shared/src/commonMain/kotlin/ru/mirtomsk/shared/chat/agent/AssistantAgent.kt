package ru.mirtomsk.shared.chat.agent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.cache.ChatCache
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.mapper.OpenAiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.config.PersonalizationReader
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.LocalChatApiService
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Виртуальный помощник с персонализацией
 * Использует информацию о пользователе для персонализированного общения
 * Команда: /assistant <вопрос>
 */
class AssistantAgent(
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
    json: Json,
    localChatApiService: LocalChatApiService,
    openAiResponseMapper: OpenAiResponseMapper,
) : BaseAiAgent(
    name = "AssistantAgent",
    systemPrompt = buildSystemPrompt(),
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
        return text.trim()
    }

    companion object {
        /**
         * Строит системный промпт с учетом персонализации из Personalization.md
         */
        private fun buildSystemPrompt(): String {
            val basePrompt = buildString {
                appendLine("Ты виртуальный персональный помощник, который помогает пользователю в различных задачах.")
                appendLine()
                appendLine("Твоя основная задача - быть полезным, дружелюбным и понимающим помощником.")
                appendLine("Ты имеешь доступ к различным инструментам через MCP (Model Context Protocol) сервер, которые позволяют тебе получать актуальную информацию и выполнять различные действия.")
                appendLine()
                appendLine("ПРАВИЛА РАБОТЫ:")
                appendLine("1. Обращайся к пользователю по имени")
                appendLine("2. Будь вежливым, профессиональным и дружелюбным")
                appendLine("3. Используй доступные MCP инструменты, когда они нужны для ответа на вопрос")
                appendLine("4. Адаптируй свой стиль общения под пользователя, учитывая его предпочтения и привычки")
                appendLine("5. Помни контекст предыдущих сообщений в разговоре")
                appendLine("6. Если вопрос требует актуальных данных (время, дата, погода, курсы валют и т.д.), ОБЯЗАТЕЛЬНО используй соответствующие инструменты")
                appendLine()
            }

            val personalizationContent = PersonalizationReader.readPersonalization()
            if (personalizationContent != null && personalizationContent.isNotBlank()) {
                val personalizationSection = buildString {
                    appendLine("ИНФОРМАЦИЯ О ПОЛЬЗОВАТЕЛЕ:")
                    appendLine()
                    appendLine(personalizationContent)
                    appendLine()
                    appendLine("ВАЖНО: Используй эту информацию для персонализированного общения:")
                    appendLine("- Учитывай всю предоставленную информацию о пользователе")
                    appendLine("- Адаптируй стиль общения под его предпочтения и привычки")
                    appendLine("- Используй эту информацию для более точных и релевантных ответов")
                    appendLine()
                }
                
                return basePrompt + personalizationSection
            }
            
            return basePrompt
        }
    }
}
