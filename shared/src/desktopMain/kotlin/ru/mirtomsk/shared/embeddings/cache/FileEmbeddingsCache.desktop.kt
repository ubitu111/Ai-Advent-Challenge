package ru.mirtomsk.shared.embeddings.cache

import java.io.File

class FileDelegate(
    val file: File,
) : CacheFile {
    override fun exists(): Boolean {
        return file.exists()
    }
}

actual fun getCacheFile(cache: FileEmbeddingsCache): CacheFile {
    val userHome = System.getProperty("user.home")
    val cacheDir = File(userHome, ".ai-advent-challenge")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    val file = File(cacheDir, cache.cacheFileName)
    return FileDelegate(file)
}

actual fun readFileContent(file: CacheFile): String {
    @Suppress("UNCHECKED_CAST")
    val delegate = file as FileDelegate
    return delegate.file.readText()
}

actual fun writeFileContent(file: CacheFile, content: String) {
    @Suppress("UNCHECKED_CAST")
    val delegate = file as FileDelegate
    delegate.file.writeText(content)
}

