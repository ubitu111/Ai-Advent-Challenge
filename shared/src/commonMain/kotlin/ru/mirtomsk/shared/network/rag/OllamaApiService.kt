package ru.mirtomsk.shared.network.rag

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OllamaApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {

    suspend fun embed(text: String): FloatArray {
        val response: OllamaEmbeddingsResponse = httpClient.post("$baseUrl/api/embeddings") {
            contentType(ContentType.Application.Json)
            setBody(
                OllamaEmbeddingsRequest(
                    model = OllamaEmbeddingsRequest.BGE_MODEL,
                    prompt = text,
                )
            )
        }.body()
        return response.embedding.map { it.toFloat() }.toFloatArray()
    }

    /**
     * Получает информацию о модели, включая размер контекстного окна
     * @param modelName имя модели (например, "qwen2.5:0.5b")
     * @return информация о модели или null в случае ошибки
     */
    suspend fun getModelInfo(modelName: String): OllamaModelInfo? {
        return try {
            // Ollama API использует POST запрос для /api/show
            val requestBody = json.encodeToString(
                OllamaShowRequest.serializer(),
                OllamaShowRequest(model = modelName)
            )
            
            val responseText = httpClient.post("$baseUrl/api/show") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body<String>()
            
            // Парсим ответ как JSON объект
            val jsonObject = json.parseToJsonElement(responseText).jsonObject
            
            // Извлекаем details
            val details = jsonObject["details"]?.jsonObject
            
            // Пытаемся извлечь context_length из model_info
            // Для разных моделей путь может быть разным: qwen2.context_length, llama.context_length и т.д.
            val modelInfo = jsonObject["model_info"]?.jsonObject
            var contextSize: Long? = null
            
            // Проверяем различные возможные пути к context_length в model_info
            if (modelInfo != null) {
                // Для qwen2 моделей: model_info.qwen2.context_length
                val qwen2Info = modelInfo["qwen2"]?.jsonObject
                contextSize = qwen2Info?.get("context_length")?.jsonPrimitive?.content?.toLongOrNull()
                
                // Если не нашли, пробуем другие варианты
                if (contextSize == null) {
                    // Для llama моделей: model_info.llama.context_length
                    val llamaInfo = modelInfo["llama"]?.jsonObject
                    contextSize = llamaInfo?.get("context_length")?.jsonPrimitive?.content?.toLongOrNull()
                }
                
                // Пробуем найти любое поле с context_length в model_info
                if (contextSize == null) {
                    modelInfo.entries.forEach { (key, value) ->
                        if (key.contains("context", ignoreCase = true)) {
                            try {
                                val primitive = value.jsonPrimitive
                                contextSize = primitive.content.toLongOrNull()
                            } catch (e: Exception) {
                                // Не primitive, пропускаем
                            }
                        }
                        try {
                            val valueObj = value.jsonObject
                            val ctxLength = valueObj["context_length"]?.jsonPrimitive?.content?.toLongOrNull()
                            if (ctxLength != null) {
                                contextSize = ctxLength
                            }
                        } catch (e: Exception) {
                            // Не объект, пропускаем
                        }
                    }
                }
            }
            
            // Также проверяем details.context_size (если есть)
            if (contextSize == null) {
                contextSize = details?.get("context_size")?.jsonPrimitive?.content?.toLongOrNull()
            }
            
            // Также пытаемся извлечь из modelfile (может содержать num_ctx)
            val modelfile = jsonObject["modelfile"]?.jsonPrimitive?.content
            val numCtxFromModelfile = modelfile?.let { extractNumCtxFromModelfile(it) }
            
            // Также проверяем parameters (может содержать num_ctx)
            val parameters = jsonObject["parameters"]?.jsonPrimitive?.content
            val numCtxFromParameters = parameters?.let { extractNumCtxFromModelfile(it) }
            
            OllamaModelInfo(
                modelName = modelName,
                contextSize = contextSize ?: numCtxFromModelfile ?: numCtxFromParameters,
                parameterSize = details?.get("parameter_size")?.jsonPrimitive?.content,
                quantizationLevel = details?.get("quantization_level")?.jsonPrimitive?.content,
                format = details?.get("format")?.jsonPrimitive?.content,
                family = details?.get("family")?.jsonPrimitive?.content,
            )
        } catch (e: Exception) {
            println("Error getting model info: ${e.message}")
            null
        }
    }
    
    /**
     * Извлекает num_ctx из modelfile строки
     */
    private fun extractNumCtxFromModelfile(modelfile: String): Long? {
        val numCtxRegex = Regex("num_ctx\\s+(\\d+)", RegexOption.IGNORE_CASE)
        return numCtxRegex.find(modelfile)?.groupValues?.get(1)?.toLongOrNull()
    }
}

/**
 * Запрос для получения информации о модели
 */
@Serializable
data class OllamaShowRequest(
    val model: String
)

/**
 * Упрощенная модель информации о модели Ollama
 */
data class OllamaModelInfo(
    val modelName: String,
    val contextSize: Long?, // Размер контекстного окна в токенах
    val parameterSize: String? = null,
    val quantizationLevel: String? = null,
    val format: String? = null,
    val family: String? = null,
)
