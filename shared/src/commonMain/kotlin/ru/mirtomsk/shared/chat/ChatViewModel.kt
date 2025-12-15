package ru.mirtomsk.shared.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.mirtomsk.shared.chat.model.ChatUiState
import ru.mirtomsk.shared.chat.model.Message
import ru.mirtomsk.shared.chat.model.Message.MessageRole
import ru.mirtomsk.shared.chat.model.MessageContent
import ru.mirtomsk.shared.chat.agent.ChatCommand
import ru.mirtomsk.shared.chat.repository.ChatRepository
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.dollarRate.DollarRateScheduler
import ru.mirtomsk.shared.embeddings.FilePicker
import ru.mirtomsk.shared.network.mcp.McpRepository
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.rag.OllamaApiService
import ru.mirtomsk.shared.chat.repository.model.AiMessage.MessageContent as AiMessageContent

class ChatViewModel(
    private val repository: ChatRepository,
    private val mcpRepository: McpRepository,
    private val mcpToolsProvider: McpToolsProvider,
    private val filePicker: FilePicker,
    private val ollamaApiService: OllamaApiService?,
    private val apiConfig: ApiConfig,
    dollarRateScheduler: DollarRateScheduler,
    mainDispatcher: CoroutineDispatcher,
) {

    private val viewmodelScope = CoroutineScope(mainDispatcher + SupervisorJob())
    var uiState by mutableStateOf(ChatUiState())
        private set

    init {
        // Load MCP tools on app startup
        loadMcpTools()
        // Start dollar rate scheduler
//        dollarRateScheduler.start()
    }

    private fun loadMcpTools() {
        viewmodelScope.launch {
            try {
                val tools = mcpRepository.getTools()
                mcpToolsProvider.updateAvailableTools(tools)
            } catch (e: Exception) {
                println("Error loading MCP tools: ${e.message}")
            }
        }
    }

    fun updateInputText(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun sendMessage() {
        val currentInput = uiState.inputText.trim()
        if (currentInput.isBlank()) return

        // Парсим команду
        val (command, _) = ChatCommand.parse(currentInput)

        // Если команда /analysis, открываем FilePicker
        if (command == ChatCommand.ANALYSIS) {
            selectFileForAnalysis()
            return
        }

        // Если команда /context, проверяем контекстное окно
        if (command == ChatCommand.CONTEXT) {
            checkModelContextWindow()
            // Очищаем input, но не добавляем сообщение пользователя
            uiState = uiState.copy(inputText = "")
            return
        }

        // Если ожидается выбор файла для анализа, но команда не /analysis, сбрасываем флаг
        if (uiState.pendingAnalysisFile) {
            uiState = uiState.copy(pendingAnalysisFile = false)
        }

        // Clear input immediately
        val userMessage = Message(
            content = MessageContent.Text(currentInput),
            role = MessageRole.USER
        )
        uiState = uiState.copy(
            messages = uiState.messages + userMessage,
            inputText = "",
            isLoading = true
        )

        // Request AI response
        viewmodelScope.launch {
            try {
                val aiResponse = repository.sendMessage(currentInput)

                if (aiResponse != null) {
                    val assistantMessage = when (val textContent = aiResponse.text) {
                        is AiMessageContent.Json -> {
                            val jsonResponse = textContent.value
                            val links = jsonResponse.resource
                                .map { it.link }
                                .filter { it.isNotBlank() && it != "отсутствуют" }

                            Message(
                                content = MessageContent.Structured(
                                    title = jsonResponse.title,
                                    text = jsonResponse.text,
                                    links = links
                                ),
                                role = MessageRole.ASSISTANT,
                                requestTime = aiResponse.requestTime,
                                promptTokens = aiResponse.promptTokens,
                                completionTokens = aiResponse.completionTokens,
                                totalTokens = aiResponse.totalTokens
                            )
                        }

                        is AiMessageContent.Text -> {
                            Message(
                                content = MessageContent.Text(textContent.value),
                                role = MessageRole.ASSISTANT,
                                requestTime = aiResponse.requestTime,
                                promptTokens = aiResponse.promptTokens,
                                completionTokens = aiResponse.completionTokens,
                                totalTokens = aiResponse.totalTokens
                            )
                        }
                    }

                    val hasContent = when (assistantMessage.content) {
                        is MessageContent.Text -> assistantMessage.content.value.isNotBlank()
                        is MessageContent.Structured -> assistantMessage.content.text.isNotBlank() || assistantMessage.content.title.isNotBlank()
                    }

                    uiState = if (hasContent) {
                        uiState.copy(
                            messages = uiState.messages + assistantMessage,
                            isLoading = false
                        )
                    } else {
                        uiState.copy(isLoading = false)
                    }
                } else {
                    uiState = uiState.copy(isLoading = false)
                }
            } catch (e: Exception) {
                // On error, add error message
                val errorMessage = Message(
                    content = MessageContent.Text("Ошибка: ${e.message ?: "Не удалось получить ответ"}"),
                    role = MessageRole.ASSISTANT
                )
                uiState = uiState.copy(
                    messages = uiState.messages + errorMessage,
                    isLoading = false
                )
            }
        }
    }

    private fun selectFileForAnalysis() {
        viewmodelScope.launch {
            try {
                val currentInput = uiState.inputText.trim()
                
                // Добавляем сообщение пользователя
                val userMessage = Message(
                    content = MessageContent.Text(currentInput),
                    role = MessageRole.USER
                )
                uiState = uiState.copy(
                    messages = uiState.messages + userMessage,
                    inputText = "",
                    pendingAnalysisFile = true,
                    isLoading = true
                )

                // Открываем FilePicker
                val fileResult = filePicker.pickFile()
                
                if (fileResult != null) {
                    // Проверяем, что файл JSON
                    if (!fileResult.fileName.endsWith(".json", ignoreCase = true)) {
                        val errorMessage = Message(
                            content = MessageContent.Text("Ошибка: Выбранный файл не является JSON файлом. Пожалуйста, выберите файл с расширением .json"),
                            role = MessageRole.ASSISTANT
                        )
                        uiState = uiState.copy(
                            messages = uiState.messages + errorMessage,
                            isLoading = false,
                            pendingAnalysisFile = false
                        )
                        return@launch
                    }

                    // Отправляем данные файла в репозиторий для анализа
                    // Передаем команду вместе с содержимым файла, чтобы команда была распознана
                    val messageWithCommand = "${ChatCommand.ANALYSIS.command}\n${fileResult.content}"
                    val aiResponse = repository.sendMessage(messageWithCommand)

                    if (aiResponse != null) {
                        val assistantMessage = when (val textContent = aiResponse.text) {
                            is AiMessageContent.Json -> {
                                val jsonResponse = textContent.value
                                val links = jsonResponse.resource
                                    .map { it.link }
                                    .filter { it.isNotBlank() && it != "отсутствуют" }

                                Message(
                                    content = MessageContent.Structured(
                                        title = jsonResponse.title,
                                        text = jsonResponse.text,
                                        links = links
                                    ),
                                    role = MessageRole.ASSISTANT,
                                    requestTime = aiResponse.requestTime,
                                    promptTokens = aiResponse.promptTokens,
                                    completionTokens = aiResponse.completionTokens,
                                    totalTokens = aiResponse.totalTokens
                                )
                            }

                            is AiMessageContent.Text -> {
                                Message(
                                    content = MessageContent.Text(textContent.value),
                                    role = MessageRole.ASSISTANT,
                                    requestTime = aiResponse.requestTime,
                                    promptTokens = aiResponse.promptTokens,
                                    completionTokens = aiResponse.completionTokens,
                                    totalTokens = aiResponse.totalTokens
                                )
                            }
                        }

                        val hasContent = when (assistantMessage.content) {
                            is MessageContent.Text -> assistantMessage.content.value.isNotBlank()
                            is MessageContent.Structured -> assistantMessage.content.text.isNotBlank() || assistantMessage.content.title.isNotBlank()
                        }

                        uiState = if (hasContent) {
                            uiState.copy(
                                messages = uiState.messages + assistantMessage,
                                isLoading = false,
                                pendingAnalysisFile = false
                            )
                        } else {
                            uiState.copy(
                                isLoading = false,
                                pendingAnalysisFile = false
                            )
                        }
                    } else {
                        uiState = uiState.copy(
                            isLoading = false,
                            pendingAnalysisFile = false
                        )
                    }
                } else {
                    // Файл не выбран, сбрасываем состояние
                    uiState = uiState.copy(
                        isLoading = false,
                        pendingAnalysisFile = false
                    )
                }
            } catch (e: Exception) {
                val errorMessage = Message(
                    content = MessageContent.Text("Ошибка при выборе файла: ${e.message ?: "Не удалось выбрать файл"}"),
                    role = MessageRole.ASSISTANT
                )
                uiState = uiState.copy(
                    messages = uiState.messages + errorMessage,
                    isLoading = false,
                    pendingAnalysisFile = false
                )
            }
        }
    }

    fun openSettings() {
        uiState = uiState.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        uiState = uiState.copy(isSettingsOpen = false)
    }

    fun openDollarRate() {
        uiState = uiState.copy(isDollarRateOpen = true)
    }

    fun closeDollarRate() {
        uiState = uiState.copy(isDollarRateOpen = false)
    }

    fun openEmbeddings() {
        uiState = uiState.copy(isEmbeddingsOpen = true)
    }

    fun closeEmbeddings() {
        uiState = uiState.copy(isEmbeddingsOpen = false)
    }

    /**
     * Проверяет размер контекстного окна модели Ollama
     * Отправляет результат в чат
     */
    fun checkModelContextWindow() {
        viewmodelScope.launch {
            try {
                if (!apiConfig.useLocalModel || ollamaApiService == null) {
                    val errorMessage = Message(
                        content = MessageContent.Text(
                            "Локальная модель не настроена. Проверьте настройки приложения."
                        ),
                        role = MessageRole.ASSISTANT
                    )
                    uiState = uiState.copy(
                        messages = uiState.messages + errorMessage
                    )
                    return@launch
                }

                val modelName = apiConfig.localModelName
                if (modelName.isBlank()) {
                    val errorMessage = Message(
                        content = MessageContent.Text(
                            "Имя модели не указано в настройках."
                        ),
                        role = MessageRole.ASSISTANT
                    )
                    uiState = uiState.copy(
                        messages = uiState.messages + errorMessage
                    )
                    return@launch
                }

                val modelInfo = ollamaApiService.getModelInfo(modelName)

                val responseText = if (modelInfo != null && modelInfo.contextSize != null) {
                    val contextSize = modelInfo.contextSize
                    val parameterSize = modelInfo.parameterSize ?: "не указан"
                    val quantization = modelInfo.quantizationLevel ?: "не указан"
                    
                    """
                    **Информация о модели: $modelName**
                    
                    📊 **Размер контекстного окна:** $contextSize токенов
                    🔢 **Параметры:** $parameterSize
                    ⚙️ **Квантование:** $quantization
                    
                    ${if (contextSize < 4000) "⚠️ **Внимание:** Размер контекстного окна меньше 4000 токенов. Это может ограничивать возможности модели при работе с длинными текстами." else ""}
                    """.trimIndent()
                } else {
                    """
                    **Информация о модели: $modelName**
                    
                    ❌ Не удалось получить информацию о размере контекстного окна.
                    
                    **Способы проверки на VPS:**
                    
                    1. **Через команду Ollama CLI:**
                       ```bash
                       ollama show $modelName
                       ```
                       Ищите поле `context_size` в выводе.
                    
                    2. **Через API запрос:**
                       ```bash
                       curl http://localhost:11434/api/show?name=$modelName
                       ```
                       Ищите `context_size` в поле `details`.
                    
                    3. **Проверка через переменную окружения:**
                       Если установлена переменная `OLLAMA_CONTEXT_LENGTH`, она может ограничивать контекстное окно.
                    """.trimIndent()
                }

                val assistantMessage = Message(
                    content = MessageContent.Text(responseText),
                    role = MessageRole.ASSISTANT
                )
                uiState = uiState.copy(
                    messages = uiState.messages + assistantMessage
                )
            } catch (e: Exception) {
                val errorMessage = Message(
                    content = MessageContent.Text(
                        "Ошибка при проверке контекстного окна: ${e.message ?: "Неизвестная ошибка"}\n\n" +
                        "**Способы проверки на VPS:**\n\n" +
                        "1. Через команду: `ollama show ${apiConfig.localModelName}`\n" +
                        "2. Через API: `curl http://localhost:11434/api/show?name=${apiConfig.localModelName}`"
                    ),
                    role = MessageRole.ASSISTANT
                )
                uiState = uiState.copy(
                    messages = uiState.messages + errorMessage
                )
            }
        }
    }
}

