package ru.mirtomsk.server.data.repository

import ru.mirtomsk.server.domain.model.McpTool
import ru.mirtomsk.server.domain.model.McpToolCall
import ru.mirtomsk.server.domain.model.McpToolCallContent
import ru.mirtomsk.server.domain.model.McpToolCallResult
import ru.mirtomsk.server.domain.model.McpToolInputSchema
import ru.mirtomsk.server.domain.model.McpToolProperty
import ru.mirtomsk.server.domain.repository.McpToolRepository

/**
 * Implementation of McpToolRepository
 * Provides in-memory storage of MCP tools and their execution logic
 * 
 * In a real-world scenario, this could be replaced with database or external service integration
 */
class McpToolRepositoryImpl : McpToolRepository {
    
    private val availableTools = listOf(
        McpTool(
            name = "get_weather",
            description = "Получить текущую погоду для указанного города",
            inputSchema = McpToolInputSchema(
                type = "object",
                properties = mapOf(
                    "city" to McpToolProperty(
                        type = "string",
                        description = "Название города"
                    )
                ),
                required = listOf("city")
            )
        ),
        McpTool(
            name = "calculate",
            description = "Выполнить математические вычисления",
            inputSchema = McpToolInputSchema(
                type = "object",
                properties = mapOf(
                    "expression" to McpToolProperty(
                        type = "string",
                        description = "Математическое выражение для вычисления"
                    )
                ),
                required = listOf("expression")
            )
        ),
        McpTool(
            name = "get_time",
            description = "Получить текущее время",
            inputSchema = McpToolInputSchema(
                type = "object",
                properties = emptyMap(),
                required = emptyList()
            )
        ),
    )
    
    override suspend fun getAllTools(): List<McpTool> {
        return availableTools
    }
    
    override suspend fun callTool(toolCall: McpToolCall): McpToolCallResult {
        return when (toolCall.toolName) {
            "get_weather" -> handleGetWeather(toolCall.arguments)
            "calculate" -> handleCalculate(toolCall.arguments)
            "get_time" -> handleGetTime()
            else -> McpToolCallResult(
                content = listOf(
                    McpToolCallContent(
                        type = "text",
                        text = "Инструмент '${toolCall.toolName}' не найден"
                    )
                ),
                isError = true
            )
        }
    }
    
    private fun handleGetWeather(arguments: Map<String, Any>): McpToolCallResult {
        val city = arguments["city"] as? String ?: "неизвестный город"
        val weather = when (city.lowercase()) {
            "москва" -> "Солнечно, +15°C"
            "санкт-петербург" -> "Облачно, +12°C"
            "новосибирск" -> "Пасмурно, +8°C"
            else -> "Погода для $city: переменная облачность, +10°C"
        }
        
        return McpToolCallResult(
            content = listOf(
                McpToolCallContent(
                    type = "text",
                    text = "Погода в $city: $weather"
                )
            )
        )
    }
    
    private fun handleCalculate(arguments: Map<String, Any>): McpToolCallResult {
        val expression = arguments["expression"] as? String
            ?: return McpToolCallResult(
                content = listOf(
                    McpToolCallContent(
                        type = "text",
                        text = "Ошибка: не указано выражение для вычисления"
                    )
                ),
                isError = true
            )
        
        return try {
            // Простой калькулятор - в реальном приложении используйте более безопасный парсер
            val result = evaluateSimpleExpression(expression)
            McpToolCallResult(
                content = listOf(
                    McpToolCallContent(
                        type = "text",
                        text = "Результат: $result"
                    )
                )
            )
        } catch (e: Exception) {
            McpToolCallResult(
                content = listOf(
                    McpToolCallContent(
                        type = "text",
                        text = "Ошибка вычисления: ${e.message}"
                    )
                ),
                isError = true
            )
        }
    }
    
    private fun handleGetTime(): McpToolCallResult {
        val currentTime = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        return McpToolCallResult(
            content = listOf(
                McpToolCallContent(
                    type = "text",
                    text = "Текущее время: $currentTime"
                )
            )
        )
    }
    
    /**
     * Simple expression evaluator
     * WARNING: This is a simplified implementation for demo purposes
     * In production, use a proper expression parser library
     */
    private fun evaluateSimpleExpression(expression: String): Double {
        // Remove whitespace
        val cleanExpr = expression.replace("\\s".toRegex(), "")
        
        // Try to evaluate as a simple arithmetic expression
        // This is a very basic implementation - only handles simple cases
        return when {
            cleanExpr.contains("+") -> {
                val parts = cleanExpr.split("+")
                parts.sumOf { it.toDouble() }
            }
            cleanExpr.contains("-") -> {
                val parts = cleanExpr.split("-")
                parts[0].toDouble() - parts.drop(1).sumOf { it.toDouble() }
            }
            cleanExpr.contains("*") -> {
                val parts = cleanExpr.split("*")
                parts.fold(1.0) { acc, part -> acc * part.toDouble() }
            }
            cleanExpr.contains("/") -> {
                val parts = cleanExpr.split("/")
                parts.drop(1).fold(parts[0].toDouble()) { acc, part -> acc / part.toDouble() }
            }
            else -> cleanExpr.toDouble()
        }
    }
}
