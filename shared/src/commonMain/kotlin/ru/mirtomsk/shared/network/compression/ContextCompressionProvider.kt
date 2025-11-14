package ru.mirtomsk.shared.network.compression

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider for context compression settings
 * Manages the current context compression state and provides it as a Flow
 */
class ContextCompressionProvider {
    private val _isCompressionEnabled = MutableStateFlow(false)
    val isCompressionEnabled: StateFlow<Boolean> = _isCompressionEnabled.asStateFlow()

    fun updateCompressionEnabled(enabled: Boolean) {
        _isCompressionEnabled.value = enabled
    }
}
