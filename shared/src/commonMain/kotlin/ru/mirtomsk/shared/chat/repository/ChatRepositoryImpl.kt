package ru.mirtomsk.shared.chat.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import ru.mirtomsk.shared.chat.context.ContextResetProvider
import ru.mirtomsk.shared.chat.repository.cache.ChatCache
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.AiResponse
import ru.mirtomsk.shared.chat.repository.model.FunctionCall
import ru.mirtomsk.shared.chat.repository.model.MessageResponseDto
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.agent.AgentTypeDto
import ru.mirtomsk.shared.network.agent.AgentTypeProvider
import ru.mirtomsk.shared.network.compression.ContextCompressionProvider
import ru.mirtomsk.shared.network.format.ResponseFormat
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.mcp.model.McpTool
import ru.mirtomsk.shared.network.prompt.SystemPromptDto
import ru.mirtomsk.shared.network.prompt.SystemPromptProvider
import ru.mirtomsk.shared.network.rag.RagService
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Implementation of ChatRepository for Yandex GPT Pro model
 */
class ChatRepositoryImpl(
    private val chatApiService: ChatApiService,
    private val apiConfig: ApiConfig,
    private val ioDispatcher: CoroutineDispatcher,
    private val yandexResponseMapper: AiResponseMapper,
    private val formatProvider: ResponseFormatProvider,
    private val agentTypeProvider: AgentTypeProvider,
    private val systemPromptProvider: SystemPromptProvider,
    private val contextResetProvider: ContextResetProvider,
    private val temperatureProvider: TemperatureProvider,
    private val maxTokensProvider: MaxTokensProvider,
    private val contextCompressionProvider: ContextCompressionProvider,
    private val ragService: RagService,
    private val chatCache: ChatCache,
    private val mcpToolsProvider: McpToolsProvider,
    private val mcpOrchestrator: McpOrchestrator,
    private val json: Json,
) : ChatRepository {

    private val cacheMutex = Mutex()
    private var lastAgentType: AgentTypeDto? = null
    private var lastSystemPrompt: SystemPromptDto? = null
    private var lastResetCounter: Long = 0L

    override suspend fun sendMessage(text: String, forceRag: Boolean): MessageResponseDto? {
        return withContext(ioDispatcher) {
            val availableMcpTools = mcpToolsProvider.getAvailableTools()
            sendMessageYandexGpt(text, availableMcpTools, forceRag)
        }
    }

    /**
     * Отправка сообщения для Yandex GPT моделей
     */
    private suspend fun sendMessageYandexGpt(
        text: String,
        availableMcpTools: List<McpTool>,
        forceRag: Boolean,
    ): MessageResponseDto? {
        val format = formatProvider.responseFormat.first()
        val agentType = agentTypeProvider.agentType.first()
        val systemPrompt = systemPromptProvider.systemPrompt.first()
        val temperature = temperatureProvider.temperature.first()
        val maxTokens = maxTokensProvider.maxTokens.first()
        val resetCounter = contextResetProvider.resetCounter.first()

        return cacheMutex.withLock {
            // Управление кешем
            manageCache(
                agentType = agentType,
                systemPrompt = systemPrompt,
                resetCounter = resetCounter
            )

            // Получаем текущий кеш
            val conversationCache = chatCache.getMessages().toMutableList()

            // Добавляем системное сообщение с базовым промптом, если кеш пуст
            if (conversationCache.isEmpty()) {
                val basePrompt = selectPrompt(systemPrompt, format)
                conversationCache.add(
                    AiRequest.Message(
                        role = MessageRoleDto.SYSTEM,
                        text = basePrompt,
                    )
                )
            }

            // Получаем RAG контекст для текущего запроса (если RAG включен или принудительно запрошен)
            // Добавляем его перед пользовательским сообщением, если контекст найден
            val ragContext = getRagContextIfEnabled(text, forceRag)
            if (ragContext != null) {
                conversationCache.add(
                    AiRequest.Message(
                        role = MessageRoleDto.SYSTEM,
                        text = ragContext,
                    )
                )
            }

            // Добавляем текущее сообщение пользователя в кеш
            addUserMessage(conversationCache, text)

            // Фиксируем время начала запроса
            val requestStartTime = System.currentTimeMillis()

            // Получаем доступные MCP инструменты
            val tools = if (availableMcpTools.isNotEmpty()) {
                convertMcpToolsToYandexFormat(availableMcpTools)
            } else {
                null
            }

            // Формируем и отправляем запрос
            val request = AiRequest(
                modelUri = "gpt://${apiConfig.keyId}/${getYandexModel(agentType)}",
                completionOptions = AiRequest.CompletionOptions(
                    stream = true,
                    temperature = temperature,
                    maxTokens = maxTokens,
                ),
                messages = conversationCache,
                tools = tools,
            )
            val responseBody = chatApiService.requestYandexGpt(request)
            val response = yandexResponseMapper.mapResponseBody(responseBody, format)

            // Обрабатываем tool calls если они есть
            var currentResponse = response
            var iterationCount = 0
            val maxToolCallIterations = 5 // Ограничение на количество итераций вызовов инструментов

            while (iterationCount < maxToolCallIterations) {
                // Получаем tool calls из ответа (поддерживаем оба формата: toolCalls и toolCallList)
                val message = currentResponse.result.alternatives.firstOrNull()?.message ?: break
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
                        // Поддерживаем оба формата: function (OpenAI) и functionCall (Yandex GPT)
                        val functionCall = toolCall.functionCall
                            ?: (toolCall.function?.let { func ->
                                // Конвертируем OpenAI формат в Yandex GPT формат
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
                            // Преобразуем arguments из Map<String, JsonElement> в JSON строку для MCP
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
                    modelUri = "gpt://${apiConfig.keyId}/${getYandexModel(agentType)}",
                    completionOptions = AiRequest.CompletionOptions(
                        stream = true,
                        temperature = temperature,
                        maxTokens = maxTokens,
                    ),
                    messages = conversationCache,
                    tools = tools, // Передаем инструменты снова на случай, если нужны дополнительные вызовы
                )

                val followUpResponseBody = chatApiService.requestYandexGpt(followUpRequest)
                currentResponse = yandexResponseMapper.mapResponseBody(followUpResponseBody, format)
            }

            // Фиксируем время окончания запроса
            val requestEndTime = System.currentTimeMillis()

            // Обрабатываем финальный ответ и добавляем в кеш
            val assistantMessage = processYandexResponse(conversationCache, currentResponse, format)

            // Сохраняем обновленный кеш
            chatCache.saveMessages(conversationCache)

            // Проверяем необходимость сжатия контекста
            compressContextIfNeeded(agentType, format, temperature, maxTokens)

            // Извлекаем информацию о токенах из ответа
            val promptTokens = response.result.usage.inputTextTokens.toIntOrNull()
            val completionTokens = response.result.usage.completionTokens.toIntOrNull()
            val totalTokens = response.result.usage.totalTokens.toIntOrNull()

            // Возвращаем сообщение с временем запроса и токенами
            assistantMessage?.let {
                val messageText = assistantMessage.text
                    ?: AiMessage.MessageContent.Text("") // Если text отсутствует (только tool calls), используем пустую строку

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


    /**
     * Сжатие контекста, если необходимо
     */
    private suspend fun compressContextIfNeeded(
        agentType: AgentTypeDto,
        format: ResponseFormat?,
        temperature: Float,
        maxTokens: Int
    ) {
        val isCompressionEnabled = contextCompressionProvider.isCompressionEnabled.first()
        val conversationCache = chatCache.getMessages()
        if (!isCompressionEnabled || conversationCache.size <= MAX_CONTEXT_WINDOW_SIZE) {
            return
        }

        val compressionMessages = conversationCache.toMutableList()
        compressionMessages.add(
            AiRequest.Message(
                role = MessageRoleDto.USER,
                text = Prompts.CONTEXT_COMPRESSION,
            )
        )

        // Отправляем запрос на сжатие
        val compressedContext = compressContextYandexGpt(agentType, format, temperature, maxTokens, compressionMessages)

        if (compressedContext != null) {
            // Сохраняем системный промпт, если он был
            val systemPromptMessage =
                conversationCache.firstOrNull { it.role == MessageRoleDto.SYSTEM }

            // Очищаем кеш
            chatCache.clear()

            // Восстанавливаем системный промпт и добавляем сжатый контекст
            val newCache = mutableListOf<AiRequest.Message>()
            if (systemPromptMessage != null) {
                newCache.add(systemPromptMessage)
            }
            addAssistantMessage(newCache, compressedContext)

            // Сохраняем обновленный кеш
            chatCache.saveMessages(newCache)
        }
    }

    /**
     * Сжатие контекста для Yandex GPT
     */
    private suspend fun compressContextYandexGpt(
        agentType: AgentTypeDto,
        format: ResponseFormat?,
        temperature: Float,
        maxTokens: Int,
        compressionMessages: List<AiRequest.Message>,
    ): String? {
        val formatValue = format ?: formatProvider.responseFormat.first()

        val request = AiRequest(
            modelUri = "gpt://${apiConfig.keyId}/${getYandexModel(agentType)}",
            completionOptions = AiRequest.CompletionOptions(
                stream = false,
                temperature = temperature,
                maxTokens = maxTokens,
            ),
            messages = compressionMessages,
        )

        val responseBody = chatApiService.requestYandexGpt(request)
        val response = yandexResponseMapper.mapResponseBody(responseBody, formatValue)

        val compressedMessage = response.result.alternatives
            .find { it.message.role == MessageRoleDto.ASSISTANT }
            ?.message
            ?: return null

        return when (val content = compressedMessage.text) {
            is AiMessage.MessageContent.Text -> content.value
            is AiMessage.MessageContent.Json -> {
                val jsonResponse = content.value
                val linksText = if (jsonResponse.resource.isNotEmpty()) {
                    "\nСсылки:\n${jsonResponse.resource.joinToString("\n") { it.link }}"
                } else ""
                "${jsonResponse.title}\n${jsonResponse.text}$linksText"
            }

            else -> AiMessage.MessageContent.Text(content.toString()).value
        }
    }


    /**
     * Управление кешем разговора (общий метод для обоих типов моделей)
     */
    private suspend fun manageCache(
        agentType: AgentTypeDto,
        systemPrompt: SystemPromptDto?,
        resetCounter: Long
    ) {
        // Check if context was reset
        if (resetCounter != lastResetCounter) {
            chatCache.clear()
            lastAgentType = null
            lastSystemPrompt = null
            lastResetCounter = resetCounter
        }

        // Check if model changed
        if (lastAgentType != null && lastAgentType != agentType) {
            chatCache.clear()
        }

        // Check if system prompt changed (только для Yandex GPT)
        if (systemPrompt != null && lastSystemPrompt != null && lastSystemPrompt != systemPrompt) {
            chatCache.clear()
        }

        lastAgentType = agentType
        lastSystemPrompt = systemPrompt
    }

    /**
     * Добавление сообщения пользователя в кеш
     */
    private fun addUserMessage(cache: MutableList<AiRequest.Message>, text: String) {
        cache.add(
            AiRequest.Message(
                role = MessageRoleDto.USER,
                text = text,
            )
        )
    }

    /**
     * Добавление сообщения ассистента в кеш
     */
    private fun addAssistantMessage(cache: MutableList<AiRequest.Message>, text: String) {
        cache.add(
            AiRequest.Message(
                role = MessageRoleDto.ASSISTANT,
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
                    // Для JSON формата преобразуем в текстовое представление
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
     * Получает RAG контекст, если RAG включен или принудительно запрошен
     */
    private suspend fun getRagContextIfEnabled(query: String, forceRag: Boolean): String? {
        if (!forceRag) return null

        return ragService.retrieveRelevantContext(query)
    }

    private fun selectPrompt(systemPrompt: SystemPromptDto, format: ResponseFormat): String {
        val basePrompt = when (systemPrompt) {
            SystemPromptDto.DEFAULT -> Prompts.DEFAULT
            SystemPromptDto.SPECIFYING_QUESTIONS -> Prompts.SPECIFYING_QUESTIONS
            SystemPromptDto.LOGIC_BY_STEP -> Prompts.LOGIC_BY_STEP
            SystemPromptDto.LOGIC_AGENT_GROUP -> Prompts.LOGIC_GROUP
            SystemPromptDto.LOGIC_SIMPLE -> Prompts.LOGIC_SIMPLE
        }
        val formatPrompt = when (format) {
            ResponseFormat.DEFAULT -> Prompts.DEFAULT_FORMAT_RESPONSE
            ResponseFormat.JSON -> Prompts.JSON_FORMAT_RESPONSE
        }
        return "$basePrompt$formatPrompt"
    }

    /**
     * Получение имени модели Yandex GPT
     * Всегда возвращает PRO модель
     */
    private fun getYandexModel(agentType: AgentTypeDto): String {
        return agentType.modelId
    }


    /**
     * Convert MCP tools to Yandex GPT API format
     * Adds information about MCP availability to tool descriptions
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


    private companion object {
        const val MAX_CONTEXT_WINDOW_SIZE = 5
    }
}

