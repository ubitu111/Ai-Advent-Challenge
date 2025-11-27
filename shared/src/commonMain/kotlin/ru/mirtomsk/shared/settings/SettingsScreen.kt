package ru.mirtomsk.shared.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ru.mirtomsk.shared.di.koinInject
import ru.mirtomsk.shared.settings.model.AgentType
import ru.mirtomsk.shared.settings.model.SystemPrompt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinInject(),
    onDismiss: () -> Unit
) {
    val uiState = viewModel.uiState
    var expanded by remember { mutableStateOf(false) }

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

        // Settings card
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
                    .padding(24.dp)
            ) {
                // Fixed header
                Text(
                    text = Strings.SETTINGS_TITLE,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Scrollable content
                val density = LocalDensity.current
                var spinnerWidth by remember { mutableStateOf(0.dp) }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Response Format section
                    Text(
                        text = Strings.RESPONSE_FORMAT_TITLE,
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Spinner-like dropdown
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    spinnerWidth = with(density) { coordinates.size.width.toDp() }
                                }
                                .clickable(onClick = { expanded = true })
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colors.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.responseFormat,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.body1
                                )
                                Text(Strings.DROPDOWN_ARROW)
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = if (spinnerWidth > 0.dp) Modifier.width(spinnerWidth) else Modifier.fillMaxWidth()
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    viewModel.setResponseFormat(Strings.DEFAULT_FORMAT)
                                    expanded = false
                                }
                            ) {
                                Text(Strings.DEFAULT_FORMAT)
                            }
                            DropdownMenuItem(
                                onClick = {
                                    viewModel.setResponseFormat(Strings.JSON_FORMAT)
                                    expanded = false
                                }
                            ) {
                                Text(Strings.JSON_FORMAT)
                            }
                        }
                    }

                    // Temperature section
                    Text(
                        text = Strings.TEMPERATURE_TITLE,
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.temperature,
                        onValueChange = { viewModel.setTemperature(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(Strings.TEMPERATURE_PLACEHOLDER) }
                    )

                    // Max Tokens section
                    Text(
                        text = Strings.MAX_TOKENS_TITLE,
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = uiState.maxTokens,
                        onValueChange = { viewModel.setMaxTokens(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(Strings.MAX_TOKENS_PLACEHOLDER) }
                    )

                    // Context Compression section
                    Text(
                        text = Strings.CONTEXT_COMPRESSION_TITLE,
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    // Radio buttons group for context compression
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setCompressionEnabled(true) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.isCompressionEnabled,
                                onClick = null
                            )
                            Text(
                                text = Strings.COMPRESSION_ENABLED,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.body1
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setCompressionEnabled(false) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !uiState.isCompressionEnabled,
                                onClick = null
                            )
                            Text(
                                text = Strings.COMPRESSION_DISABLED,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.body1
                            )
                        }
                    }

                    // RAG section
                    Text(
                        text = Strings.RAG_TITLE,
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    // Radio buttons group for RAG
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setRagEnabled(true) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.isRagEnabled,
                                onClick = null
                            )
                            Text(
                                text = Strings.RAG_ENABLED,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.body1
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setRagEnabled(false) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !uiState.isRagEnabled,
                                onClick = null
                            )
                            Text(
                                text = Strings.RAG_DISABLED,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.body1
                            )
                        }
                    }

                    // Agent Selection section
                    Text(
                        text = Strings.AGENT_SELECTION_TITLE,
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    // Radio buttons group for agent type
                    Column {
                        AgentType.entries.forEach { agentType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setSelectedAgent(agentType) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.selectedAgent == agentType,
                                    onClick = null
                                )
                                Text(
                                    text = Strings.getAgentName(agentType),
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.body1
                                )
                            }
                        }
                    }

                    // System Prompt Selection section
                    Text(
                        text = Strings.SYSTEM_PROMPT_SELECTION_TITLE,
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    // Radio buttons group for system prompt
                    Column {
                        SystemPrompt.entries.forEach { systemPrompt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setSelectedSystemPrompt(systemPrompt) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.selectedSystemPrompt == systemPrompt,
                                    onClick = null
                                )
                                Text(
                                    text = Strings.getSystemPromptName(systemPrompt),
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.body1
                                )
                            }
                        }
                    }

                }

                // Fixed buttons at bottom
                Button(
                    onClick = { viewModel.resetContext() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(Strings.RESET_CONTEXT_BUTTON)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(Strings.CLOSE_BUTTON)
                }
            }
        }
    }
}

