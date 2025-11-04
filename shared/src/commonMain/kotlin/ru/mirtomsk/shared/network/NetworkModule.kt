package ru.mirtomsk.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkModule {
    /**
     * Creates and configures an HttpClient instance for network requests
     * 
     * @param enableLogging Whether to enable request/response logging (default: true)
     * @return Configured HttpClient instance
     */
    fun createHttpClient(enableLogging: Boolean = true): HttpClient {
        return HttpClient {
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

