package ru.mirtomsk.server.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import ru.mirtomsk.server.data.repository.McpToolRepositoryImpl
import ru.mirtomsk.server.data.service.ExchangeRateCurrencyService
import ru.mirtomsk.server.data.service.GitHubApiService
import ru.mirtomsk.server.data.service.LocalGitCommandService
import ru.mirtomsk.server.data.service.OpenMeteoWeatherService
import ru.mirtomsk.server.domain.repository.McpToolRepository
import ru.mirtomsk.server.domain.service.CurrencyService
import ru.mirtomsk.server.domain.service.GitHubService
import ru.mirtomsk.server.domain.service.LocalGitService
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
        
        install(Logging) {
            logger = object : Logger {
                private val log = LoggerFactory.getLogger("HttpClient")
                
                override fun log(message: String) {
                    log.debug(message)
                }
            }
            level = LogLevel.ALL
            sanitizeHeader { name -> name == "Authorization" }
        }
    }

    // Services
    private val weatherService: WeatherService = OpenMeteoWeatherService(httpClient)
    private val currencyService: CurrencyService = ExchangeRateCurrencyService(httpClient)
    private val gitHubService: GitHubService = GitHubApiService(httpClient)
    private val localGitService: LocalGitService = LocalGitCommandService()

    // Repository
    private val mcpToolRepository: McpToolRepository = McpToolRepositoryImpl(weatherService, currencyService, gitHubService, localGitService)

    // Use cases
    val getToolsUseCase: GetToolsUseCase = GetToolsUseCase(mcpToolRepository)
    val callToolUseCase: CallToolUseCase = CallToolUseCase(mcpToolRepository)

    // Controllers
    val mcpController: McpController = McpController(
        getToolsUseCase = getToolsUseCase,
        callToolUseCase = callToolUseCase
    )
}
