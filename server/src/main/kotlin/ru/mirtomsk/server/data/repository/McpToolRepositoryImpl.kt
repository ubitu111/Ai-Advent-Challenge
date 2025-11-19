package ru.mirtomsk.server.data.repository

import ru.mirtomsk.server.domain.model.McpToolCall
import ru.mirtomsk.server.domain.model.McpToolCallContent
import ru.mirtomsk.server.domain.model.McpToolCallResult
import ru.mirtomsk.server.domain.repository.McpToolRepository
import ru.mirtomsk.server.domain.service.WeatherService

/**
 * Implementation of McpToolRepository
 * Provides in-memory storage of MCP tools and their execution logic
 * 
 * In a real-world scenario, this could be replaced with database or external service integration
 */
class McpToolRepositoryImpl(
    private val weatherService: WeatherService
) : McpToolRepository {
    
    override suspend fun getAllTools() = McpToolName.getAllTools()
    
    override suspend fun callTool(toolCall: McpToolCall): McpToolCallResult {
        val toolName = McpToolName.fromName(toolCall.toolName)
            ?: return createErrorResult("Инструмент '${toolCall.toolName}' не найден")
        
        return when (toolName) {
            McpToolName.GET_WEATHER -> handleGetWeather(toolCall.arguments)
            McpToolName.GET_CURRENT_WEATHER -> handleGetCurrentWeather(toolCall.arguments)
            McpToolName.GET_HOURLY_FORECAST -> handleGetHourlyForecast(toolCall.arguments)
            McpToolName.CALCULATE -> handleCalculate(toolCall.arguments)
            McpToolName.GET_TIME -> handleGetTime()
        }
    }
    
    // ==================== Weather Tools ====================
    
    private suspend fun handleGetWeather(arguments: Map<String, Any>): McpToolCallResult {
        val city = extractCity(arguments) ?: return createErrorResult("не указано название города")
        val weather = getWeatherForCity(city) ?: return createErrorResult("не удалось получить данные о погоде для города '$city'")
        
        return createSuccessResult("Погода в $city: ${weather.description}, ${formatTemperature(weather.temperature)}°C")
    }
    
    private suspend fun handleGetCurrentWeather(arguments: Map<String, Any>): McpToolCallResult {
        val city = extractCity(arguments) ?: return createErrorResult("не указано название города")
        val weather = getWeatherForCity(city) ?: return createErrorResult("не удалось получить данные о погоде для города '$city'")
        
        val result = buildString {
            appendLine("Текущая погода в $city:")
            appendLine("Температура: ${formatTemperature(weather.temperature)}°C")
            appendLine("Условия: ${weather.description}")
            appendLine("Скорость ветра: ${formatWindSpeed(weather.windSpeed)} км/ч")
            appendLine("Время: ${weather.time}")
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleGetHourlyForecast(arguments: Map<String, Any>): McpToolCallResult {
        val city = extractCity(arguments) ?: return createErrorResult("не указано название города")
        val hours = extractHours(arguments)
        
        val coordinates = getCoordinates(city) ?: return createErrorResult("не удалось найти координаты для города '$city'")
        val forecast = weatherService.getHourlyForecast(coordinates.first, coordinates.second, hours)
            ?: return createErrorResult("не удалось получить прогноз погоды для города '$city'")
        
        val result = buildString {
            appendLine("Почасовой прогноз погоды для $city (${forecast.size} часов):")
            appendLine()
            forecast.take(24).forEach { hour ->
                val time = extractTimeFromIso(hour.time)
                appendLine("$time: ${formatTemperature(hour.temperature)}°C, ${hour.description}, ветер ${formatWindSpeed(hour.windSpeed)} км/ч")
            }
            if (forecast.size > 24) {
                appendLine()
                appendLine("... и еще ${forecast.size - 24} часов")
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    // ==================== Calculation Tools ====================
    
    private fun handleCalculate(arguments: Map<String, Any>): McpToolCallResult {
        val expression = extractExpression(arguments)
            ?: return createErrorResult("не указано выражение для вычисления")
        
        return try {
            val result = evaluateSimpleExpression(expression)
            createSuccessResult("Результат: $result")
        } catch (e: Exception) {
            createErrorResult("вычисления: ${e.message}")
        }
    }
    
    // ==================== Time Tools ====================
    
    private fun handleGetTime(): McpToolCallResult {
        val currentTime = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        return createSuccessResult("Текущее время: $currentTime")
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Extract city argument from arguments map
     */
    private fun extractCity(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.CITY.key()] as? String
    }
    
    /**
     * Extract hours argument from arguments map with validation
     */
    private fun extractHours(arguments: Map<String, Any>): Int {
        return (arguments[McpToolArgument.HOURS.key()] as? Number)?.toInt()?.coerceIn(1, 168) ?: 24
    }
    
    /**
     * Extract expression argument from arguments map
     */
    private fun extractExpression(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.EXPRESSION.key()] as? String
    }
    
    /**
     * Get coordinates for a city
     */
    private suspend fun getCoordinates(city: String): Pair<Double, Double>? {
        return weatherService.getCoordinates(city)
    }
    
    /**
     * Get current weather for a city
     */
    private suspend fun getWeatherForCity(city: String): ru.mirtomsk.server.domain.service.CurrentWeatherData? {
        val coordinates = getCoordinates(city) ?: return null
        return weatherService.getCurrentWeather(coordinates.first, coordinates.second)
    }
    
    /**
     * Create success result with text content
     */
    private fun createSuccessResult(text: String): McpToolCallResult {
        return McpToolCallResult(
            content = listOf(
                McpToolCallContent(
                    type = "text",
                    text = text
                )
            )
        )
    }
    
    /**
     * Create error result with error message
     */
    private fun createErrorResult(message: String): McpToolCallResult {
        return McpToolCallResult(
            content = listOf(
                McpToolCallContent(
                    type = "text",
                    text = "Ошибка: $message"
                )
            ),
            isError = true
        )
    }
    
    /**
     * Format temperature to one decimal place
     */
    private fun formatTemperature(temperature: Double): String {
        return "%.1f".format(temperature)
    }
    
    /**
     * Format wind speed to one decimal place
     */
    private fun formatWindSpeed(windSpeed: Double): String {
        return "%.1f".format(windSpeed)
    }
    
    /**
     * Extract time (HH:mm) from ISO format string (YYYY-MM-DDTHH:mm)
     */
    private fun extractTimeFromIso(isoTime: String): String {
        return try {
            if (isoTime.length >= 16) {
                isoTime.substring(11, 16)
            } else {
                isoTime
            }
        } catch (e: Exception) {
            isoTime
        }
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
