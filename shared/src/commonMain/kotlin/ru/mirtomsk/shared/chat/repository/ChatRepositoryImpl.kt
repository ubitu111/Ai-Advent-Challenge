package ru.mirtomsk.shared.chat.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.chat.context.ContextResetProvider
import ru.mirtomsk.shared.chat.repository.cache.ChatCache
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.mapper.HuggingFaceResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.AiResponse
import ru.mirtomsk.shared.chat.repository.model.MessageResponseDto
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.HuggingFaceMessage
import ru.mirtomsk.shared.network.HuggingFaceParameters
import ru.mirtomsk.shared.network.HuggingFaceTool
import ru.mirtomsk.shared.network.HuggingFaceToolFunction
import ru.mirtomsk.shared.network.HuggingFaceToolParameters
import ru.mirtomsk.shared.network.HuggingFaceToolProperty
import ru.mirtomsk.shared.network.agent.AgentTypeDto
import ru.mirtomsk.shared.network.agent.AgentTypeProvider
import ru.mirtomsk.shared.network.compression.ContextCompressionProvider
import ru.mirtomsk.shared.network.format.ResponseFormat
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpApiService
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.mcp.model.McpTool
import ru.mirtomsk.shared.chat.repository.model.FunctionCall
import ru.mirtomsk.shared.network.prompt.SystemPromptDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.mirtomsk.shared.network.prompt.SystemPromptProvider
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Unified implementation of ChatRepository for all AI models (Yandex GPT and HuggingFace)
 * Provides a single point of access for all model requests
 */
