package ru.mirtomsk.shared.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.mirtomsk.shared.chat.model.ChatUiState
import ru.mirtomsk.shared.chat.model.Message

class ChatViewModel {
    var uiState by mutableStateOf(ChatUiState())
        private set

    fun updateInputText(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun sendMessage() {
        val currentInput = uiState.inputText.trim()
        if (currentInput.isBlank()) return

        val newMessage = Message(text = currentInput)
        uiState = uiState.copy(
            messages = uiState.messages + newMessage,
            inputText = ""
        )
    }

    fun clearMessages() {
        uiState = uiState.copy(messages = emptyList())
    }
}

