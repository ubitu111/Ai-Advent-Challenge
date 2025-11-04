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
import ru.mirtomsk.shared.chat.repository.ChatRepository

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

        viewmodelScope.launch {
            try {
                val sentMessage = repository.sendMessage(currentInput)
                uiState = uiState.copy(
                    messages = uiState.messages + Message(text = sentMessage.result.alternatives[0].message.text),
                    inputText = ""
                )
            } catch (e: Exception) {
                // On error, add message locally
                val newMessage = Message(text = currentInput)
                uiState = uiState.copy(
                    messages = uiState.messages + newMessage,
                    inputText = ""
                )
            }
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(messages = emptyList())
    }
}

