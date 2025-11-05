package ru.mirtomsk.shared.chat.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val role: MessageRole = MessageRole.USER
) {
    enum class MessageRole {
        USER,
        ASSISTANT
    }
}

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false
)

