package ru.mirtomsk.shared.chat.agent

/**
 * Команды чата для выбора агента
 */
enum class ChatCommand(val command: String, val description: String) {
    REVIEW("/review", "Ревью кода на Kotlin"),
    HELP("/help", "Помощь разработчика с использованием RAG"),
    GIT("/git", "Git команды через MCP"),
    NONE("", "Обычный чат (без команды)");

    companion object {
        /**
         * Парсит команду из текста сообщения
         * @param text Текст сообщения пользователя
         * @return Pair<ChatCommand, String> где первый элемент - команда, второй - текст после команды
         */
        fun parse(text: String): Pair<ChatCommand, String> {
            val trimmed = text.trim()
            
            if (trimmed.startsWith(REVIEW.command)) {
                val query = trimmed.removePrefix(REVIEW.command).trim()
                return Pair(REVIEW, query)
            }
            
            if (trimmed.startsWith(HELP.command)) {
                val query = trimmed.removePrefix(HELP.command).trim()
                return Pair(HELP, query)
            }
            
            if (trimmed.startsWith(GIT.command)) {
                val query = trimmed.removePrefix(GIT.command).trim()
                return Pair(GIT, query)
            }
            
            return Pair(NONE, trimmed)
        }
    }
}