class ChatRepositoryImpl(
    private val chatApiService: ChatApiService,
    private val apiConfig: ApiConfig,
    private val ioDispatcher: CoroutineDispatcher,
    private val yandexResponseMapper: AiResponseMapper,
    private val huggingFaceResponseMapper: HuggingFaceResponseMapper,
    private val formatProvider: ResponseFormatProvider,
    private val agentTypeProvider: AgentTypeProvider,
    private val systemPromptProvider: SystemPromptProvider,
    private val contextResetProvider: ContextResetProvider,
    private val temperatureProvider: TemperatureProvider,
    private val maxTokensProvider: MaxTokensProvider,
    private val contextCompressionProvider: ContextCompressionProvider,
    private val chatCache: ChatCache,
    private val mcpToolsProvider: McpToolsProvider,
    private val mcpApiService: McpApiService,
    private val json: Json,
) : ChatRepository {

    private val cacheMutex = Mutex()
    private var lastAgentType: AgentTypeDto? = null
    private var lastSystemPrompt: SystemPromptDto? = null
    private var lastResetCounter: Long = 0L

    override suspend fun sendMessage(text: String): MessageResponseDto? {
        return withContext(ioDispatcher) {
            val agentType = agentTypeProvider.agentType.first()

            if (agentType.isYandexGpt) {
                sendMessageYandexGpt(text)
            } else {
                sendMessageHuggingFace(text)
            }
        }
    }

    /**
     * Отправка сообщения для Yandex GPT моделей
     */
    private suspend fun sendMessageYandexGpt(text: String): MessageResponseDto? {
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

            // Добавляем системное сообщение, если кеш пуст
            if (conversationCache.isEmpty()) {
                conversationCache.add(
                    AiRequest.Message(
                        role = MessageRoleDto.SYSTEM,
                        text = selectPrompt(systemPrompt, format),
                    )
                )
            }

            // Добавляем текущее сообщение пользователя в кеш
            addUserMessage(conversationCache, text)

            // Фиксируем время начала запроса
            val requestStartTime = System.currentTimeMillis()

            // Получаем выбранные MCP инструменты
            val selectedMcpTools = mcpToolsProvider.getSelectedTools()
            val tools = if (selectedMcpTools.isNotEmpty()) {
                convertMcpToolsToYandexFormat(selectedMcpTools)
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
                                        kotlinx.serialization.json.buildJsonObject { 
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
                            
                            val toolResult = mcpApiService.callTool(
                                toolName = functionCall.name,
                                arguments = argumentsJson
                            )
                            toolResults.add("Tool: ${functionCall.name}\nResult: $toolResult")
                        }
                    } catch (e: Exception) {
                        val toolName = toolCall.functionCall?.name ?: toolCall.function?.name ?: "unknown"
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
     * Отправка сообщения для HuggingFace моделей
     */
    private suspend fun sendMessageHuggingFace(text: String): MessageResponseDto? {
        val agentType = agentTypeProvider.agentType.first()
        val temperature = temperatureProvider.temperature.first()
        val maxTokens = maxTokensProvider.maxTokens.first()
        val resetCounter = contextResetProvider.resetCounter.first()

        return cacheMutex.withLock {
            // Управление кешем
            manageCache(
                agentType = agentType,
                systemPrompt = null,
                resetCounter = resetCounter
            )

            // Получаем текущий кеш
            val conversationCache = chatCache.getMessages().toMutableList()

            // Добавляем текущее сообщение пользователя в кеш
            addUserMessage(conversationCache, text)

            // Преобразуем кеш в формат HuggingFace messages
            val messages = conversationCache.map { message ->
                HuggingFaceMessage(
                    role = when (message.role) {
                        MessageRoleDto.USER -> "user"
                        MessageRoleDto.ASSISTANT -> "assistant"
                        MessageRoleDto.SYSTEM -> "system"
                    },
                    content = message.text
                )
            }

            // Формируем параметры запроса
            val parameters = HuggingFaceParameters(
                max_new_tokens = maxTokens,
                temperature = temperature.toDouble(),
            )

            // Получаем выбранные MCP инструменты
            val selectedMcpTools = mcpToolsProvider.getSelectedTools()
            val tools = if (selectedMcpTools.isNotEmpty()) {
                convertMcpToolsToHuggingFaceFormat(selectedMcpTools)
            } else {
                null
            }

            // Фиксируем время начала запроса
            val requestStartTime = System.currentTimeMillis()

            // Отправляем запрос и получаем сырой ответ
            var rawResponse = chatApiService.requestHuggingFace(
                model = agentType,
                messages = messages,
                parameters = parameters,
                tools = tools,
            )

            // Парсим ответ через маппер
            var huggingFaceResponse = huggingFaceResponseMapper.mapResponseBody(rawResponse)

            // Обрабатываем tool calls если они есть
            var iterationCount = 0
            val maxToolCallIterations = 5 // Ограничение на количество итераций вызовов инструментов

            while (huggingFaceResponse.toolCalls != null && huggingFaceResponse.toolCalls!!.isNotEmpty() && iterationCount < maxToolCallIterations) {
                iterationCount++
                
                // Добавляем сообщение ассистента в кеш
                addAssistantMessage(conversationCache, huggingFaceResponse.content)

                // Вызываем каждый инструмент через MCP
                val toolResults = mutableListOf<String>()
                for (toolCall in huggingFaceResponse.toolCalls!!) {
                    try {
                        val toolResult = mcpApiService.callTool(
                            toolName = toolCall.function.name,
                            arguments = toolCall.function.arguments
                        )
                        toolResults.add("Tool: ${toolCall.function.name}\nResult: $toolResult")
                    } catch (e: Exception) {
                        toolResults.add("Tool: ${toolCall.function.name}\nError: ${e.message}")
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

                // Преобразуем обновленный кеш в формат HuggingFace messages
                val updatedMessages = conversationCache.map { message ->
                    HuggingFaceMessage(
                        role = when (message.role) {
                            MessageRoleDto.USER -> "user"
                            MessageRoleDto.ASSISTANT -> "assistant"
                            MessageRoleDto.SYSTEM -> "system"
                        },
                        content = message.text
                    )
                }

                // Отправляем новый запрос с результатами инструментов
                rawResponse = chatApiService.requestHuggingFace(
                    model = agentType,
                    messages = updatedMessages,
                    parameters = parameters,
                    tools = tools, // Передаем инструменты снова на случай, если нужны дополнительные вызовы
                )
                
                huggingFaceResponse = huggingFaceResponseMapper.mapResponseBody(rawResponse)
            }

            // Фиксируем время окончания запроса
            val requestEndTime = System.currentTimeMillis()

            // Добавляем финальное сообщение ассистента в кеш
            addAssistantMessage(conversationCache, huggingFaceResponse.content)

            // Сохраняем обновленный кеш
            chatCache.saveMessages(conversationCache)

            // Проверяем необходимость сжатия контекста
            compressContextIfNeeded(agentType, null, temperature, maxTokens)

            // Возвращаем сообщение в формате AiMessage с временем запроса и токенами
            MessageResponseDto(
                role = MessageRoleDto.ASSISTANT,
                text = AiMessage.MessageContent.Text(huggingFaceResponse.content),
                requestTime = requestEndTime - requestStartTime,
                promptTokens = huggingFaceResponse.promptTokens,
                completionTokens = huggingFaceResponse.completionTokens,
                totalTokens = huggingFaceResponse.totalTokens,
            )
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
        val compressedContext = if (agentType.isYandexGpt) {
            compressContextYandexGpt(agentType, format, temperature, maxTokens, compressionMessages)
        } else {
            compressContextHuggingFace(agentType, temperature, maxTokens, compressionMessages)
        }

        if (compressedContext != null) {
            // Сохраняем системный промпт, если он был
            val systemPromptMessage = conversationCache.firstOrNull { it.role == MessageRoleDto.SYSTEM }

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
     * Сжатие контекста для HuggingFace
     */
    private suspend fun compressContextHuggingFace(
        agentType: AgentTypeDto,
        temperature: Float,
        maxTokens: Int,
        compressionMessages: List<AiRequest.Message>,
    ): String? {
        val messages = compressionMessages.map {
            HuggingFaceMessage(role = it.role.name, content = it.text)
        }

        val parameters = HuggingFaceParameters(
            max_new_tokens = maxTokens,
            temperature = temperature.toDouble(),
        )

        val rawResponse = chatApiService.requestHuggingFace(
            model = agentType,
            messages = messages,
            parameters = parameters,
        )

        val huggingFaceResponse = huggingFaceResponseMapper.mapResponseBody(rawResponse)
        return huggingFaceResponse.content
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
     */
    private fun getYandexModel(agentType: AgentTypeDto): String {
        return when (agentType) {
            AgentTypeDto.LITE -> MODEL_LITE
            AgentTypeDto.PRO -> MODEL_PRO
            else -> throw IllegalArgumentException("Model ${agentType.name} is not a Yandex GPT model")
        }
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

    /**
     * Convert MCP tools to HuggingFace API format
     * Adds information about MCP availability to tool descriptions
     */
    private fun convertMcpToolsToHuggingFaceFormat(mcpTools: List<McpTool>): List<HuggingFaceTool> {
        return mcpTools.map { mcpTool ->
            val enhancedDescription = buildString {
                mcpTool.description?.let { append(it) }
                append(" (Доступен через MCP сервер. Используй этот инструмент, когда он нужен для ответа на вопрос пользователя.)")
            }
            
            HuggingFaceTool(
                type = "function",
                function = HuggingFaceToolFunction(
                    name = mcpTool.name,
                    description = enhancedDescription,
                    parameters = mcpTool.inputSchema?.let { schema ->
                        HuggingFaceToolParameters(
                            type = schema.type ?: "object",
                            properties = schema.properties?.mapValues { (_, prop) ->
                                HuggingFaceToolProperty(
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
        const val MODEL_LITE = "yandexgpt-lite"
        const val MODEL_PRO = "yandexgpt"
        const val MAX_CONTEXT_WINDOW_SIZE = 5
    }
}

