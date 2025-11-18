package ru.mirtomsk.shared.chat.repository.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto

/**
 * Реализация кеша на основе файлового хранилища
 * Использует JSON сериализацию для сохранения данных
 */
class FileChatCache(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ChatCache {

    internal val cacheFileName = "chat_cache.json"

    @Serializable
    private data class CacheData(
        val messages: List<SerializableMessage>
    )

    @Serializable
    private data class SerializableMessage(
        val role: MessageRoleDto,
        val text: String
    )

    override suspend fun getMessages(): List<AiRequest.Message> = withContext(Dispatchers.Default) {
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
            cacheData.messages.map { serializableMessage ->
                AiRequest.Message(
                    role = serializableMessage.role,
                    text = serializableMessage.text
                )
            }
        } catch (e: Exception) {
            // В случае ошибки возвращаем пустой список
            emptyList()
        }
    }

    override suspend fun saveMessages(messages: List<AiRequest.Message>) =
        withContext(Dispatchers.Default) {
            try {
                val serializableMessages = messages.map { message ->
                    SerializableMessage(
                        role = message.role,
                        text = message.text
                    )
                }

                val cacheData = CacheData(messages = serializableMessages)
                val jsonContent = json.encodeToString(CacheData.serializer(), cacheData)

                val file = getCacheFile()
                writeFileContent(file, jsonContent)
            } catch (e: Exception) {
                // В случае ошибки просто игнорируем
            }
        }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        try {
            val file = getCacheFile()
            if (file.exists()) {
                deleteFile(file)
            }
        } catch (e: Exception) {
            // В случае ошибки просто игнорируем
        }
    }

    override suspend fun isEmpty(): Boolean = withContext(Dispatchers.Default) {
        try {
            val file = getCacheFile()
            if (!file.exists()) {
                return@withContext true
            }

            val content = readFileContent(file)
            if (content.isBlank()) {
                return@withContext true
            }

            val cacheData = json.decodeFromString<CacheData>(content)
            cacheData.messages.isEmpty()
        } catch (e: Exception) {
            true
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
internal expect fun getCacheFile(cache: FileChatCache): CacheFile

/**
 * Прочитать содержимое файла (expect функция)
 */
internal expect fun readFileContent(file: CacheFile): String

/**
 * Записать содержимое в файл (expect функция)
 */
internal expect fun writeFileContent(file: CacheFile, content: String)

/**
 * Удалить файл (expect функция)
 */
internal expect fun deleteFile(file: CacheFile)

interface CacheFile {
    fun exists(): Boolean
}
