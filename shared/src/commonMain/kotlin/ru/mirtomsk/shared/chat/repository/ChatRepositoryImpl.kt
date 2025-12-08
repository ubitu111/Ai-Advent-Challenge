package ru.mirtomsk.shared.chat.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.mirtomsk.shared.chat.agent.BuildAgent
import ru.mirtomsk.shared.chat.agent.ChatCommand
import ru.mirtomsk.shared.chat.agent.CodeReviewAgent
import ru.mirtomsk.shared.chat.agent.DeveloperAgent
import ru.mirtomsk.shared.chat.agent.DeveloperHelperAgent
import ru.mirtomsk.shared.chat.agent.SimpleChatAgent
import ru.mirtomsk.shared.chat.agent.SupportAgent
import ru.mirtomsk.shared.chat.context.ContextResetProvider
import ru.mirtomsk.shared.chat.repository.model.MessageResponseDto

/**
 * Реализация ChatRepository как оркестратора AI агентов
 * Выбирает подходящего агента на основе команд пользователя
 */
class ChatRepositoryImpl(
    private val ioDispatcher: CoroutineDispatcher,
    private val contextResetProvider: ContextResetProvider,
    private val simpleChatAgent: SimpleChatAgent,
    private val codeReviewAgent: CodeReviewAgent,
    private val developerHelperAgent: DeveloperHelperAgent,
    private val supportAgent: SupportAgent,
    private val developerAgent: DeveloperAgent,
    private val buildAgent: BuildAgent,
) : ChatRepository {

    private var lastResetCounter: Long = 0L

    override suspend fun sendMessage(text: String): MessageResponseDto? {
        return withContext(ioDispatcher) {
            // Проверяем, был ли сброшен контекст
            val resetCounter = contextResetProvider.resetCounter.first()
            if (resetCounter != lastResetCounter) {
                clearAllAgentsCache()
                lastResetCounter = resetCounter
            }

            // Парсим команду из текста
            val (command, messageText) = ChatCommand.parse(text)

            // Выбираем агента на основе команды
            val agent = when (command) {
                ChatCommand.REVIEW -> codeReviewAgent
                ChatCommand.HELP, ChatCommand.GIT -> developerHelperAgent
                ChatCommand.SUPPORT -> supportAgent
                ChatCommand.DEVELOP -> developerAgent
                ChatCommand.BUILD -> buildAgent
                ChatCommand.NONE -> simpleChatAgent
            }

            // Обрабатываем сообщение через выбранного агента
            agent.processMessage(messageText, command)
        }
    }

    /**
     * Очистка кеша всех агентов
     */
    private suspend fun clearAllAgentsCache() {
        simpleChatAgent.clearCache()
        codeReviewAgent.clearCache()
        developerHelperAgent.clearCache()
        supportAgent.clearCache()
        developerAgent.clearCache()
        buildAgent.clearCache()
    }
}
