package ru.mirtomsk.shared.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import ru.mirtomsk.shared.chat.model.Message
import ru.mirtomsk.shared.chat.model.Message.MessageRole
import ru.mirtomsk.shared.di.koinInject

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinInject()
) {
    val uiState = viewModel.uiState
    val listState = rememberLazyListState()

    // Scroll to bottom when new message is added or loading state changes
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        val itemCount = uiState.messages.size + if (uiState.isLoading) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
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
        Text(
            text = message.text,
            modifier = Modifier.padding(12.dp),
            color = if (isUser) {
                MaterialTheme.colors.onPrimary
            } else {
                MaterialTheme.colors.onSurface
            }
        )
    }
}
