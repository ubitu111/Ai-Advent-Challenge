package ru.mirtomsk.shared.chat.repository

import ru.mirtomsk.shared.chat.repository.model.MessageResponseDto

/**
 * Repository interface for chat operations
 * Оркестрирует работу различных AI агентов на основе команд пользователя
 */
interface ChatRepository {
    suspend fun sendMessage(text: String): MessageResponseDto?
}

