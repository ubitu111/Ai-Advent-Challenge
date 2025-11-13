package ru.mirtomsk.shared.network.tokens

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider for max tokens settings
 * Manages the current max tokens value and provides it as a Flow
 */
class MaxTokensProvider {
    private val _maxTokens = MutableStateFlow(2000)
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    fun updateMaxTokens(maxTokens: Int) {
        // Clamp max tokens between 1 and reasonable maximum (e.g., 100000)
        _maxTokens.value = maxTokens.coerceIn(1, 100000)
    }
}

