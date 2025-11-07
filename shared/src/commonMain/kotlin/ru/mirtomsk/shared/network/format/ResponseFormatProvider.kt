package ru.mirtomsk.shared.network.format

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider for response format settings
 * Manages the current response format and provides it as a Flow
 */
class ResponseFormatProvider {
    private val _responseFormat = MutableStateFlow(ResponseFormat.DEFAULT)
    val responseFormat: StateFlow<ResponseFormat> = _responseFormat.asStateFlow()

    fun updateFormat(format: ResponseFormat) {
        _responseFormat.value = format
    }
}
