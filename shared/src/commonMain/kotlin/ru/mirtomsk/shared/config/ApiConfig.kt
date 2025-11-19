package ru.mirtomsk.shared.config

/**
 * Configuration provider for API keys and other settings
 */
interface ApiConfig {
    val apiKey: String
    val keyId: String
    val huggingFaceToken: String
    val mcpgateToken: String
}

/**
 * Implementation that reads from build config or properties
 * For multiplatform, we'll inject values from build.gradle.kts
 */
class ApiConfigImpl(
    override val apiKey: String,
    override val keyId: String,
    override val huggingFaceToken: String,
    override val mcpgateToken: String,
) : ApiConfig

