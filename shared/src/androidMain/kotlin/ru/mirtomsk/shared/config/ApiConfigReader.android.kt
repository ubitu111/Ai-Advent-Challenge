package ru.mirtomsk.shared.config

import android.content.Context

// We'll need to initialize context from Android app
var androidContext: Context? = null

fun initAndroidContext(context: Context) {
    androidContext = context.applicationContext
}

actual fun getResourceInputStream(path: String): java.io.InputStream? {
    return try {
        androidContext?.assets?.open(path) ?: run {
            // Fallback: try to read from classloader
            ApiConfigReader::class.java.classLoader?.getResourceAsStream(path)
        }
    } catch (e: Exception) {
        null
    }
}

