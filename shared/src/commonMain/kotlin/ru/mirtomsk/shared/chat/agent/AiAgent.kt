package ru.mirtomsk.shared.chat.agent

import ru.mirtomsk.shared.chat.repository.model.MessageResponseDto

/**
 * Абстракция для AI агентов
 * Каждый агент имеет свой кеш, промпт и логику работы
 */
interface AiAgent {
    /**
     * Имя агента для идентификации
     */
    val name: String

    /**
     * Системный промпт агента
     */
    val systemPrompt: String

    /**
     * Обработка сообщения пользователя
     * @param text Текст сообщения (может быть с командой или без)
     * @param command Команда, если была указана
     * @return Ответ агента или null если обработка не удалась
     */
    suspend fun processMessage(text: String, command: ChatCommand): MessageResponseDto?

    /**
     * Очистка кеша агента
     */
    suspend fun clearCache()
}

