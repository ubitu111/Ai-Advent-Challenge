package ru.mirtomsk.shared.chat.agent

/**
 * Команды чата для выбора агента
 */
enum class ChatCommand(val command: String, val description: String) {
    REVIEW("/review", "Ревью кода на Kotlin"),
    HELP("/help", "Помощь разработчика с использованием RAG"),
    GIT("/git", "Git команды через MCP"),
    SUPPORT("/support", "Поддержка пользователей с использованием RAG и CRM"),
    DEVELOP("/develop", "Помощник команды разработчиков с RAG и MCP инструментами"),
    BUILD("/build", "Сборка APK и загрузка на Яндекс Диск"),
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
            
            if (trimmed.startsWith(SUPPORT.command)) {
                val query = trimmed.removePrefix(SUPPORT.command).trim()
                return Pair(SUPPORT, query)
            }
            
            if (trimmed.startsWith(DEVELOP.command)) {
                val query = trimmed.removePrefix(DEVELOP.command).trim()
                return Pair(DEVELOP, query)
            }
            
            if (trimmed.startsWith(BUILD.command)) {
                val query = trimmed.removePrefix(BUILD.command).trim()
                return Pair(BUILD, query)
            }
            
            return Pair(NONE, trimmed)
        }
    }
}

