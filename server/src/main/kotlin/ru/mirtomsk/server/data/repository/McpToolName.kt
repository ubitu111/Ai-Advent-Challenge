package ru.mirtomsk.server.data.repository

import ru.mirtomsk.server.domain.model.McpTool
import ru.mirtomsk.server.domain.model.McpToolInputSchema
import ru.mirtomsk.server.domain.model.McpToolProperty

/**
 * Enum for MCP tool names
 * Centralizes tool definitions for easier maintenance
 */
enum class McpToolName(
    val description: String,
    val inputSchema: McpToolInputSchema
) {
//    GET_WEATHER(
//        description = "Получить текущую погоду для указанных координат",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = mapOf(
//                McpToolArgument.LATITUDE.key() to McpToolProperty(
//                    type = "number",
//                    description = "Широта в градусах"
//                ),
//                McpToolArgument.LONGITUDE.key() to McpToolProperty(
//                    type = "number",
//                    description = "Долгота в градусах"
//                )
//            ),
//            required = listOf(McpToolArgument.LATITUDE.key(), McpToolArgument.LONGITUDE.key())
//        )
//    ),
//    GET_CURRENT_WEATHER(
//        description = "Получить текущую погоду для указанных координат (детальная информация)",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = mapOf(
//                McpToolArgument.LATITUDE.key() to McpToolProperty(
//                    type = "number",
//                    description = "Широта в градусах"
//                ),
//                McpToolArgument.LONGITUDE.key() to McpToolProperty(
//                    type = "number",
//                    description = "Долгота в градусах"
//                )
//            ),
//            required = listOf(McpToolArgument.LATITUDE.key(), McpToolArgument.LONGITUDE.key())
//        )
//    ),
//    GET_HOURLY_FORECAST(
//        description = "Получить почасовой прогноз погоды для указанных координат",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = mapOf(
//                McpToolArgument.LATITUDE.key() to McpToolProperty(
//                    type = "number",
//                    description = "Широта в градусах"
//                ),
//                McpToolArgument.LONGITUDE.key() to McpToolProperty(
//                    type = "number",
//                    description = "Долгота в градусах"
//                ),
//                McpToolArgument.HOURS.key() to McpToolProperty(
//                    type = "number",
//                    description = "Количество часов для прогноза (по умолчанию 24, максимум 168)"
//                )
//            ),
//            required = listOf(McpToolArgument.LATITUDE.key(), McpToolArgument.LONGITUDE.key())
//        )
//    ),
//    GET_WEATHER_BY_DATE(
//        description = "Получить прогноз погоды на конкретную дату для указанных координат (до 16 дней вперед)",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = mapOf(
//                McpToolArgument.LATITUDE.key() to McpToolProperty(
//                    type = "number",
//                    description = "Широта в градусах"
//                ),
//                McpToolArgument.LONGITUDE.key() to McpToolProperty(
//                    type = "number",
//                    description = "Долгота в градусах"
//                ),
//                McpToolArgument.DATE.key() to McpToolProperty(
//                    type = "string",
//                    description = "Дата в формате YYYY-MM-DD (например: 2024-12-25)"
//                )
//            ),
//            required = listOf(McpToolArgument.LATITUDE.key(), McpToolArgument.LONGITUDE.key(), McpToolArgument.DATE.key())
//        )
//    ),
//    CALCULATE(
//        description = "Выполнить математические вычисления",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = mapOf(
//                McpToolArgument.EXPRESSION.key() to McpToolProperty(
//                    type = "string",
//                    description = "Математическое выражение для вычисления"
//                )
//            ),
//            required = listOf(McpToolArgument.EXPRESSION.key())
//        )
//    ),
//    GET_CITY_COORDINATES(
//        description = "Получить координаты (широту и долготу) для указанного города",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = mapOf(
//                McpToolArgument.CITY.key() to McpToolProperty(
//                    type = "string",
//                    description = "Название города"
//                )
//            ),
//            required = listOf(McpToolArgument.CITY.key())
//        )
//    ),
    GET_CURRENT_DATETIME(
        description = "Получить текущую дату и время в различных форматах (дата, время, ISO формат, день недели, день года, неделя года)",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = emptyMap(),
            required = emptyList()
        )
    ),

    //    GET_CURRENCY_RATE(
