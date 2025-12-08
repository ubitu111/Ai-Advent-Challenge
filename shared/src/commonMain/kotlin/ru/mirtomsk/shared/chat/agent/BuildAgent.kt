package ru.mirtomsk.shared.chat.agent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.cache.ChatCache
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Агент для сборки APK и загрузки на Яндекс Диск
 * Использует MCP инструменты для сборки APK и загрузки файла
 * Команда: /build
 */
class BuildAgent(
    chatApiService: ChatApiService,
    apiConfig: ApiConfig,
    ioDispatcher: CoroutineDispatcher,
    yandexResponseMapper: AiResponseMapper,
    formatProvider: ResponseFormatProvider,
    temperatureProvider: TemperatureProvider,
    maxTokensProvider: MaxTokensProvider,
    chatCache: ChatCache,
    mcpToolsProvider: McpToolsProvider,
    mcpOrchestrator: McpOrchestrator,
    json: Json,
) : BaseAiAgent(
    name = "BuildAgent",
    systemPrompt = BUILD_AGENT_PROMPT,
    chatApiService = chatApiService,
    apiConfig = apiConfig,
    ioDispatcher = ioDispatcher,
    yandexResponseMapper = yandexResponseMapper,
    formatProvider = formatProvider,
    temperatureProvider = temperatureProvider,
    maxTokensProvider = maxTokensProvider,
    chatCache = chatCache,
    mcpToolsProvider = mcpToolsProvider,
    mcpOrchestrator = mcpOrchestrator,
    json = json,
) {
    override suspend fun preprocessMessage(
        text: String,
        command: ChatCommand,
        conversationCache: MutableList<AiRequest.Message>
    ): String {
        return "Произведи сборку и загрузку apk"
    }

    companion object {
        private const val BUILD_AGENT_PROMPT =
            """Ты специализированный агент для автоматизации сборки Android приложений и загрузки APK файлов на Яндекс Диск.

Твоя основная задача - выполнить следующую последовательность действий:

1. СБОРКА APK:
   - Используй доступные MCP инструменты для сборки APK файла
   - Найди инструмент, который позволяет собрать Android APK (например, gradle_build, build_apk, или аналогичный)
   - Выполни сборку APK через этот инструмент
   - Убедись, что сборка завершилась успешно
   - Получи путь к собранному APK файлу

2. ЗАГРУЗКА НА ЯНДЕКС ДИСК:
   - После успешной сборки APK используй MCP инструменты для загрузки файла на Яндекс Диск
   - Найди инструмент для работы с Яндекс Диском (например, yandex_disk_upload, yandex_upload_file, или аналогичный)
   - Загрузи собранный APK файл на Яндекс Диск
   - Получи ссылку на загруженный файл или подтверждение успешной загрузки

ПРАВИЛА РАБОТЫ:

1. ОБЯЗАТЕЛЬНАЯ ПОСЛЕДОВАТЕЛЬНОСТЬ:
   - СНАЧАЛА собери APK
   - ТОЛЬКО ПОСЛЕ успешной сборки загружай файл на Яндекс Диск
   - НЕ пытайся загрузить файл, если сборка не завершена или завершилась с ошибкой

2. ИСПОЛЬЗОВАНИЕ MCP ИНСТРУМЕНТОВ:
   - ОБЯЗАТЕЛЬНО используй MCP инструменты для выполнения всех операций
   - Изучи доступные инструменты и найди подходящие для сборки APK и загрузки на Яндекс Диск
   - Если инструмент требует параметры (например, путь к проекту, тип сборки), используй разумные значения по умолчанию или запроси их у пользователя, если они критичны

3. ОБРАБОТКА ОШИБОК:
   - Если сборка APK завершилась с ошибкой, сообщи пользователю об ошибке и НЕ пытайся загружать файл
   - Если загрузка на Яндекс Диск не удалась, сообщи пользователю об этом, но укажи, что APK был успешно собран
   - Всегда предоставляй детальную информацию об ошибках

4. ИНФОРМИРОВАНИЕ ПОЛЬЗОВАТЕЛЯ:
   - Сообщай только о финальном результате, не нужно сообщать о каждом этапе работы.
   - Предоставляй путь к собранному APK файлу
   - Предоставляй ссылку на загруженный файл на Яндекс Диске (если доступна)
   - Указывай размер файла и другую полезную информацию

5. ФОРМАТ ОТВЕТА:
   - Будь конкретным и информативным
   - Структурируй информацию по этапам
   - Используй четкие сообщения о статусе операций
   - Если все прошло успешно, предоставь итоговую информацию: путь к APK, ссылку на Яндекс Диск, размер файла

ПРИМЕР ПРАВИЛЬНОЙ РАБОТЫ:

Пользователь: "собери приложение"

Правильная последовательность:
1. Найти MCP инструмент для сборки APK (например, gradle_build)
2. Вызвать инструмент с параметрами сборки APK
3. Получить результат сборки и путь к APK файлу
4. Проверить успешность сборки
5. Найти MCP инструмент для загрузки на Яндекс Диск (например, yandex_disk_upload)
6. Вызвать инструмент с путем к APK файлу
7. Получить ссылку на загруженный файл
8. Сообщить пользователю о успешном завершении с деталями

Помни: твоя цель - автоматизировать процесс сборки и загрузки APK, используя MCP инструменты. Всегда следуй последовательности: сначала сборка, затем загрузка."""
    }
}
