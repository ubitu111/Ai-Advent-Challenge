package ru.mirtomsk.shared.chat.model

data class Message(
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = ""
)

