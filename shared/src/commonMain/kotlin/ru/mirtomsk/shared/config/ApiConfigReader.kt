package ru.mirtomsk.shared.config

import java.util.Properties

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

    private fun readProperty(key: String): String {

        val properties = Properties()
        val resourceStream = getResourceInputStream("api.properties")

        resourceStream.use { properties.load(it) }
        return properties.getProperty(key)
    }
}

