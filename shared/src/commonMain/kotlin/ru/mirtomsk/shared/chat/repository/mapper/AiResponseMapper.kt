package ru.mirtomsk.shared.chat.repository.mapper

import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.AiResponse
import ru.mirtomsk.shared.chat.repository.model.JsonResponse
import ru.mirtomsk.shared.network.format.ResponseFormat

/**
 * Mapper for converting API response body to AiResponse
 * Handles NDJSON parsing and format-specific transformations
 */
class AiResponseMapper(
    private val json: Json
) {

    /**
     * Parse response body (NDJSON format) into AiResponse
     * Takes the last FINAL response from the stream
     * For JSON format, automatically parses JSON content from text field
     */
    fun mapResponseBody(
        responseBody: String,
        format: ResponseFormat
    ): AiResponse {
        val lines = responseBody.lines().filter { it.isNotBlank() }

        var finalResponse: AiResponse? = null

        lines.forEach { line ->
            try {
                val aiResponse = json.decodeFromString<AiResponse>(line)
                // Keep the last response (should be FINAL status)
                finalResponse = aiResponse
            } catch (e: Exception) {
                // Log error but continue processing other lines
                println("Error deserializing line: $line, error: ${e.message}")
            }
        }

        val response = finalResponse ?: throw IllegalStateException("No valid response received")

        // Parse JSON format if needed
        if (format == ResponseFormat.JSON) {
            val alternatives = response.result.alternatives.map { alternative ->
                val message = alternative.message
                val jsonContent = when (val textContent = message.text) {
                    is AiMessage.MessageContent.Json -> textContent // Already parsed
                    is AiMessage.MessageContent.Text -> {
                        // Extract and clean JSON from text (may contain markdown code blocks)
                        val cleanedJson = extractJsonFromText(textContent.value)
                        val jsonResponse = json.decodeFromString<JsonResponse>(cleanedJson)
                        AiMessage.MessageContent.Json(jsonResponse)
                    }
                }

                alternative.copy(
                    message = message.copy(text = jsonContent)
                )
            }

            return response.copy(
                result = response.result.copy(alternatives = alternatives)
            )
        }

        return response
    }

    /**
     * Extract JSON from text that may contain markdown code blocks or escaped sequences
     * Handles formats like:
     * - ```json {...} ```
     * - ``` {...} ```
     * - Plain JSON with escaped quotes and newlines
     */
    private fun extractJsonFromText(text: String): String {
        var cleaned = text.trim()

        // Remove markdown code blocks (```json ... ``` or ``` ... ```)
        val codeBlockRegex = Regex("```(?:json)?\\s*(.*?)\\s*```", RegexOption.DOT_MATCHES_ALL)
        val match = codeBlockRegex.find(cleaned)
        if (match != null) {
            cleaned = match.groupValues[1].trim()
        }

        // Unescape common escape sequences
        // Replace \" with " (unescape quotes)
        cleaned = cleaned.replace("\\\"", "\"")

        // Handle case where the entire text might be wrapped in quotes
        // Remove leading/trailing quotes if present
        cleaned = cleaned.removeSurrounding("\"")

        return cleaned.trim()
    }
}

