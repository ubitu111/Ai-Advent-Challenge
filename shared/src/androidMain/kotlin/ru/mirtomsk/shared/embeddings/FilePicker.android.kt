package ru.mirtomsk.shared.embeddings

/**
 * Android implementation of FilePicker
 * TODO: Implement using Activity Result API or Intent
 */
class AndroidFilePicker : FilePicker {
    override suspend fun pickFile(): FilePickerResult? {
        // TODO: Implement file picker for Android
        // This requires integration with Android's file picker API
        // For now, return null to indicate not implemented
        println("File picker not yet implemented for Android")
        return null
    }
}

actual fun createFilePicker(): FilePicker = AndroidFilePicker()
