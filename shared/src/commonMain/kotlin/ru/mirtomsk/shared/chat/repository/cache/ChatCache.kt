package ru.mirtomsk.shared.chat.repository.cache

import ru.mirtomsk.shared.chat.repository.model.AiRequest

/**
 * Интерфейс для кеша сообщений чата
 * Отвечает только за сохранение и получение данных
 */
interface ChatCache {
    /**
     * Получить все сообщения из кеша
     */
    suspend fun getMessages(): List<AiRequest.Message>

    /**
     * Сохранить сообщения в кеш
     */
    suspend fun saveMessages(messages: List<AiRequest.Message>)

    /**
     * Очистить кеш
     */
    suspend fun clear()

    /**
     * Проверить, пуст ли кеш
     */
    suspend fun isEmpty(): Boolean
}
