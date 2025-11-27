package ru.mirtomsk.shared.network.rag

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider for RAG reranking settings
 * Manages the current RAG reranking state and provides it as a Flow
 */
class RagRerankingProvider {
    private val _isRerankingEnabled = MutableStateFlow(false)
    val isRerankingEnabled: StateFlow<Boolean> = _isRerankingEnabled.asStateFlow()

    fun updateRerankingEnabled(enabled: Boolean) {
        _isRerankingEnabled.value = enabled
    }
}
