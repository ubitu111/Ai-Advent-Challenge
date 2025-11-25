package ru.mirtomsk.shared.dollarRate

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.mcp.model.McpTool
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Repository for dollar rate requests
 * Makes requests without message cache
 */
class DollarRateRepository(
    private val chatApiService: ChatApiService,
    private val apiConfig: ApiConfig,
    private val ioDispatcher: CoroutineDispatcher,
    private val yandexResponseMapper: AiResponseMapper,
    private val formatProvider: ResponseFormatProvider,
    private val temperatureProvider: TemperatureProvider,
    private val maxTokensProvider: MaxTokensProvider,
    private val mcpToolsProvider: McpToolsProvider,
    private val mcpOrchestrator: McpOrchestrator,
    private val json: Json,
) {

    /**
     * Request dollar rate summary for the last week
     * @param dates List of dates for the last week (format: "YYYY-MM-DD")
     */
    suspend fun requestDollarRateSummary(dates: List<String>): String? {
        return withContext(ioDispatcher) {
            // Force Yandex GPT Pro - use PRO model directly
            // We don't use agentTypeProvider here to ensure we always use PRO

            val format = formatProvider.responseFormat.first()
            val temperature = temperatureProvider.temperature.first()
            val maxTokens = maxTokensProvider.maxTokens.first()
            val availableMcpTools = mcpToolsProvider.getAvailableTools()

            // Build prompt with dates
            val datesText = dates.joinToString(", ")
            val prompt = """
                Собери данные о курсе доллара США (USD) к российскому рублю (RUB) за каждый день последней недели.
                Даты для проверки: $datesText
                
                После сбора данных за все дни, предоставь суммаризацию:
                - Минимальный курс за неделю
                - Максимальный курс за неделю
                - Средний курс за неделю
                - Тренд (растет/падает/стабилен)
                - Прогноз на ближайшие дни (если возможно)
            """.trimIndent()

            // Create messages without cache
            val messages = mutableListOf<AiRequest.Message>()

            // Add system prompt
            messages.add(
                AiRequest.Message(
                    role = MessageRoleDto.SYSTEM,
                    text = "Ты помощник, который собирает и анализирует данные о курсе валют. Используй доступные инструменты для получения актуальной информации."
                )
            )

            // Add user message
            messages.add(
                AiRequest.Message(
                    role = MessageRoleDto.USER,
                    text = prompt
                )
            )

            // Convert MCP tools to Yandex format
            val tools = if (availableMcpTools.isNotEmpty()) {
                convertMcpToolsToYandexFormat(availableMcpTools)
            } else {
                null
            }

            // Make request - always use Yandex GPT Pro
            val request = AiRequest(
                modelUri = "gpt://${apiConfig.keyId}/yandexgpt",
                completionOptions = AiRequest.CompletionOptions(
                    stream = true,
                    temperature = temperature,
                    maxTokens = maxTokens,
                ),
                messages = messages,
                tools = tools,
            )

            val responseBody = chatApiService.requestYandexGpt(request)
            val response = yandexResponseMapper.mapResponseBody(responseBody, format)

            // Process tool calls if any
            var currentResponse = response
            var iterationCount = 0
            val maxToolCallIterations = 20

            while (iterationCount < maxToolCallIterations) {
                val message = currentResponse.result.alternatives.firstOrNull()?.message ?: break
                val toolCalls = message.toolCallList?.toolCalls
                    ?: message.toolCalls
                    ?: break

                if (toolCalls.isEmpty()) break

                iterationCount++

                // Add assistant message with tool calls if there's text
                message.text?.let { textContent ->
                    val assistantMessageText = when (textContent) {
                        is ru.mirtomsk.shared.chat.repository.model.AiMessage.MessageContent.Text -> textContent.value
                        is ru.mirtomsk.shared.chat.repository.model.AiMessage.MessageContent.Json -> {
                            val jsonResponse = textContent.value
                            val linksText = if (jsonResponse.resource.isNotEmpty()) {
                                "\nСсылки:\n${jsonResponse.resource.joinToString("\n") { it.link }}"
                            } else ""
                            "${jsonResponse.title}\n${jsonResponse.text}$linksText"
                        }
                    }

                    messages.add(
                        AiRequest.Message(
                            role = MessageRoleDto.ASSISTANT,
                            text = assistantMessageText
                        )
                    )
                }

                // Call tools through MCP
                val toolResults = mutableListOf<String>()
                for (toolCall in toolCalls) {
                    try {
                        val functionCall = toolCall.functionCall
                            ?: (toolCall.function?.let { func ->
                                ru.mirtomsk.shared.chat.repository.model.FunctionCall(
                                    name = func.name,
                                    arguments = try {
                                        json.parseToJsonElement(func.arguments).jsonObject
                                    } catch (e: Exception) {
                                        buildJsonObject {
                                            put("input", func.arguments)
                                        }
                                    }
                                )
                            })

                        if (functionCall != null) {
                            val argumentsJsonObject = buildJsonObject {
                                functionCall.arguments.forEach { (key, value) ->
                                    put(key, value)
                                }
                            }
                            val argumentsJson = json.encodeToString(
                                kotlinx.serialization.json.JsonObject.serializer(),
                                argumentsJsonObject
                            )

                            val toolResult = mcpOrchestrator.callTool(
                                toolName = functionCall.name,
                                arguments = argumentsJson
                            )
                            toolResults.add("Tool: ${functionCall.name}\nResult: $toolResult")
                        }
                    } catch (e: Exception) {
                        val toolName =
                            toolCall.functionCall?.name ?: toolCall.function?.name ?: "unknown"
                        toolResults.add("Tool: $toolName\nError: ${e.message}")
                    }
                }

                // Add tool results to context
                val toolResultsText = toolResults.joinToString("\n\n")
                messages.add(
                    AiRequest.Message(
                        role = MessageRoleDto.USER,
                        text = "Результаты вызова инструментов:\n$toolResultsText\n\nИспользуй эти результаты для формирования ответа пользователю."
                    )
                )

                // Make follow-up request
                val followUpRequest = AiRequest(
                    modelUri = "gpt://${apiConfig.keyId}/yandexgpt",
                    completionOptions = AiRequest.CompletionOptions(
                        stream = true,
                        temperature = temperature,
                        maxTokens = maxTokens,
                    ),
                    messages = messages,
                    tools = tools,
                )

                val followUpResponseBody = chatApiService.requestYandexGpt(followUpRequest)
                currentResponse = yandexResponseMapper.mapResponseBody(followUpResponseBody, format)
            }

            // Extract final response
            val assistantMessage = currentResponse.result.alternatives
                .find { it.message.role == MessageRoleDto.ASSISTANT }
                ?.message

            when (val content = assistantMessage?.text) {
                is ru.mirtomsk.shared.chat.repository.model.AiMessage.MessageContent.Text -> content.value
                is ru.mirtomsk.shared.chat.repository.model.AiMessage.MessageContent.Json -> {
                    val jsonResponse = content.value
                    val linksText = if (jsonResponse.resource.isNotEmpty()) {
                        "\nСсылки:\n${jsonResponse.resource.joinToString("\n") { it.link }}"
                    } else ""
                    "${jsonResponse.title}\n${jsonResponse.text}$linksText"
                }

                else -> null
            }
        }
    }

    /**
     * Convert MCP tools to Yandex GPT API format
     */
    private fun convertMcpToolsToYandexFormat(mcpTools: List<McpTool>): List<AiRequest.Tool> {
        return mcpTools.map { mcpTool ->
            val enhancedDescription = buildString {
                mcpTool.description?.let { append(it) }
                append(" (Доступен через MCP сервер. Используй этот инструмент, когда он нужен для ответа на вопрос пользователя.)")
            }

            AiRequest.Tool(
                type = "function",
                function = AiRequest.ToolFunction(
                    name = mcpTool.name,
                    description = enhancedDescription,
                    parameters = mcpTool.inputSchema?.let { schema ->
                        AiRequest.ToolParameters(
                            type = schema.type ?: "object",
                            properties = schema.properties?.mapValues { (_, prop) ->
                                AiRequest.ToolProperty(
                                    type = prop.type,
                                    description = prop.description
                                )
                            },
                            required = schema.required
                        )
                    }
                )
            )
        }
    }
}
