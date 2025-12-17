package ru.mirtomsk.shared.config

actual fun getPersonalizationInputStream(path: String): java.io.InputStream? {
    return try {
        // Read from resources via classloader (resources are bundled with the app)
        PersonalizationReader::class.java.classLoader?.getResourceAsStream(path)
    } catch (e: Exception) {
        null
    }
}
