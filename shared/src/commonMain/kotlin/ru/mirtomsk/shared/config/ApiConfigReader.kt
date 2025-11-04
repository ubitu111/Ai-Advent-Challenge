package ru.mirtomsk.shared.config

import java.util.Properties

/**
 * Reads API configuration from properties file
 * Reads from api.properties resource file which is generated from local.properties during build
 */
object ApiConfigReader {
    fun readApiKey(): String {
        return readProperty("api.key", "")
    }
    
    fun readKeyId(): String {
        return readProperty("api.key.id", "")
    }
    
    private fun readProperty(key: String, defaultValue: String): String {
        return try {
            val properties = Properties()
            val resourceStream = ApiConfigReader::class.java.classLoader
                .getResourceAsStream("api.properties")
            
            if (resourceStream != null) {
                resourceStream.use { properties.load(it) }
                properties.getProperty(key, defaultValue)
            } else {
                // Fallback: try to read from environment variables
                System.getenv(key.uppercase().replace(".", "_")) 
                    ?: System.getProperty(key, defaultValue)
            }
        } catch (e: Exception) {
            // Fallback to environment variables
            System.getenv(key.uppercase().replace(".", "_")) 
                ?: System.getProperty(key, defaultValue)
        }
    }
}

