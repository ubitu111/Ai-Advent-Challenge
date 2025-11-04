package ru.mirtomsk.shared.chat.repository.model

import kotlinx.serialization.Serializable

/**
 * Response model from Yandex GPT API
 */
@Serializable
data class AiResponse(
    val result: AiResult,
) {
    /**
     * Extract the first alternative text from response
     */
    fun getText(): String {
        return result.alternatives.firstOrNull()?.message?.text ?: ""
    }
    
    /**
     * Check if this is a final response
     */
    fun isFinal(): Boolean {
        return result.alternatives.firstOrNull()?.status == "ALTERNATIVE_STATUS_FINAL"
    }
    
    /**
     * Check if this is a partial response
     */
    fun isPartial(): Boolean {
        return result.alternatives.firstOrNull()?.status == "ALTERNATIVE_STATUS_PARTIAL"
    }
}

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
    val role: String,
    val text: String,
)

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

