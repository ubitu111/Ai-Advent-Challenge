package ru.mirtomsk.shared.network.prompt

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider for system prompt settings
 * Manages the current system prompt and provides it as a Flow
 */
class SystemPromptProvider {
    private val _systemPrompt = MutableStateFlow(SystemPromptDto.DEFAULT)
    val systemPrompt: StateFlow<SystemPromptDto> = _systemPrompt.asStateFlow()

    fun updateSystemPrompt(systemPrompt: SystemPromptDto) {
        _systemPrompt.value = systemPrompt
    }
}

