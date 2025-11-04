package ru.mirtomsk.shared.config

actual fun getResourceInputStream(path: String): java.io.InputStream? {
    return ApiConfigReader::class.java.classLoader.getResourceAsStream(path)
}

