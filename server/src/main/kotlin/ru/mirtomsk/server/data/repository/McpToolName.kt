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
    GIT_STATUS(
        description = "Получить статус репозитория GitHub (информация о репозитории, последний коммит, ветка по умолчанию)",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = emptyMap(),
            required = emptyList()
        )
    ),
    GIT_LOG(
        description = "Получить историю коммитов из GitHub репозитория (аналог git log)",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.LIMIT.key() to McpToolProperty(
                    type = "number",
                    description = "Максимальное количество коммитов для отображения (по умолчанию: 30, максимум: 100)"
                ),
                McpToolArgument.BRANCH.key() to McpToolProperty(
                    type = "string",
                    description = "Имя ветки (опционально, по умолчанию используется ветка по умолчанию репозитория)"
                )
            ),
            required = emptyList()
        )
    ),
    GIT_BRANCH(
        description = "Получить список веток GitHub репозитория (аналог git branch)",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = emptyMap(),
            required = emptyList()
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

