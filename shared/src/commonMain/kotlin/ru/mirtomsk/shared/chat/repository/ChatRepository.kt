package ru.mirtomsk.shared.chat.repository

import ru.mirtomsk.shared.chat.repository.model.AiMessage

/**
 * Repository interface for chat operations
 */
interface ChatRepository {
    suspend fun sendMessage(text: String): AiMessage?
}

