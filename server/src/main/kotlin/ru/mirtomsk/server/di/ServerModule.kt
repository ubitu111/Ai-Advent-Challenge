package ru.mirtomsk.server.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.mirtomsk.server.data.repository.McpToolRepositoryImpl
import ru.mirtomsk.server.data.service.OpenMeteoWeatherService
import ru.mirtomsk.server.domain.repository.McpToolRepository
import ru.mirtomsk.server.domain.service.WeatherService
import ru.mirtomsk.server.domain.usecase.CallToolUseCase
import ru.mirtomsk.server.domain.usecase.GetToolsUseCase
import ru.mirtomsk.server.presentation.controller.McpController

/**
 * Dependency injection module for server
 * In a real-world scenario, this could use a DI framework like Koin or Kodein
 * For simplicity, we use manual dependency injection
 */
object ServerModule {

    // HTTP Client
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    // Services
    private val weatherService: WeatherService = OpenMeteoWeatherService(httpClient)

    // Repository
    private val mcpToolRepository: McpToolRepository = McpToolRepositoryImpl(weatherService)

    // Use cases
    val getToolsUseCase: GetToolsUseCase = GetToolsUseCase(mcpToolRepository)
    val callToolUseCase: CallToolUseCase = CallToolUseCase(mcpToolRepository)

    // Controllers
    val mcpController: McpController = McpController(
        getToolsUseCase = getToolsUseCase,
        callToolUseCase = callToolUseCase
    )
}
