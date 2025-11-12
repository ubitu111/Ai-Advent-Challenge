package ru.mirtomsk.shared.chat.repository.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromString
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.parseToJsonElement

/**
 * Mapper for converting HuggingFace API response to generated text
 * Handles parsing of HuggingFace Inference API responses
 */
class HuggingFaceResponseMapper(
    private val json: Json
) {

    /**
     * Parse HuggingFace API response body and extract generated text
     * Handles both array and object response formats
     * 
     * @param responseBody Raw response body from HuggingFace API
     * @return Generated text extracted from response
     * @throws Exception if response contains error or cannot be parsed
     */
    fun mapResponseBody(responseBody: String): String {
        if (responseBody.isBlank()) {
            throw Exception("Empty response from HuggingFace API")
        }

        try {
            val jsonElement = json.parseToJsonElement(responseBody)

            // Проверяем на ошибку
            if (jsonElement is JsonObject) {
                val error = jsonElement["error"]?.jsonPrimitive?.content
                if (error != null) {
                    throw Exception("HuggingFace API error: $error")
                }

                // Одиночный ответ (объект)
                val generatedText = jsonElement["generated_text"]?.jsonPrimitive?.content
                if (generatedText != null) {
                    return generatedText
                }
            }

            // Пытаемся распарсить как массив
            val array = json.decodeFromString<List<JsonObject>>(responseBody)
            if (array.isNotEmpty()) {
                // Берем первый элемент (обычно HuggingFace возвращает один элемент)
                val firstItem = array.first()
                val generatedText = firstItem["generated_text"]?.jsonPrimitive?.content
                if (generatedText != null) {
                    return generatedText
                } else {
                    throw Exception("No generated_text field in response")
                }
            } else {
                throw Exception("Empty response array")
            }
        } catch (e: Exception) {
            // Если это уже наша ошибка, пробрасываем дальше
            if (e.message?.startsWith("HuggingFace API error") == true ||
                e.message?.startsWith("No generated_text") == true ||
                e.message?.startsWith("Empty response") == true
            ) {
                throw e
            }
            // Если парсинг не удался, выбрасываем ошибку с информацией
            throw Exception("Failed to parse HuggingFace response: ${e.message}", e)
        }
    }
}

