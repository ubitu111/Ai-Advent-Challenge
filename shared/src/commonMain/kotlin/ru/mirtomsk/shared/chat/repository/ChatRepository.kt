package ru.mirtomsk.shared.chat.repository

import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.network.format.ResponseFormat

/**
 * Repository interface for chat operations
 */
interface ChatRepository {
    suspend fun sendMessage(text: String, format: ResponseFormat): AiMessage?
}

