package ru.mirtomsk.shared.embeddings

/**
 * Интерфейс для выбора файла из системы
 */
interface FilePicker {
    /**
     * Открыть диалог выбора файла
     * @return Pair<путь к файлу, содержимое файла> или null, если файл не выбран
     */
    suspend fun pickFile(): FilePickerResult?
}

data class FilePickerResult(
    val filePath: String,
    val fileName: String,
    val content: String
)

/**
 * Создать FilePicker (expect функция)
 */
expect fun createFilePicker(): FilePicker
