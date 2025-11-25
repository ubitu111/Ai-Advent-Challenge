package ru.mirtomsk.shared.embeddings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.mirtomsk.shared.di.koinInject

@Composable
fun EmbeddingsScreen(
    viewModel: EmbeddingsViewModel = koinInject(),
    onDismiss: () -> Unit
) {
    val uiState = viewModel.uiState

    // Modal dialog overlay with semi-transparent background
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent background overlay
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        ) {}

        // Embeddings card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxSize(0.9f)
                .padding(16.dp),
            elevation = 8.dp,
            backgroundColor = MaterialTheme.colors.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = "Генерация эмбеддингов",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Selected file info
                if (uiState.selectedFileName.isNotBlank()) {
                    Text(
                        text = "Выбранный файл: ${uiState.selectedFileName}",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Select file button
                Button(
                    onClick = { viewModel.selectFile() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("Выбрать файл")
                }

                // Generate button
                Button(
                    onClick = { viewModel.generateEmbeddings() },
                    enabled = !uiState.isLoading && uiState.selectedFileName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colors.onPrimary
                        )
                    }
                    Text(if (uiState.isLoading) "Генерация..." else "Сгенерировать эмбеддинги")
                }

                // Success message
                if (uiState.lastProcessedFileName.isNotBlank() && !uiState.isLoading) {
                    Text(
                        text = "Обработка завершена: ${uiState.lastProcessedFileName}",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colors.primary
                    )
                }

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}
