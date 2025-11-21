package ru.mirtomsk.shared.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.mirtomsk.shared.chat.model.Message
import ru.mirtomsk.shared.chat.model.Message.MessageRole
import ru.mirtomsk.shared.chat.model.MessageContent
import ru.mirtomsk.shared.clipboard.createClipboardHelper
import ru.mirtomsk.shared.di.koinInject
import ru.mirtomsk.shared.settings.SettingsScreen
import ru.mirtomsk.shared.dollarRate.DollarRateScreen
import ru.mirtomsk.shared.dollarRate.DollarRateViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinInject(),
    dollarRateViewModel: DollarRateViewModel = koinInject()
) {
    val uiState = viewModel.uiState
    val dollarRateUiState = dollarRateViewModel.uiState
    val listState = rememberLazyListState()

    // Scroll to bottom when new message is added or loading state changes
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        val itemCount = uiState.messages.size + if (uiState.isLoading) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    // Auto-open dollar rate screen when info is received
    LaunchedEffect(dollarRateUiState.dollarRateInfo) {
        if (dollarRateUiState.dollarRateInfo != null && 
            dollarRateUiState.dollarRateInfo.isNotBlank() && 
            !uiState.isDollarRateOpen) {
            viewModel.openDollarRate()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top App Bar with settings button
            TopAppBar(
                title = { Text("Чат") },
                actions = {
                    TextButton(
                        onClick = { viewModel.openDollarRate() },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "💵",
                            style = MaterialTheme.typography.h6
                        )
                    }
                    TextButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "⚙️",
                            style = MaterialTheme.typography.h6
                        )
                    }
                }
            )

            // Messages list
            LazyColumn(
                modifier = Modifier
                    .imePadding()
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.role == MessageRole.USER) Arrangement.End else Arrangement.Start
                    ) {
                        MessageBubble(message = message)
                    }
                }

                // Show loading bubble if loading
                if (uiState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            LoadingBubble()
                        }
                    }
                }
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::updateInputText,
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter && uiState.inputText.isNotBlank()) {
                                viewModel.sendMessage()
                                true
                            } else {
                                false
                            }
                        },
                    placeholder = { Text("Type a message...") },
                    singleLine = true
                )
                Button(
                    onClick = viewModel::sendMessage,
                    enabled = uiState.inputText.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }

        // Settings modal dialog - overlays on top
        if (uiState.isSettingsOpen) {
            SettingsScreen(
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // Dollar rate modal dialog - overlays on top
        if (uiState.isDollarRateOpen) {
            DollarRateScreen(
                viewModel = dollarRateViewModel,
                onDismiss = { viewModel.closeDollarRate() }
            )
        }
    }
}

@Composable
private fun LoadingBubble() {
    Card(
        modifier = Modifier
            .widthIn(max = 80.dp)
            .padding(horizontal = 4.dp),
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.role == MessageRole.USER

    Card(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .padding(horizontal = 4.dp),
        elevation = 2.dp,
        backgroundColor = if (isUser) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.surface
        }
    ) {
        Column {
            when (val content = message.content) {
                is MessageContent.Text -> {
                    SelectionContainer {
                        Text(
                            text = content.value,
                            modifier = Modifier.padding(12.dp),
                            color = if (isUser) {
                                MaterialTheme.colors.onPrimary
                            } else {
                                MaterialTheme.colors.onSurface
                            }
                        )
                    }
                }

                is MessageContent.Structured -> {
                    if (!isUser) {
                        StructuredMessageContent(
                            content = content,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        // Fallback for user messages with structured content (shouldn't happen)
                        SelectionContainer {
                            Text(
                                text = content.text,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colors.onPrimary
                            )
                        }
                    }
                }
            }

            // Информация о времени выполнения и токенах для сообщений ассистента
            if (!isUser) {
                MessageMetadata(
                    requestTime = message.requestTime,
                    promptTokens = message.promptTokens,
                    completionTokens = message.completionTokens,
                    totalTokens = message.totalTokens,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StructuredMessageContent(
    content: MessageContent.Structured,
    modifier: Modifier = Modifier
) {
    val clipboardHelper = createClipboardHelper()

    SelectionContainer {
        Column(modifier = modifier) {
            // Title - larger and bold, green color
            Text(
                text = content.title,
                style = MaterialTheme.typography.h6.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF4CAF50), // Green color
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Main text - keep as is
            Text(
                text = content.text,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Links section
            if (content.links.isNotEmpty()) {
                Text(
                    text = "Ссылки:",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                content.links.forEach { link ->
                    Text(
                        text = link,
                        style = MaterialTheme.typography.body2,
                        color = Color(0xFF2196F3), // Blue color
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                clipboardHelper.copyToClipboard(link)
                            }
                            .padding(vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = "Ссылки отсутствуют",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Suppress("DefaultLocale")
@Composable
private fun MessageMetadata(
    requestTime: Long,
    promptTokens: Int?,
    completionTokens: Int?,
    totalTokens: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Разделитель
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
        )

        // Заголовок
        Text(
            text = "Информация",
            style = MaterialTheme.typography.body2.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Время выполнения
        val timeText = if (requestTime >= 1000) {
            String.format("%.2f с", requestTime / 1000.0)
        } else {
            "$requestTime мс"
        }
        Text(
            text = "Время выполнения: $timeText",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 2.dp)
        )

        // Токены
        val tokensInfo = buildString {
            append("Промпт: $promptTokens\n")
            append("Ответ: $completionTokens\n")
            append("Всего: $totalTokens")
        }
        Text(
            text = "Токены:\n$tokensInfo",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}
