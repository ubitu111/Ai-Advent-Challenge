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
import ru.mirtomsk.shared.chat.repository.ChatRepository
import ru.mirtomsk.shared.chat.repository.model.AiMessage.MessageContent as AiMessageContent

class ChatViewModel(
    private val repository: ChatRepository,
    mainDispatcher: CoroutineDispatcher,
) {

    private val viewmodelScope = CoroutineScope(mainDispatcher + SupervisorJob())
    var uiState by mutableStateOf(ChatUiState())
        private set

    fun updateInputText(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun sendMessage() {
        val currentInput = uiState.inputText.trim()
        if (currentInput.isBlank()) return

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

    fun openSettings() {
        uiState = uiState.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        uiState = uiState.copy(isSettingsOpen = false)
    }
}

