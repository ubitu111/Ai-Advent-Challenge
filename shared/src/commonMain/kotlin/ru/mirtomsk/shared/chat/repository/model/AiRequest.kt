package ru.mirtomsk.shared.chat.repository.model

import kotlinx.serialization.Serializable

@Serializable
data class AiRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>,
) {
    @Serializable
    data class CompletionOptions(
        val stream: Boolean,
        val temperature: Float,
        val maxTokens: Int,
    )

    @Serializable
    data class Message(
        val role: String,
        val text: String,
    )
}
