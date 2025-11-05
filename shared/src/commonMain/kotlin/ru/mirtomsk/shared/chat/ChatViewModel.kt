package ru.mirtomsk.shared.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.mirtomsk.shared.chat.model.ChatUiState
import ru.mirtomsk.shared.chat.model.Message
import ru.mirtomsk.shared.chat.model.Message.MessageRole
import ru.mirtomsk.shared.chat.repository.ChatRepository
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.network.format.ResponseFormatProvider

class ChatViewModel(
    private val repository: ChatRepository,
    private val formatProvider: ResponseFormatProvider,
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
        val userMessage = Message(text = currentInput, role = MessageRole.USER)
        uiState = uiState.copy(
            messages = uiState.messages + userMessage,
            inputText = "",
            isLoading = true
        )

        // Request AI response
        viewmodelScope.launch {
            try {
                // Get current format from Flow
                val format = formatProvider.responseFormat.first()
                val aiResponse = repository.sendMessage(currentInput, format)
                val assistantText = aiResponse.result.alternatives
                    .find { it.message.role == AiMessage.Role.assistant }
                    ?.message?.text.orEmpty()

                if (assistantText.isNotBlank()) {
                    val assistantMessage =
                        Message(text = assistantText, role = MessageRole.ASSISTANT)
                    uiState = uiState.copy(
                        messages = uiState.messages + assistantMessage,
                        isLoading = false
                    )
                } else {
                    uiState = uiState.copy(isLoading = false)
                }
            } catch (e: Exception) {
                // On error, add error message
                val errorMessage = Message(
                    text = "Ошибка: ${e.message ?: "Не удалось получить ответ"}",
                    role = MessageRole.ASSISTANT
                )
                uiState = uiState.copy(
                    messages = uiState.messages + errorMessage,
                    isLoading = false
                )
            }
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(messages = emptyList())
    }

    fun openSettings() {
        uiState = uiState.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        uiState = uiState.copy(isSettingsOpen = false)
    }
}

