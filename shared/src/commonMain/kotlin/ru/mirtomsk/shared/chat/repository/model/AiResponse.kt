package ru.mirtomsk.shared.chat.repository.model

import kotlinx.serialization.Serializable

/**
 * Response model from Yandex GPT API
 */
@Serializable
data class AiResponse(
    val result: AiResult,
)

/**
 * Result data from API response
 */
@Serializable
data class AiResult(
    val alternatives: List<AiAlternative>,
    val usage: AiUsage,
    val modelVersion: String,
)

/**
 * Alternative response option
 */
@Serializable
data class AiAlternative(
    val message: AiMessage,
    val status: String,
)

/**
 * Message content
 */
@Serializable
data class AiMessage(
    val role: MessageRoleDto,
    @Serializable(with = MessageContentSerializer::class)
    val text: MessageContent? = null,
    val toolCalls: List<AiToolCall>? = null,
    val toolCallList: ToolCallList? = null,
) {
    /**
     * Message content - either plain text or JSON structured data
     */
    @Serializable(with = MessageContentSerializer::class)
    sealed class MessageContent {
        @Serializable
        data class Text(val value: String) : MessageContent()

        @Serializable
        data class Json(val value: JsonResponse) : MessageContent()
    }
}

/**
 * Tool call list from Yandex GPT API
 */
@Serializable
data class ToolCallList(
    val toolCalls: List<AiToolCall>,
)

/**
 * Tool call from AI model
 */
@Serializable
data class AiToolCall(
    val id: String? = null,
    val type: String = "function",
    val function: AiToolCallFunction? = null,
    val functionCall: FunctionCall? = null,
)

/**
 * Tool call function details (OpenAI format)
 */
@Serializable
data class AiToolCallFunction(
    val name: String,
    val arguments: String, // JSON string with arguments
)

/**
 * Function call from Yandex GPT API
 */
@Serializable
data class FunctionCall(
    val name: String,
    val arguments: Map<String, kotlinx.serialization.json.JsonElement>, // Object with arguments
)

/**
 * Structured JSON response content
 */
@Serializable
data class JsonResponse(
    val title: String,
    val text: String,
    val resource: List<ResourceItem>,
) {
    @Serializable
    data class ResourceItem(
        val link: String,
    )
}

/**
 * Token usage information
 */
@Serializable
data class AiUsage(
    val inputTextTokens: String,
    val completionTokens: String,
    val totalTokens: String,
    val completionTokensDetails: CompletionTokensDetails
)

/**
 * Completion tokens details
 */
@Serializable
data class CompletionTokensDetails(
    val reasoningTokens: String
)