//        description = "Получить текущий курс обмена валют",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = mapOf(
//                McpToolArgument.BASE_CURRENCY.key() to McpToolProperty(
//                    type = "string",
//                    description = "Базовая валюта (например: USD, EUR, RUB)"
//                ),
//                McpToolArgument.TARGET_CURRENCY.key() to McpToolProperty(
//                    type = "string",
//                    description = "Целевая валюта (например: USD, EUR, RUB)"
//                )
//            ),
//            required = listOf(McpToolArgument.BASE_CURRENCY.key(), McpToolArgument.TARGET_CURRENCY.key())
//        )
//    ),
//    GET_CURRENCY_RATE_HISTORICAL(
//        description = "Получить курс обмена валют за определенный прошлый день",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = mapOf(
//                McpToolArgument.BASE_CURRENCY.key() to McpToolProperty(
//                    type = "string",
//                    description = "Базовая валюта (например: USD, EUR, RUB)"
//                ),
//                McpToolArgument.TARGET_CURRENCY.key() to McpToolProperty(
//                    type = "string",
//                    description = "Целевая валюта (например: USD, EUR, RUB)"
//                ),
//                McpToolArgument.DATE.key() to McpToolProperty(
//                    type = "string",
//                    description = "Дата в формате YYYY-MM-DD (например: 2024-01-15)"
//                )
//            ),
//            required = listOf(McpToolArgument.BASE_CURRENCY.key(), McpToolArgument.TARGET_CURRENCY.key(), McpToolArgument.DATE.key())
//        )
//    ),
//    GIT_STATUS(
//        description = "Получить статус репозитория GitHub (информация о репозитории, последний коммит, ветка по умолчанию)",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = emptyMap(),
//            required = emptyList()
//        )
//    ),
//    GIT_LOG(
//        description = "Получить историю коммитов из GitHub репозитория (аналог git log)",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = mapOf(
//                McpToolArgument.LIMIT.key() to McpToolProperty(
//                    type = "number",
//                    description = "Максимальное количество коммитов для отображения (по умолчанию: 30, максимум: 100)"
//                ),
//                McpToolArgument.BRANCH.key() to McpToolProperty(
//                    type = "string",
//                    description = "Имя ветки (опционально, по умолчанию используется ветка по умолчанию репозитория)"
//                )
//            ),
//            required = emptyList()
//        )
//    ),
//    GIT_BRANCH(
//        description = "Получить список веток GitHub репозитория (аналог git branch)",
//        inputSchema = McpToolInputSchema(
//            type = "object",
//            properties = emptyMap(),
//            required = emptyList()
//        )
//    ),
    GIT_STATUS_LOCAL(
        description = "Получить статус локального Git репозитория (аналог git status)",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = emptyMap(),
            required = emptyList()
        )
    ),
    GIT_LOG_LOCAL(
        description = "Получить историю коммитов локального Git репозитория (аналог git log)",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.LIMIT.key() to McpToolProperty(
                    type = "number",
                    description = "Максимальное количество коммитов для отображения (по умолчанию: 30, максимум: 1000)"
                ),
                McpToolArgument.BRANCH.key() to McpToolProperty(
                    type = "string",
                    description = "Имя ветки (опционально, по умолчанию используется текущая ветка)"
                )
            ),
            required = emptyList()
        )
    ),
    GIT_BRANCH_LOCAL(
        description = "Получить список веток локального Git репозитория (аналог git branch)",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = emptyMap(),
            required = emptyList()
        )
    ),
    GIT_DIFF_LOCAL(
        description = "Получить diff измененных файлов из локального Git репозитория (аналог git diff)",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.FILE_PATH.key() to McpToolProperty(
                    type = "string",
                    description = "Путь к конкретному файлу (опционально, если не указан, возвращается diff всех измененных файлов)"
                ),
                McpToolArgument.STAGED.key() to McpToolProperty(
                    type = "boolean",
                    description = "Если true, возвращает diff для файлов в staging area (git diff --cached), иначе для неиндексированных изменений (по умолчанию: false)"
                )
            ),
            required = emptyList()
        )
    ),
    READ_TICKETS(
        description = "Получить список всех созданных тикетов из CRM системы",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = emptyMap(),
            required = emptyList()
        )
    ),
    CREATE_TICKET(
        description = "Создать новый тикет в CRM системе",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.USERNAME.key() to McpToolProperty(
                    type = "string",
                    description = "Имя пользователя, создающего тикет"
                ),
                McpToolArgument.TITLE.key() to McpToolProperty(
                    type = "string",
                    description = "Заголовок тикета"
                ),
                McpToolArgument.QUESTION.key() to McpToolProperty(
                    type = "string",
                    description = "Вопрос или описание проблемы в тикете"
                ),
                McpToolArgument.ANSWER.key() to McpToolProperty(
                    type = "string",
                    description = "Ответ на тикет (опционально, может быть заполнен позже)"
                ),
                McpToolArgument.DATE.key() to McpToolProperty(
                    type = "string",
                    description = "Дата создания тикета в формате YYYY-MM-DD (опционально, по умолчанию используется текущая дата)"
                )
            ),
            required = listOf(
                McpToolArgument.USERNAME.key(),
                McpToolArgument.TITLE.key(),
                McpToolArgument.QUESTION.key()
            )
        )
    ),
    CREATE_TASK(
        description = "Создать новую задачу",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.TASK_NAME.key() to McpToolProperty(
                    type = "string",
                    description = "Название задачи"
                ),
                McpToolArgument.TASK_DESCRIPTION.key() to McpToolProperty(
                    type = "string",
                    description = "Описание задачи"
                ),
                McpToolArgument.PRIORITY.key() to McpToolProperty(
                    type = "string",
                    description = "Приоритет задачи. Возможные значения: LOW, MEDIUM, HIGH"
                ),
                McpToolArgument.STATUS.key() to McpToolProperty(
                    type = "string",
                    description = "Статус задачи. Возможные значения: NEW, IN_PROGRESS, COMPLETED (по умолчанию: NEW, если не указан)"
                )
            ),
            required = listOf(
                McpToolArgument.TASK_NAME.key(),
                McpToolArgument.TASK_DESCRIPTION.key(),
                McpToolArgument.PRIORITY.key()
            )
        )
    ),
    GET_ALL_TASKS(
        description = "Получить список всех созданных задач",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = emptyMap(),
            required = emptyList()
        )
    ),
    GET_TASKS_BY_PRIORITY_AND_STATUS(
        description = "Получить список задач по указанному приоритету и статусу",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.PRIORITY.key() to McpToolProperty(
                    type = "string",
                    description = "Приоритет задачи. Возможные значения: LOW, MEDIUM, HIGH (опционально, если не указан, фильтрация по приоритету не применяется)"
                ),
                McpToolArgument.STATUS.key() to McpToolProperty(
                    type = "string",
                    description = "Статус задачи. Возможные значения: NEW, IN_PROGRESS, COMPLETED (опционально, если не указан, фильтрация по статусу не применяется)"
                )
            ),
            required = emptyList()
        )
    ),
    BUILD_RELEASE_APK(
        description = "Собрать подписанный релизный APK из проекта Git репозитория, используя Gradle команды. Собранный APK будет сохранен в папку builds с датой и временем в названии",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = emptyMap(),
            required = emptyList()
        )
    ),
    UPLOAD_APK_TO_YANDEX_DISK(
        description = "Загрузить APK файл на Яндекс Диск и получить публичную ссылку",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.APK_PATH.key() to McpToolProperty(
                    type = "string",
                    description = "Путь к APK файлу для загрузки"
                )
            ),
            required = listOf(McpToolArgument.APK_PATH.key())
        )
    );

    /**
     * Convert enum to McpTool domain model
     */
    fun toMcpTool(): McpTool {
        return McpTool(
            name = name.lowercase(),
            description = description,
            inputSchema = inputSchema
        )
    }

    companion object {
        /**
         * Get all available tools as domain models
         */
        fun getAllTools(): List<McpTool> {
            return entries.map { it.toMcpTool() }
        }

        /**
         * Find tool by name (case-insensitive)
         */
        fun fromName(name: String): McpToolName? {
            return entries.find { it.name.lowercase() == name.lowercase() }
        }
    }
}

