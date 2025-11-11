package ru.mirtomsk.shared.chat.context

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider for context reset signal
 * Manages context reset events and provides them as a Flow
 */
class ContextResetProvider {
    private val _resetCounter = MutableStateFlow(0L)
    val resetCounter: StateFlow<Long> = _resetCounter.asStateFlow()

    fun resetContext() {
        _resetCounter.value = _resetCounter.value + 1
    }
}
