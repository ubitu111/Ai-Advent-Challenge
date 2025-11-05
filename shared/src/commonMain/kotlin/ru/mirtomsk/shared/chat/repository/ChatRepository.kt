package ru.mirtomsk.shared.chat.repository

import ru.mirtomsk.shared.chat.repository.model.AiResponse

/**
 * Repository interface for chat operations
 */
interface ChatRepository {
    suspend fun sendMessage(text: String): AiResponse
}

