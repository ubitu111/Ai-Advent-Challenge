package ru.mirtomsk.shared.chat.repository

import ru.mirtomsk.shared.chat.repository.model.AiResponse
import ru.mirtomsk.shared.network.format.ResponseFormat

/**
 * Repository interface for chat operations
 */
interface ChatRepository {
    suspend fun sendMessage(text: String, format: ResponseFormat): AiResponse
}

