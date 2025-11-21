package ru.mirtomsk.shared.dollarRate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ViewModel for dollar rate screen
 */
class DollarRateViewModel {
    var uiState by mutableStateOf(DollarRateUiState())
        private set

    fun updateDollarRateInfo(info: String?) {
        uiState = uiState.copy(
            dollarRateInfo = info,
            error = null
        )
    }

    fun updateLoading(loading: Boolean) {
        uiState = uiState.copy(isLoading = loading)
    }

    fun updateError(error: String?) {
        uiState = uiState.copy(
            error = error,
            isLoading = false
        )
    }

    fun openScreen() {
        uiState = uiState.copy(isOpen = true)
    }

    fun closeScreen() {
        uiState = uiState.copy(isOpen = false)
    }
}

/**
 * UI state for dollar rate screen
 */
data class DollarRateUiState(
    val dollarRateInfo: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOpen: Boolean = false,
)
