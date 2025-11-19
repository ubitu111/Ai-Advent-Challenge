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
    GET_WEATHER(
        description = "Получить текущую погоду для указанного города",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.CITY.key() to McpToolProperty(
                    type = "string",
                    description = "Название города"
                )
            ),
            required = listOf(McpToolArgument.CITY.key())
        )
    ),
    GET_CURRENT_WEATHER(
        description = "Получить текущую погоду для указанного города (детальная информация)",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.CITY.key() to McpToolProperty(
                    type = "string",
                    description = "Название города"
                )
            ),
            required = listOf(McpToolArgument.CITY.key())
        )
    ),
    GET_HOURLY_FORECAST(
        description = "Получить почасовой прогноз погоды для указанного города",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.CITY.key() to McpToolProperty(
                    type = "string",
                    description = "Название города"
                ),
                McpToolArgument.HOURS.key() to McpToolProperty(
                    type = "number",
                    description = "Количество часов для прогноза (по умолчанию 24, максимум 168)"
                )
            ),
            required = listOf(McpToolArgument.CITY.key())
        )
    ),
    CALCULATE(
        description = "Выполнить математические вычисления",
        inputSchema = McpToolInputSchema(
            type = "object",
            properties = mapOf(
                McpToolArgument.EXPRESSION.key() to McpToolProperty(
                    type = "string",
                    description = "Математическое выражение для вычисления"
                )
            ),
            required = listOf(McpToolArgument.EXPRESSION.key())
        )
    ),
    GET_TIME(
        description = "Получить текущее время",
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

