package ru.mirtomsk.shared.chat.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.chat.repository.model.AiResponse
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.format.ResponseFormat

/**
 * Implementation of ChatRepository using ChatApiService
 */
class ChatRepositoryImpl(
    private val chatApiService: ChatApiService,
    private val ioDispatcher: CoroutineDispatcher,
) : ChatRepository {

    override suspend fun sendMessage(text: String, format: ResponseFormat): AiResponse {
        return withContext(ioDispatcher) { 
            chatApiService.requestModel(text, format) 
        }
    }
}

