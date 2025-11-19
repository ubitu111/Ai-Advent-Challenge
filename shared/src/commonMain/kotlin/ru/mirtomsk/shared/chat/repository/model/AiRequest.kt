package ru.mirtomsk.shared.chat.repository.model

import kotlinx.serialization.Serializable

@Serializable
data class AiRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>,
    val tools: List<Tool>? = null,
) {
    @Serializable
    data class CompletionOptions(
        val stream: Boolean,
        val temperature: Float,
        val maxTokens: Int,
    )

    @Serializable
    data class Message(
        val role: MessageRoleDto,
        val text: String,
    )

    @Serializable
    data class Tool(
        val type: String = "function",
        val function: ToolFunction,
    )
    
    @Serializable
    data class ToolFunction(
        val name: String,
        val description: String? = null,
        val parameters: ToolParameters? = null,
    )

    @Serializable
    data class ToolParameters(
        val type: String = "object",
        val properties: Map<String, ToolProperty>? = null,
        val required: List<String>? = null,
    )

    @Serializable
    data class ToolProperty(
        val type: String? = null,
        val description: String? = null,
    )
}
