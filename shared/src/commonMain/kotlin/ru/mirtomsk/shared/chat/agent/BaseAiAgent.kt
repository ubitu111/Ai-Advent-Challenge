package ru.mirtomsk.shared.chat.agent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import ru.mirtomsk.shared.chat.repository.cache.ChatCache
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.mapper.OpenAiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.AiResponse
import ru.mirtomsk.shared.chat.repository.model.FunctionCall
import ru.mirtomsk.shared.chat.repository.model.MessageResponseDto
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.LocalChatApiService
import ru.mirtomsk.shared.network.agent.ModelTypeDto
import ru.mirtomsk.shared.network.format.ResponseFormat
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.mcp.model.McpTool
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Базовая реализация AI агента
 * Содержит общую логику для работы с AI моделью, кешем и MCP инструментами
 */
abstract class BaseAiAgent(
    override val name: String,
    override val systemPrompt: String,
    protected val chatApiService: ChatApiService,
    protected val apiConfig: ApiConfig,
    protected val ioDispatcher: CoroutineDispatcher,
    protected val yandexResponseMapper: AiResponseMapper,
    protected val formatProvider: ResponseFormatProvider,
    protected val temperatureProvider: TemperatureProvider,
    protected val maxTokensProvider: MaxTokensProvider,
    protected val chatCache: ChatCache,
    protected val mcpToolsProvider: McpToolsProvider,
    protected val mcpOrchestrator: McpOrchestrator,
    protected val json: Json,
    // Новые параметры для локальной модели
    private val localChatApiService: LocalChatApiService? = null,
    private val openAiResponseMapper: OpenAiResponseMapper? = null,
) : AiAgent {

    protected val cacheMutex = Mutex()
    private val modelType = ModelTypeDto.PRO

    /**
     * Обработка сообщения с базовой логикой
     */
    override suspend fun processMessage(text: String, command: ChatCommand): MessageResponseDto? {
        return withContext(ioDispatcher) {
            val availableMcpTools = mcpToolsProvider.getAvailableTools()

            cacheMutex.withLock {
                // Получаем текущий кеш
                val conversationCache = chatCache.getMessages().toMutableList()

                // Добавляем системное сообщение с промптом агента, если кеш пуст
                if (conversationCache.isEmpty()) {
                    conversationCache.add(
                        AiRequest.Message(
                            role = MessageRoleDto.SYSTEM,
                            text = systemPrompt,
                        )
                    )
                }

                // Обработка специфичной для агента логики (RAG, дополнительные сообщения и т.д.)
                val processedText = preprocessMessage(text, command, conversationCache)

                // Добавляем текущее сообщение пользователя в кеш
                addUserMessage(conversationCache, processedText)

                // Фиксируем время начала запроса
                val requestStartTime = System.currentTimeMillis()

                // Получаем доступные MCP инструменты
                val tools = if (shouldUseMcpTools(command)) {
                    convertMcpToolsToYandexFormat(availableMcpTools)
                } else {
                    null
                }

                // Формируем и отправляем запрос
                val format = formatProvider.responseFormat.first()
                val temperature = temperatureProvider.temperature.first()
                val maxTokens = maxTokensProvider.maxTokens.first()

                val request = AiRequest(
                    modelUri = "gpt://${apiConfig.keyId}/${modelType.modelId}",
                    completionOptions = AiRequest.CompletionOptions(
                        stream = true,
                        temperature = temperature,
                        maxTokens = maxTokens,
                    ),
                    messages = conversationCache,
                    tools = tools,
                )
                
                // Используем локальную модель, если она включена и доступна
                val useLocalModel = apiConfig.useLocalModel && localChatApiService != null && openAiResponseMapper != null
                val responseBody = if (useLocalModel) {
                    localChatApiService!!.requestLocalLlm(request)
                } else {
                    chatApiService.requestYandexGpt(request)
                }
                val response = if (useLocalModel) {
                    openAiResponseMapper!!.mapResponseBody(responseBody, format)
                } else {
                    yandexResponseMapper.mapResponseBody(responseBody, format)
                }

                // Обрабатываем tool calls если они есть
                var currentResponse = response
                var iterationCount = 0
                val maxToolCallIterations = 5

                while (iterationCount < maxToolCallIterations) {
                    val message =
                        currentResponse.result.alternatives.firstOrNull()?.message ?: break
                    val toolCalls = message.toolCallList?.toolCalls
                        ?: message.toolCalls
                        ?: break

                    if (toolCalls.isEmpty()) break

                    iterationCount++

                    // Добавляем сообщение ассистента с tool calls в кеш (если есть текст)
                    message.text?.let { textContent ->
                        val assistantMessageText = when (textContent) {
                            is AiMessage.MessageContent.Text -> textContent.value
                            is AiMessage.MessageContent.Json -> {
                                val jsonResponse = textContent.value
                                val linksText = if (jsonResponse.resource.isNotEmpty()) {
                                    "\nСсылки:\n${jsonResponse.resource.joinToString("\n") { it.link }}"
                                } else ""
                                "${jsonResponse.title}\n${jsonResponse.text}$linksText"
                            }
                        }

                        conversationCache.add(
                            AiRequest.Message(
                                role = MessageRoleDto.ASSISTANT,
                                text = assistantMessageText
                            )
                        )
                    }

                    // Вызываем каждый инструмент через MCP
                    val toolResults = mutableListOf<String>()
                    for (toolCall in toolCalls) {
                        try {
                            val functionCall = toolCall.functionCall
                                ?: (toolCall.function?.let { func ->
                                    FunctionCall(
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

                    // Добавляем результаты инструментов в контекст
                    val toolResultsText = toolResults.joinToString("\n\n")
                    conversationCache.add(
                        AiRequest.Message(
                            role = MessageRoleDto.USER,
                            text = "Результаты вызова инструментов:\n$toolResultsText\n\nИспользуй эти результаты для формирования ответа пользователю."
                        )
                    )

                    // Отправляем новый запрос с результатами инструментов
                    val followUpRequest = AiRequest(
                        modelUri = "gpt://${apiConfig.keyId}/${modelType.modelId}",
                        completionOptions = AiRequest.CompletionOptions(
                            stream = true,
                            temperature = temperature,
                            maxTokens = maxTokens,
                        ),
                        messages = conversationCache,
                        tools = tools,
                    )

                    val followUpResponseBody = if (useLocalModel) {
                        localChatApiService!!.requestLocalLlm(followUpRequest)
                    } else {
                        chatApiService.requestYandexGpt(followUpRequest)
                    }
                    currentResponse = if (useLocalModel) {
                        openAiResponseMapper!!.mapResponseBody(followUpResponseBody, format)
                    } else {
                        yandexResponseMapper.mapResponseBody(followUpResponseBody, format)
                    }
                }

                // Фиксируем время окончания запроса
                val requestEndTime = System.currentTimeMillis()

                // Обрабатываем финальный ответ и добавляем в кеш
                val assistantMessage =
                    processYandexResponse(conversationCache, currentResponse, format)

                // Сохраняем обновленный кеш
                chatCache.saveMessages(conversationCache)

                // Извлекаем информацию о токенах из ответа
                val promptTokens = response.result.usage.inputTextTokens.toIntOrNull()
                val completionTokens = response.result.usage.completionTokens.toIntOrNull()
                val totalTokens = response.result.usage.totalTokens.toIntOrNull()

                // Возвращаем сообщение с временем запроса и токенами
                assistantMessage?.let {
                    val messageText = assistantMessage.text
                        ?: AiMessage.MessageContent.Text("")

                    MessageResponseDto(
                        role = assistantMessage.role,
                        text = messageText,
                        requestTime = requestEndTime - requestStartTime,
                        promptTokens = promptTokens,
                        completionTokens = completionTokens,
                        totalTokens = totalTokens,
                    )
                }
            }
        }
    }

    /**
     * Предобработка сообщения перед отправкой в AI
     * Может быть переопределена в подклассах для добавления специфичной логики
     */
    protected abstract suspend fun preprocessMessage(
        text: String,
        command: ChatCommand,
        conversationCache: MutableList<AiRequest.Message>
    ): String

    /**
     * Определяет, нужно ли использовать MCP инструменты
     * Может быть переопределено в подклассах
     */
    protected open fun shouldUseMcpTools(command: ChatCommand): Boolean {
        return true
    }

    /**
     * Добавление сообщения пользователя в кеш
     */
    protected fun addUserMessage(cache: MutableList<AiRequest.Message>, text: String) {
        cache.add(
            AiRequest.Message(
                role = MessageRoleDto.USER,
                text = text,
            )
        )
    }

    /**
     * Обработка ответа Yandex GPT и добавление в кеш
     */
    private fun processYandexResponse(
        cache: MutableList<AiRequest.Message>,
        response: AiResponse,
        format: ResponseFormat
    ): AiMessage? {
        val assistantMessage = response.result.alternatives
            .find { it.message.role == MessageRoleDto.ASSISTANT }
            ?.message
            ?: return null

        // Добавляем сообщение в кеш только если есть текст (не только tool calls)
        assistantMessage.text?.let { content ->
            val messageText = when (content) {
                is AiMessage.MessageContent.Text -> content.value
                is AiMessage.MessageContent.Json -> {
                    val jsonResponse = content.value
                    val linksText = if (jsonResponse.resource.isNotEmpty()) {
                        "\nСсылки:\n${jsonResponse.resource.joinToString("\n") { it.link }}"
                    } else ""
                    "${jsonResponse.title}\n${jsonResponse.text}$linksText"
                }
            }
            cache.add(
                AiRequest.Message(
                    role = MessageRoleDto.ASSISTANT,
                    text = messageText
                )
            )
        }

        return assistantMessage
    }

    /**
     * Convert MCP tools to Yandex GPT API format
     */
    protected fun convertMcpToolsToYandexFormat(mcpTools: List<McpTool>): List<AiRequest.Tool> {
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

    /**
     * Очистка кеша агента
     */
    override suspend fun clearCache() {
        cacheMutex.withLock {
            chatCache.clear()
        }
    }
}

