package ru.mirtomsk.shared.config

actual fun getPersonalizationInputStream(path: String): java.io.InputStream? {
    return PersonalizationReader::class.java.classLoader.getResourceAsStream(path)
}
