package ru.mirtomsk.shared.config

import java.util.Properties
import java.nio.charset.StandardCharsets

/**
 * Reads API configuration from properties file
 * Reads from api.properties resource file which is generated from local.properties during build
 */
expect fun getResourceInputStream(path: String): java.io.InputStream?

object ApiConfigReader {
    fun readApiKey(): String {
        return readProperty("api.key")
    }

    fun readKeyId(): String {
        return readProperty("api.key.id")
    }

    fun readMcpgateToken(): String {
        return readProperty("mcpgate.token")
    }

    fun readUseLocalModel(): Boolean {
        return readProperty("local.model.enabled", "false").toBoolean()
    }

    fun readLocalModelBaseUrl(): String {
        return readProperty("local.model.base.url", "http://localhost:11434")
    }

    fun readLocalModelName(): String {
        return readProperty("local.model.name", "llama3.1:8b")
    }

    private fun readProperty(key: String): String {
        val properties = Properties()
        val resourceStream = getResourceInputStream("api.properties")

        resourceStream?.use { inputStream ->
            java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                properties.load(reader)
            }
        }
        return properties.getProperty(key) ?: ""
    }

    private fun readProperty(key: String, defaultValue: String): String {
        val properties = Properties()
        val resourceStream = getResourceInputStream("api.properties")

        resourceStream?.use { inputStream ->
            java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                properties.load(reader)
            }
        }
        return properties.getProperty(key, defaultValue)
    }
}

