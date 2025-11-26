package ru.mirtomsk.shared.network.rag

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider for RAG settings
 * Manages the current RAG state and provides it as a Flow
 */
class RagProvider {
    private val _isRagEnabled = MutableStateFlow(false)
    val isRagEnabled: StateFlow<Boolean> = _isRagEnabled.asStateFlow()

    fun updateRagEnabled(enabled: Boolean) {
        _isRagEnabled.value = enabled
    }
}

