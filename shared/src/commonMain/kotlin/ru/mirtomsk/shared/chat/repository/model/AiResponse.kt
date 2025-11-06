package ru.mirtomsk.shared.chat.repository.model

import kotlinx.serialization.SerialName
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
    val role: Role,
    val text: MessageContent,
) {
    @Serializable
    enum class Role {
        @SerialName("system")
        SYSTEM,

        @SerialName("user")
        USER,

        @SerialName("assistant")
        ASSISTANT
    }

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

