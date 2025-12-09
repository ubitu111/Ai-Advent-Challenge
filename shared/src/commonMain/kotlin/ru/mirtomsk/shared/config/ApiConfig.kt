package ru.mirtomsk.shared.config

/**
 * Configuration provider for API keys and other settings
 */
interface ApiConfig {
    val apiKey: String
    val keyId: String
    val mcpgateToken: String
    val useLocalModel: Boolean
    val localModelBaseUrl: String
    val localModelName: String
}

/**
 * Implementation that reads from build config or properties
 * For multiplatform, we'll inject values from build.gradle.kts
 */
class ApiConfigImpl(
    override val apiKey: String,
    override val keyId: String,
    override val mcpgateToken: String,
    override val useLocalModel: Boolean = false,
    override val localModelBaseUrl: String = "http://localhost:11434",
    override val localModelName: String = "llama3.1:8b",
) : ApiConfig

