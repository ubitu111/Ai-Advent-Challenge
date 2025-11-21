package ru.mirtomsk.shared.chat.model

import kotlinx.serialization.Serializable

/**
 * Message content - either plain text or structured content
 */
@Serializable
sealed class MessageContent {
    @Serializable
    data class Text(val value: String) : MessageContent()

    @Serializable
    data class Structured(
        val title: String,
        val text: String,
        val links: List<String>
    ) : MessageContent()
}

@Serializable
data class Message(
    val content: MessageContent,
    val timestamp: Long = System.currentTimeMillis(),
    val role: MessageRole = MessageRole.USER,
    val requestTime: Long = 0,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
) {
    enum class MessageRole {
        USER,
        ASSISTANT
    }
}

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isDollarRateOpen: Boolean = false
)

