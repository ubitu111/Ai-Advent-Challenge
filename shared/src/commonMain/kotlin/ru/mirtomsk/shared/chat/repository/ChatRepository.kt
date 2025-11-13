package ru.mirtomsk.shared.chat.repository

import ru.mirtomsk.shared.chat.repository.model.MessageResponseDto

/**
 * Repository interface for chat operations
 */
interface ChatRepository {
    suspend fun sendMessage(text: String): MessageResponseDto?
}

