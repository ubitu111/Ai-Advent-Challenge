package ru.mirtomsk.shared.embeddings

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DesktopFilePicker : FilePicker {
    override suspend fun pickFile(): FilePickerResult? {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = FileNameExtensionFilter("Text files", "txt", "md", "json", "xml")
        fileChooser.isAcceptAllFileFilterUsed = true

        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            val content = selectedFile.readText()
            return FilePickerResult(
                filePath = selectedFile.absolutePath,
                fileName = selectedFile.name,
                content = content
            )
        }
        return null
    }
}

actual fun createFilePicker(): FilePicker = DesktopFilePicker()
