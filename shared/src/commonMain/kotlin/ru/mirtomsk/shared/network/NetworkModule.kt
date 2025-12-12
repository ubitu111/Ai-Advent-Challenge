package ru.mirtomsk.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

expect fun createHttpClientEngine(): io.ktor.client.engine.HttpClientEngine

object NetworkModule {
    /**
     * Creates and configures an HttpClient instance for network requests
     *
     * @param enableLogging Whether to enable request/response logging (default: true)
     * @return Configured HttpClient instance
     */
    fun createHttpClient(enableLogging: Boolean = true): HttpClient {
        return HttpClient(createHttpClientEngine()) {
            // Don't throw exceptions on HTTP error status codes
            expectSuccess = false

            // Content Negotiation for JSON serialization
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    }
                )
            }

            // Настройка таймаутов для HTTP запросов
            // Пример: как задать время ожидания ответа в Ktor
            install(HttpTimeout) {
                // Таймаут на подключение (connection timeout)
                // Время, в течение которого клиент будет пытаться установить соединение
                connectTimeoutMillis = 10.seconds.inWholeMilliseconds

                // Таймаут на получение ответа (socket timeout / read timeout)
                // Время ожидания данных от сервера после установки соединения
                socketTimeoutMillis = 80.seconds.inWholeMilliseconds

                // Общий таймаут на запрос (request timeout)
                // Максимальное время выполнения всего запроса (включая подключение и чтение)
                requestTimeoutMillis = 120.seconds.inWholeMilliseconds
            }

            // Logging (optional)
            if (enableLogging) {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            println("Ktor: $message")
                        }
                    }
                    level = LogLevel.ALL
                }
            }
        }
    }
}

