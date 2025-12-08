package ru.mirtomsk.server.data.repository

import ru.mirtomsk.server.domain.model.McpToolCall
import ru.mirtomsk.server.domain.model.McpToolCallContent
import ru.mirtomsk.server.domain.model.McpToolCallResult
import ru.mirtomsk.server.domain.model.TaskPriority
import ru.mirtomsk.server.domain.model.TaskStatus
import ru.mirtomsk.server.domain.repository.McpToolRepository
import ru.mirtomsk.server.domain.model.Ticket
import ru.mirtomsk.server.domain.service.CurrencyService
import ru.mirtomsk.server.domain.service.GitHubService
import ru.mirtomsk.server.domain.service.LocalGitService
import ru.mirtomsk.server.domain.service.TaskService
import ru.mirtomsk.server.domain.service.TicketService
import ru.mirtomsk.server.domain.service.WeatherService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Implementation of McpToolRepository
 * Provides in-memory storage of MCP tools and their execution logic
 * 
 * In a real-world scenario, this could be replaced with database or external service integration
 */
class McpToolRepositoryImpl(
    private val weatherService: WeatherService,
    private val currencyService: CurrencyService,
    private val gitHubService: GitHubService,
    private val localGitService: LocalGitService,
    private val ticketService: TicketService,
    private val taskService: TaskService,
) : McpToolRepository {
    
    override suspend fun getAllTools() = McpToolName.getAllTools()
    
    override suspend fun callTool(toolCall: McpToolCall): McpToolCallResult {
        val toolName = McpToolName.fromName(toolCall.toolName)
            ?: return createErrorResult("Инструмент '${toolCall.toolName}' не найден")
        
        return when (toolName) {
//            McpToolName.GET_WEATHER -> handleGetWeather(toolCall.arguments)
//            McpToolName.GET_CURRENT_WEATHER -> handleGetCurrentWeather(toolCall.arguments)
//            McpToolName.GET_HOURLY_FORECAST -> handleGetHourlyForecast(toolCall.arguments)
//            McpToolName.GET_WEATHER_BY_DATE -> handleGetWeatherByDate(toolCall.arguments)
//            McpToolName.CALCULATE -> handleCalculate(toolCall.arguments)
//            McpToolName.GET_CURRENCY_RATE -> handleGetCurrencyRate(toolCall.arguments)
//            McpToolName.GET_CURRENCY_RATE_HISTORICAL -> handleGetCurrencyRateHistorical(toolCall.arguments)
//            McpToolName.GET_CITY_COORDINATES -> handleGetCityCoordinates(toolCall.arguments)
            McpToolName.GET_CURRENT_DATETIME -> handleGetCurrentDateTime()
//            McpToolName.GIT_STATUS -> handleGitStatus()
//            McpToolName.GIT_LOG -> handleGitLog(toolCall.arguments)
//            McpToolName.GIT_BRANCH -> handleGitBranch()
            McpToolName.GIT_STATUS_LOCAL -> handleGitStatusLocal()
            McpToolName.GIT_LOG_LOCAL -> handleGitLogLocal(toolCall.arguments)
            McpToolName.GIT_BRANCH_LOCAL -> handleGitBranchLocal()
            McpToolName.GIT_DIFF_LOCAL -> handleGitDiffLocal(toolCall.arguments)
            McpToolName.READ_TICKETS -> handleReadTickets()
            McpToolName.CREATE_TICKET -> handleCreateTicket(toolCall.arguments)
            McpToolName.CREATE_TASK -> handleCreateTask(toolCall.arguments)
            McpToolName.GET_ALL_TASKS -> handleGetAllTasks()
            McpToolName.GET_TASKS_BY_PRIORITY_AND_STATUS -> handleGetTasksByPriorityAndStatus(toolCall.arguments)
        }
    }
    
    // ==================== Weather Tools ====================
    
    private suspend fun handleGetWeather(arguments: Map<String, Any>): McpToolCallResult {
        val coordinates = extractCoordinates(arguments) 
            ?: return createErrorResult("не указаны координаты (latitude и longitude)")
        
        val weather = getWeatherForCoordinates(coordinates.first, coordinates.second)
            ?: return createErrorResult("не удалось получить данные о погоде")
        
        val location = "${coordinates.first}, ${coordinates.second}"
        return createSuccessResult("Погода в $location: ${weather.description}, ${formatTemperature(weather.temperature)}°C")
    }
    
    private suspend fun handleGetCurrentWeather(arguments: Map<String, Any>): McpToolCallResult {
        val coordinates = extractCoordinates(arguments) 
            ?: return createErrorResult("не указаны координаты (latitude и longitude)")
        
        val weather = getWeatherForCoordinates(coordinates.first, coordinates.second)
            ?: return createErrorResult("не удалось получить данные о погоде")
        
        val location = "${coordinates.first}, ${coordinates.second}"
        val result = buildString {
            appendLine("Текущая погода в $location:")
            appendLine("Температура: ${formatTemperature(weather.temperature)}°C")
            appendLine("Условия: ${weather.description}")
            appendLine("Скорость ветра: ${formatWindSpeed(weather.windSpeed)} км/ч")
            appendLine("Время: ${weather.time}")
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleGetHourlyForecast(arguments: Map<String, Any>): McpToolCallResult {
        val coordinates = extractCoordinates(arguments) 
            ?: return createErrorResult("не указаны координаты (latitude и longitude)")
        val hours = extractHours(arguments)
        
        val forecast = weatherService.getHourlyForecast(coordinates.first, coordinates.second, hours)
            ?: return createErrorResult("не удалось получить прогноз погоды")
        
        val location = "${coordinates.first}, ${coordinates.second}"
        val result = buildString {
            appendLine("Почасовой прогноз погоды для $location (${forecast.size} часов):")
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
    
    private suspend fun handleGetWeatherByDate(arguments: Map<String, Any>): McpToolCallResult {
        val coordinates = extractCoordinates(arguments) 
            ?: return createErrorResult("не указаны координаты (latitude и longitude)")
        val date = extractDate(arguments)
            ?: return createErrorResult("не указана дата")
        
        // Validate date format (YYYY-MM-DD)
        if (!isValidDateFormat(date)) {
            return createErrorResult("неверный формат даты. Используйте формат YYYY-MM-DD (например: 2024-12-25)")
        }
        
        val forecast = weatherService.getDailyForecastByDate(coordinates.first, coordinates.second, date)
            ?: return createErrorResult("не удалось получить прогноз погоды на дату $date. Убедитесь, что дата не более чем на 16 дней в будущем")
        
        val location = "${coordinates.first}, ${coordinates.second}"
        val result = buildString {
            appendLine("Прогноз погоды на $date для $location:")
            appendLine("Температура: от ${formatTemperature(forecast.temperatureMin)}°C до ${formatTemperature(forecast.temperatureMax)}°C")
            appendLine("Условия: ${forecast.description}")
            appendLine("Максимальная скорость ветра: ${formatWindSpeed(forecast.windSpeedMax)} км/ч")
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
    
    private fun handleGetCurrentDateTime(): McpToolCallResult {
        val now = java.time.LocalDateTime.now()
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        val result = buildString {
            appendLine("Текущая дата и время:")
            appendLine("Дата и время: ${now.format(dateTimeFormatter)}")
            appendLine("Дата: ${now.format(dateFormatter)}")
            appendLine("Время: ${now.format(timeFormatter)}")
            appendLine("ISO формат: ${now.format(isoFormatter)}")
            appendLine("День недели: ${now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("ru"))}")
            appendLine("День года: ${now.dayOfYear}")
            appendLine("Неделя года: ${now.get(java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).weekOfWeekBasedYear())}")
        }
        
        return createSuccessResult(result.trim())
    }
    
    // ==================== Location Tools ====================
    
    private suspend fun handleGetCityCoordinates(arguments: Map<String, Any>): McpToolCallResult {
        val city = extractCity(arguments) ?: return createErrorResult("не указано название города")
        val coordinates = getCoordinates(city) 
            ?: return createErrorResult("не удалось найти координаты для города '$city'")
        
        val result = buildString {
            appendLine("Координаты для города '$city':")
            appendLine("Широта: ${coordinates.first}")
            appendLine("Долгота: ${coordinates.second}")
            appendLine("Формат: ${coordinates.first}, ${coordinates.second}")
        }
        
        return createSuccessResult(result.trim())
    }
    
    // ==================== Currency Tools ====================
    
    private suspend fun handleGetCurrencyRate(arguments: Map<String, Any>): McpToolCallResult {
        val baseCurrency = extractBaseCurrency(arguments)
            ?: return createErrorResult("не указана базовая валюта")
        val targetCurrency = extractTargetCurrency(arguments)
            ?: return createErrorResult("не указана целевая валюта")
        
        val rateData = currencyService.getExchangeRate(baseCurrency, targetCurrency)
            ?: return createErrorResult("не удалось получить курс обмена для пары $baseCurrency/$targetCurrency")
        
        val result = buildString {
            appendLine("Курс обмена валют:")
            appendLine("${rateData.baseCurrency} → ${rateData.targetCurrency}")
            appendLine("Курс: ${formatRate(rateData.rate)}")
            appendLine("Дата: ${rateData.date}")
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleGetCurrencyRateHistorical(arguments: Map<String, Any>): McpToolCallResult {
        val baseCurrency = extractBaseCurrency(arguments)
            ?: return createErrorResult("не указана базовая валюта")
        val targetCurrency = extractTargetCurrency(arguments)
            ?: return createErrorResult("не указана целевая валюта")
        val date = extractDate(arguments)
            ?: return createErrorResult("не указана дата")
        
        // Validate date format (YYYY-MM-DD)
        if (!isValidDateFormat(date)) {
            return createErrorResult("неверный формат даты. Используйте формат YYYY-MM-DD (например: 2024-01-15)")
        }
        
        val rateData = currencyService.getExchangeRateByDate(baseCurrency, targetCurrency, date)
            ?: return createErrorResult("не удалось получить курс обмена для пары $baseCurrency/$targetCurrency на дату $date")
        
        val result = buildString {
            appendLine("Курс обмена валют на $date:")
            appendLine("${rateData.baseCurrency} → ${rateData.targetCurrency}")
            appendLine("Курс: ${formatRate(rateData.rate)}")
            appendLine("Дата: ${rateData.date}")
        }
        
        return createSuccessResult(result.trim())
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Extract city argument from arguments map
     */
    private fun extractCity(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.CITY.key()] as? String
    }
    
    /**
     * Extract coordinates from arguments map (latitude and longitude)
     */
    private fun extractCoordinates(arguments: Map<String, Any>): Pair<Double, Double>? {
        val latitude = (arguments[McpToolArgument.LATITUDE.key()] as? Number)?.toDouble()
        val longitude = (arguments[McpToolArgument.LONGITUDE.key()] as? Number)?.toDouble()
        
        if (latitude != null && longitude != null) {
            // Validate coordinates
            if (latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                return Pair(latitude, longitude)
            } else {
                return null
            }
        }
        
        return null
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
     * Extract base currency argument from arguments map
     */
    private fun extractBaseCurrency(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.BASE_CURRENCY.key()] as? String
    }
    
    /**
     * Extract target currency argument from arguments map
     */
    private fun extractTargetCurrency(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.TARGET_CURRENCY.key()] as? String
    }
    
    /**
     * Extract date argument from arguments map
     */
    private fun extractDate(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.DATE.key()] as? String
    }
    
    /**
     * Validate date format (YYYY-MM-DD)
     */
    private fun isValidDateFormat(date: String): Boolean {
        return try {
            val regex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
            if (!regex.matches(date)) return false
            
            val parts = date.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            
            // Basic validation
            year in 2000..2100 && month in 1..12 && day in 1..31
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get coordinates for a city
     */
    private suspend fun getCoordinates(city: String): Pair<Double, Double>? {
        return weatherService.getCoordinates(city)
    }
    
    /**
     * Get current weather for coordinates
     */
    private suspend fun getWeatherForCoordinates(latitude: Double, longitude: Double): ru.mirtomsk.server.domain.service.CurrentWeatherData? {
        return weatherService.getCurrentWeather(latitude, longitude)
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
     * Format exchange rate to 4 decimal places
     */
    private fun formatRate(rate: Double): String {
        return "%.4f".format(rate)
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
    
    // ==================== GitHub Tools ====================
    
    private suspend fun handleGitStatus(): McpToolCallResult {
        val status = gitHubService.getRepositoryStatus()
            ?: return createErrorResult("не удалось получить статус репозитория. Убедитесь, что GitHub настроен правильно (GITHUB_TOKEN, GITHUB_OWNER, GITHUB_REPO)")
        
        val result = buildString {
            appendLine("Статус репозитория GitHub:")
            appendLine("Репозиторий: ${status.fullName}")
            appendLine("Описание: ${status.description ?: "Нет описания"}")
            appendLine("Владелец: ${status.owner ?: "Неизвестно"}")
            appendLine("Ветка по умолчанию: ${status.defaultBranch ?: "Неизвестно"}")
            appendLine("Последнее обновление: ${status.lastUpdated ?: "Неизвестно"}")
            appendLine("Последний push: ${status.lastPushed ?: "Неизвестно"}")
            if (status.url != null) {
                appendLine("URL: ${status.url}")
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleGitLog(arguments: Map<String, Any>): McpToolCallResult {
        val limit = extractLimit(arguments)
        val branch = extractBranch(arguments)
        
        val commits = gitHubService.getCommitHistory(limit, branch)
            ?: return createErrorResult("не удалось получить историю коммитов. Убедитесь, что GitHub настроен правильно (GITHUB_TOKEN, GITHUB_OWNER, GITHUB_REPO)")
        
        if (commits.isEmpty()) {
            return createSuccessResult("История коммитов пуста")
        }
        
        val result = buildString {
            appendLine("История коммитов (${commits.size}):")
            appendLine()
            commits.forEachIndexed { index, commit ->
                appendLine("${index + 1}. ${commit.sha} - ${commit.message}")
                appendLine("   Автор: ${commit.author} <${commit.authorEmail}>")
                appendLine("   Дата: ${commit.date}")
                if (commit.url != null) {
                    appendLine("   URL: ${commit.url}")
                }
                appendLine()
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleGitBranch(): McpToolCallResult {
        val branches = gitHubService.getBranches()
            ?: return createErrorResult("не удалось получить список веток. Убедитесь, что GitHub настроен правильно (GITHUB_TOKEN, GITHUB_OWNER, GITHUB_REPO)")
        
        if (branches.isEmpty()) {
            return createSuccessResult("Ветки не найдены")
        }
        
        val result = buildString {
            appendLine("Список веток (${branches.size}):")
            appendLine()
            branches.forEach { branch ->
                val protected = if (branch.isProtected) " [защищена]" else ""
                appendLine("  ${branch.name}${protected} (последний коммит: ${branch.lastCommitSha})")
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    /**
     * Extract limit argument from arguments map
     */
    private fun extractLimit(arguments: Map<String, Any>): Int {
        return (arguments[McpToolArgument.LIMIT.key()] as? Number)?.toInt()?.coerceIn(1, 100) ?: 30
    }
    
    /**
     * Extract branch argument from arguments map
     */
    private fun extractBranch(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.BRANCH.key()] as? String
    }
    
    // ==================== Local Git Tools ====================
    
    private suspend fun handleGitStatusLocal(): McpToolCallResult {
        val status = localGitService.getRepositoryStatus()
            ?: return createErrorResult("не удалось получить статус локального репозитория. Убедитесь, что GIT_REPO_PATH настроен правильно и указывает на валидный Git репозиторий")
        
        val result = buildString {
            appendLine("Статус локального Git репозитория:")
            appendLine("Текущая ветка: ${status.currentBranch}")
            appendLine("Состояние: ${if (status.isClean) "чистое" else "есть изменения"}")
            
            if (status.lastCommit != null) {
                appendLine("Последний коммит: ${status.lastCommit}")
                if (status.lastCommitMessage != null) {
                    appendLine("Сообщение: ${status.lastCommitMessage}")
                }
            }
            
            if (status.stagedFiles.isNotEmpty()) {
                appendLine()
                appendLine("Файлы в staging area (${status.stagedFiles.size}):")
                status.stagedFiles.forEach { file ->
                    appendLine("  + $file")
                }
            }
            
            if (status.modifiedFiles.isNotEmpty()) {
                appendLine()
                appendLine("Измененные файлы (${status.modifiedFiles.size}):")
                status.modifiedFiles.forEach { file ->
                    appendLine("  M $file")
                }
            }
            
            if (status.untrackedFiles.isNotEmpty()) {
                appendLine()
                appendLine("Неотслеживаемые файлы (${status.untrackedFiles.size}):")
                status.untrackedFiles.forEach { file ->
                    appendLine("  ? $file")
                }
            }
            
            if (status.isClean) {
                appendLine()
                appendLine("Рабочая директория чистая, нет изменений для коммита.")
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleGitLogLocal(arguments: Map<String, Any>): McpToolCallResult {
        val limit = extractLimitLocal(arguments)
        val branch = extractBranch(arguments)
        
        val commits = localGitService.getCommitHistory(limit, branch)
            ?: return createErrorResult("не удалось получить историю коммитов. Убедитесь, что GIT_REPO_PATH настроен правильно и указывает на валидный Git репозиторий")
        
        if (commits.isEmpty()) {
            return createSuccessResult("История коммитов пуста")
        }
        
        val result = buildString {
            appendLine("История коммитов локального репозитория (${commits.size}):")
            if (branch != null) {
                appendLine("Ветка: $branch")
            }
            appendLine()
            commits.forEachIndexed { index, commit ->
                appendLine("${index + 1}. ${commit.sha} - ${commit.message}")
                appendLine("   Автор: ${commit.author}")
                appendLine("   Дата: ${commit.date}")
                appendLine()
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleGitBranchLocal(): McpToolCallResult {
        val branches = localGitService.getBranches()
            ?: return createErrorResult("не удалось получить список веток. Убедитесь, что GIT_REPO_PATH настроен правильно и указывает на валидный Git репозиторий")
        
        if (branches.isEmpty()) {
            return createSuccessResult("Ветки не найдены")
        }
        
        val result = buildString {
            appendLine("Список веток локального репозитория (${branches.size}):")
            appendLine()
            branches.forEach { branch ->
                val current = if (branch.isCurrent) "* " else "  "
                appendLine("$current${branch.name} (${branch.lastCommitSha})")
                if (branch.lastCommitMessage != null) {
                    appendLine("    ${branch.lastCommitMessage}")
                }
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    /**
     * Extract limit argument from arguments map for local git (max 1000)
     */
    private fun extractLimitLocal(arguments: Map<String, Any>): Int {
        return (arguments[McpToolArgument.LIMIT.key()] as? Number)?.toInt()?.coerceIn(1, 1000) ?: 30
    }

    private suspend fun handleGitDiffLocal(arguments: Map<String, Any>): McpToolCallResult {
        val filePath = extractFilePath(arguments)
        val staged = extractStaged(arguments)

        val diff = localGitService.getDiff(filePath, staged)
            ?: return createErrorResult("не удалось получить diff. Убедитесь, что GIT_REPO_PATH настроен правильно и указывает на валидный Git репозиторий")

        if (diff.isEmpty()) {
            val message = when {
                filePath != null && staged -> "Нет изменений для файла '$filePath' в staging area"
                filePath != null -> "Нет неиндексированных изменений для файла '$filePath'"
                staged -> "Нет изменений в staging area"
                else -> "Нет неиндексированных изменений в рабочей директории"
            }
            return createSuccessResult(message)
        }

        val result = buildString {
            if (filePath != null) {
                appendLine("Diff для файла: $filePath")
            } else {
                appendLine("Diff всех измененных файлов")
            }
            if (staged) {
                appendLine("(изменения в staging area)")
            } else {
                appendLine("(неиндексированные изменения)")
            }
            appendLine()
            appendLine(diff)
        }

        return createSuccessResult(result.trim())
    }

    /**
     * Extract file path argument from arguments map
     */
    private fun extractFilePath(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.FILE_PATH.key()] as? String
    }

    /**
     * Extract staged argument from arguments map
     */
    private fun extractStaged(arguments: Map<String, Any>): Boolean {
        return (arguments[McpToolArgument.STAGED.key()] as? Boolean) ?: false
    }
    
    // ==================== Ticket Tools ====================
    
    private suspend fun handleReadTickets(): McpToolCallResult {
        val tickets = ticketService.getAllTickets()
        
        if (tickets.isEmpty()) {
            return createSuccessResult("Тикеты не найдены. Список тикетов пуст.")
        }
        
        val result = buildString {
            appendLine("Список тикетов (${tickets.size}):")
            appendLine()
            tickets.forEachIndexed { index, ticket ->
                appendLine("${index + 1}. ${ticket.title}")
                appendLine("   Пользователь: ${ticket.username}")
                appendLine("   Дата: ${ticket.date}")
                appendLine("   Вопрос: ${ticket.question}")
                if (ticket.answer != null && ticket.answer.isNotBlank()) {
                    appendLine("   Ответ: ${ticket.answer}")
                } else {
                    appendLine("   Ответ: (не указан)")
                }
                appendLine()
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleCreateTicket(arguments: Map<String, Any>): McpToolCallResult {
        val username = extractUsername(arguments)
            ?: return createErrorResult("не указано имя пользователя")
        val title = extractTitle(arguments)
            ?: return createErrorResult("не указан заголовок тикета")
        val question = extractQuestion(arguments)
            ?: return createErrorResult("не указан вопрос")
        val answer = extractAnswer(arguments)
        val date = extractDate(arguments) ?: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val ticket = Ticket(
            username = username,
            date = date,
            title = title,
            question = question,
            answer = answer
        )
        
        val createdTicket = ticketService.createTicket(ticket)
        
        val result = buildString {
            appendLine("Тикет успешно создан:")
            appendLine("Заголовок: ${createdTicket.title}")
            appendLine("Пользователь: ${createdTicket.username}")
            appendLine("Дата: ${createdTicket.date}")
            appendLine("Вопрос: ${createdTicket.question}")
            if (createdTicket.answer != null && createdTicket.answer.isNotBlank()) {
                appendLine("Ответ: ${createdTicket.answer}")
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    /**
     * Extract username argument from arguments map
     */
    private fun extractUsername(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.USERNAME.key()] as? String
    }
    
    /**
     * Extract title argument from arguments map
     */
    private fun extractTitle(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.TITLE.key()] as? String
    }
    
    /**
     * Extract question argument from arguments map
     */
    private fun extractQuestion(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.QUESTION.key()] as? String
    }
    
    /**
     * Extract answer argument from arguments map
     */
    private fun extractAnswer(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.ANSWER.key()] as? String
    }
    
    // ==================== Task Tools ====================
    
    private suspend fun handleCreateTask(arguments: Map<String, Any>): McpToolCallResult {
        val taskName = extractTaskName(arguments)
            ?: return createErrorResult("не указано название задачи")
        val taskDescription = extractTaskDescription(arguments)
            ?: return createErrorResult("не указано описание задачи")
        val priorityString = extractPriorityString(arguments)
            ?: return createErrorResult("не указан приоритет задачи")
        val priority = TaskPriority.fromName(priorityString)
            ?: return createErrorResult("неверный приоритет задачи. Возможные значения: ${TaskPriority.getAllNames().joinToString(", ")}")
        val statusString = extractStatusString(arguments)
        val status = if (statusString != null) {
            TaskStatus.fromName(statusString)
                ?: return createErrorResult("неверный статус задачи. Возможные значения: ${TaskStatus.getAllNames().joinToString(", ")}")
        } else {
            TaskStatus.NEW
        }
        
        val task = taskService.createTask(taskName, taskDescription, priority, status)
        
        val result = buildString {
            appendLine("Задача успешно создана:")
            appendLine("ID: ${task.id}")
            appendLine("Название: ${task.name}")
            appendLine("Описание: ${task.description}")
            appendLine("Приоритет: ${task.priority.displayName} (${task.priority.name})")
            appendLine("Статус: ${task.status.displayName} (${task.status.name})")
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleGetAllTasks(): McpToolCallResult {
        val tasks = taskService.getAllTasks()
        
        if (tasks.isEmpty()) {
            return createSuccessResult("Задачи не найдены. Список задач пуст.")
        }
        
        val result = buildString {
            appendLine("Список задач (${tasks.size}):")
            appendLine()
            tasks.forEachIndexed { index, task ->
                appendLine("${index + 1}. ${task.name}")
                appendLine("   ID: ${task.id}")
                appendLine("   Описание: ${task.description}")
                appendLine("   Приоритет: ${task.priority.displayName} (${task.priority.name})")
                appendLine("   Статус: ${task.status.displayName} (${task.status.name})")
                appendLine()
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    private suspend fun handleGetTasksByPriorityAndStatus(arguments: Map<String, Any>): McpToolCallResult {
        val priorityString = extractPriorityString(arguments)
        val priority = priorityString?.let { 
            TaskPriority.fromName(it) 
                ?: return createErrorResult("неверный приоритет задачи. Возможные значения: ${TaskPriority.getAllNames().joinToString(", ")}")
        }
        val statusString = extractStatusString(arguments)
        val status = statusString?.let {
            TaskStatus.fromName(it)
                ?: return createErrorResult("неверный статус задачи. Возможные значения: ${TaskStatus.getAllNames().joinToString(", ")}")
        }
        
        val tasks = taskService.getTasksByPriorityAndStatus(priority, status)
        
        if (tasks.isEmpty()) {
            val filters = buildString {
                if (priority != null) append("приоритет: ${priority.name}")
                if (priority != null && status != null) append(", ")
                if (status != null) append("статус: ${status.name}")
            }
            val filterText = if (filters.isNotEmpty()) " с фильтрами ($filters)" else ""
            return createSuccessResult("Задачи$filterText не найдены.")
        }
        
        val result = buildString {
            val filters = buildString {
                if (priority != null) append("приоритет: ${priority.name}")
                if (priority != null && status != null) append(", ")
                if (status != null) append("статус: ${status.name}")
            }
            val filterText = if (filters.isNotEmpty()) " (фильтры: $filters)" else ""
            appendLine("Список задач${filterText} (${tasks.size}):")
            appendLine()
            tasks.forEachIndexed { index, task ->
                appendLine("${index + 1}. ${task.name}")
                appendLine("   ID: ${task.id}")
                appendLine("   Описание: ${task.description}")
                appendLine("   Приоритет: ${task.priority.displayName} (${task.priority.name})")
                appendLine("   Статус: ${task.status.displayName} (${task.status.name})")
                appendLine()
            }
        }
        
        return createSuccessResult(result.trim())
    }
    
    /**
     * Extract task name argument from arguments map
     */
    private fun extractTaskName(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.TASK_NAME.key()] as? String
    }
    
    /**
     * Extract task description argument from arguments map
     */
    private fun extractTaskDescription(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.TASK_DESCRIPTION.key()] as? String
    }
    
    /**
     * Extract priority argument from arguments map as string
     */
    private fun extractPriorityString(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.PRIORITY.key()] as? String
    }
    
    /**
     * Extract status argument from arguments map as string
     */
    private fun extractStatusString(arguments: Map<String, Any>): String? {
        return arguments[McpToolArgument.STATUS.key()] as? String
    }
}
