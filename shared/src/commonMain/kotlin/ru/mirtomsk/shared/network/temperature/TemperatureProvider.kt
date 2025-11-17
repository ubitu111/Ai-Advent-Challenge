package ru.mirtomsk.shared.network.temperature

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider for temperature settings
 * Manages the current temperature value and provides it as a Flow
 */
class TemperatureProvider {
    private val _temperature = MutableStateFlow(0.3f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    fun updateTemperature(temperature: Float) {
        // Clamp temperature between 0 and 1
        _temperature.value = temperature.coerceIn(0f, 1f)
    }
}

