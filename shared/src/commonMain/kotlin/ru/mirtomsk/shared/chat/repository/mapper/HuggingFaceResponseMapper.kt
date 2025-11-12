package ru.mirtomsk.shared.chat.repository.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Result of parsing HuggingFace API response
 */
data class HuggingFaceResponse(
    val content: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
)

/**
 * Mapper for converting HuggingFace Chat API response to generated text and usage
 * Handles parsing of new Chat API format with choices array and usage
 */
class HuggingFaceResponseMapper(
    private val json: Json
) {

    /**
     * Parse HuggingFace Chat API response body and extract generated text and usage
     * New format: { "choices": [{ "message": { "content": "..." } }], "usage": {...} }
     *
     * @param responseBody Raw response body from HuggingFace API
     * @return HuggingFaceResponse with content and token usage
     * @throws Exception if response contains error or cannot be parsed
     */
    fun mapResponseBody(responseBody: String): HuggingFaceResponse {
        if (responseBody.isBlank()) {
            throw Exception("Empty response from HuggingFace API")
        }

        try {
            val jsonElement = json.parseToJsonElement(responseBody)

            // Проверяем на ошибку
            if (jsonElement is JsonObject) {
                val error = jsonElement["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    ?: jsonElement["error"]?.jsonPrimitive?.content
                if (error != null) {
                    throw Exception("HuggingFace API error: $error")
                }

                // Парсим usage из ответа
                val usage = jsonElement["usage"]?.jsonObject
                val promptTokens = usage?.get("prompt_tokens")?.jsonPrimitive?.content?.toIntOrNull()
                val completionTokens = usage?.get("completion_tokens")?.jsonPrimitive?.content?.toIntOrNull()
                val totalTokens = usage?.get("total_tokens")?.jsonPrimitive?.content?.toIntOrNull()

                // Новый формат Chat API: { "choices": [{ "message": { "content": "..." } }] }
                val choices = jsonElement["choices"]?.jsonArray
                if (choices != null && choices.isNotEmpty()) {
                    val firstChoice = choices.first().jsonObject
                    val message = firstChoice["message"]?.jsonObject
                    val content = message?.get("content")?.jsonPrimitive?.content
                    if (content != null) {
                        return HuggingFaceResponse(
                            content = content,
                            promptTokens = promptTokens,
                            completionTokens = completionTokens,
                            totalTokens = totalTokens,
                        )
                    }
                }

                // Старый формат для обратной совместимости: { "generated_text": "..." }
                val generatedText = jsonElement["generated_text"]?.jsonPrimitive?.content
                if (generatedText != null) {
                    return HuggingFaceResponse(
                        content = generatedText,
                        promptTokens = promptTokens,
                        completionTokens = completionTokens,
                        totalTokens = totalTokens,
                    )
                }
            }

            // Пытаемся распарсить как массив (старый формат)
            val array = json.decodeFromString<List<JsonObject>>(responseBody)
            if (array.isNotEmpty()) {
                val firstItem = array.first()
                val generatedText = firstItem["generated_text"]?.jsonPrimitive?.content
                if (generatedText != null) {
                    return HuggingFaceResponse(
                        content = generatedText,
                    )
                }
            }

            throw Exception("No content or generated_text field in response")
        } catch (e: Exception) {
            // Если это уже наша ошибка, пробрасываем дальше
            if (e.message?.startsWith("HuggingFace API error") == true ||
                e.message?.startsWith("No content") == true ||
                e.message?.startsWith("Empty response") == true
            ) {
                throw e
            }
            // Если парсинг не удался, выбрасываем ошибку с информацией
            throw Exception("Failed to parse HuggingFace response: ${e.message}", e)
        }
    }
}

