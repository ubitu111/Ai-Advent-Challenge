package ru.mirtomsk.shared.chat.repository.model

import ru.mirtomsk.shared.chat.repository.model.AiMessage.MessageContent

data class MessageResponseDto(
    val role: MessageRoleDto,
    val text: MessageContent,
    val requestTime: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?,
)
