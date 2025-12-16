package ru.mirtomsk.shared.config

import java.nio.charset.StandardCharsets

/**
 * Reads personalization data from Personalization.md resource file
 */
expect fun getPersonalizationInputStream(path: String): java.io.InputStream?

object PersonalizationReader {
    /**
     * Reads personalization content from Personalization.md file
     * @return Content of Personalization.md file or null if file doesn't exist
     */
    fun readPersonalization(): String? {
        return try {
            val inputStream = getPersonalizationInputStream("Personalization.md")
            inputStream?.use { stream ->
                stream.readBytes().toString(StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }
}
