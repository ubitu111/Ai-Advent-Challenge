package ru.mirtomsk.shared.embeddings.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.embeddings.model.EmbeddingsResult

/**
 * Реализация кеша на основе файлового хранилища
 * Использует JSON сериализацию для сохранения данных
 */
class FileEmbeddingsCache(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : EmbeddingsCache {

    internal val cacheFileName = "embeddings_cache.json"

    @kotlinx.serialization.Serializable
    private data class CacheData(
        val results: List<EmbeddingsResult>
    )

    override suspend fun saveResult(result: EmbeddingsResult) = withContext(Dispatchers.Default) {
        try {
            val existingResults = getAllResults().toMutableList()
            
            // Удаляем старый результат с таким же именем файла, если есть
            existingResults.removeAll { it.metadata.fileName == result.metadata.fileName }
            
            // Добавляем новый результат
            existingResults.add(result)
            
            val cacheData = CacheData(results = existingResults)
            val jsonContent = json.encodeToString(CacheData.serializer(), cacheData)

            val file = getCacheFile()
            writeFileContent(file, jsonContent)
        } catch (e: Exception) {
            println("Error saving embeddings result: ${e.message}")
            e.printStackTrace()
        }
    }

    override suspend fun getResult(fileName: String): EmbeddingsResult? = withContext(Dispatchers.Default) {
        try {
            val file = getCacheFile()
            if (!file.exists()) {
                return@withContext null
            }

            val content = readFileContent(file)
            if (content.isBlank()) {
                return@withContext null
            }

            val cacheData = json.decodeFromString<CacheData>(content)
            cacheData.results.firstOrNull { it.metadata.fileName == fileName }
        } catch (e: Exception) {
            println("Error reading embeddings result: ${e.message}")
            null
        }
    }

    override suspend fun getAllResults(): List<EmbeddingsResult> = withContext(Dispatchers.Default) {
        try {
            val file = getCacheFile()
            if (!file.exists()) {
                return@withContext emptyList()
            }

            val content = readFileContent(file)
            if (content.isBlank()) {
                return@withContext emptyList()
            }

            val cacheData = json.decodeFromString<CacheData>(content)
            cacheData.results
        } catch (e: Exception) {
            println("Error reading all embeddings results: ${e.message}")
            emptyList()
        }
    }

    override suspend fun deleteResult(fileName: String) = withContext(Dispatchers.Default) {
        try {
            val existingResults = getAllResults().toMutableList()
            existingResults.removeAll { it.metadata.fileName == fileName }
            
            val cacheData = CacheData(results = existingResults)
            val jsonContent = json.encodeToString(CacheData.serializer(), cacheData)

            val file = getCacheFile()
            writeFileContent(file, jsonContent)
        } catch (e: Exception) {
            println("Error deleting embeddings result: ${e.message}")
        }
    }

    /**
     * Получить файл кеша
     */
    private fun getCacheFile(): CacheFile = getCacheFile(this)
}

/**
 * Получить файл кеша (expect функция)
 */
internal expect fun getCacheFile(cache: FileEmbeddingsCache): CacheFile

/**
 * Прочитать содержимое файла (expect функция)
 */
internal expect fun readFileContent(file: CacheFile): String

/**
 * Записать содержимое в файл (expect функция)
 */
internal expect fun writeFileContent(file: CacheFile, content: String)

interface CacheFile {
    fun exists(): Boolean
}
