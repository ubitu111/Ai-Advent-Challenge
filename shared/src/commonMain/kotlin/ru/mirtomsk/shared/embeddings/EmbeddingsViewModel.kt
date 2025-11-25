package ru.mirtomsk.shared.embeddings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.mirtomsk.shared.embeddings.cache.EmbeddingsCache
import ru.mirtomsk.shared.embeddings.repository.EmbeddingsRepository

class EmbeddingsViewModel(
    private val repository: EmbeddingsRepository,
    private val cache: EmbeddingsCache,
    private val filePicker: FilePicker,
    mainDispatcher: CoroutineDispatcher,
) {
    private val viewmodelScope = CoroutineScope(mainDispatcher + SupervisorJob())

    var uiState by mutableStateOf(EmbeddingsUiState())
        private set

    fun selectFile() {
        if (uiState.isLoading) return

        viewmodelScope.launch {
            try {
                val fileResult = filePicker.pickFile()
                if (fileResult != null) {
                    uiState = uiState.copy(
                        selectedFileName = fileResult.fileName,
                        selectedFilePath = fileResult.filePath,
                        selectedFileContent = fileResult.content
                    )
                }
            } catch (e: Exception) {
                println("Error selecting file: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun generateEmbeddings() {
        if (uiState.isLoading) return
        if (uiState.selectedFilePath.isBlank() || uiState.selectedFileName.isBlank()) {
            println("No file selected")
            return
        }

        uiState = uiState.copy(isLoading = true)

        viewmodelScope.launch {
            try {
                if (uiState.selectedFileContent.isBlank()) {
                    println("File content is empty")
                    uiState = uiState.copy(isLoading = false)
                    return@launch
                }

                // Обрабатываем текст через репозиторий
                val result = repository.processText(
                    text = uiState.selectedFileContent,
                    fileName = uiState.selectedFileName,
                    filePath = uiState.selectedFilePath
                )

                // Сохраняем результат в кеш
                cache.saveResult(result)

                println("Embeddings processed successfully:")
                println("  File: ${result.metadata.fileName}")
                println("  Total chunks: ${result.metadata.totalChunks}")
                println("  Timestamp: ${result.metadata.timestamp}")
                println("  First chunk embeddings size: ${result.chunks.firstOrNull()?.embeddings?.size ?: 0}")

                uiState = uiState.copy(
                    isLoading = false,
                    lastProcessedFileName = result.metadata.fileName
                )
            } catch (e: Exception) {
                println("Error generating embeddings: ${e.message}")
                e.printStackTrace()
                uiState = uiState.copy(isLoading = false)
            }
        }
    }
}

data class EmbeddingsUiState(
    val isLoading: Boolean = false,
    val selectedFileName: String = "",
    val selectedFilePath: String = "",
    val selectedFileContent: String = "",
    val lastProcessedFileName: String = ""
)
