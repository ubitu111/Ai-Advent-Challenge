package ru.mirtomsk.shared.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.mirtomsk.shared.network.format.ResponseFormat
import ru.mirtomsk.shared.network.format.ResponseFormatProvider

class SettingsViewModel(
    private val formatProvider: ResponseFormatProvider,
    mainDispatcher: CoroutineDispatcher,
) {
    private val viewmodelScope = CoroutineScope(mainDispatcher + SupervisorJob())
    
    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        // Initialize UI state with current format from provider
        viewmodelScope.launch {
            val currentFormat = formatProvider.responseFormat.first()
            uiState = uiState.copy(responseFormat = formatToString(currentFormat))
        }
    }

    fun setResponseFormat(formatString: String) {
        uiState = uiState.copy(responseFormat = formatString)
        val format = stringToFormat(formatString)
        formatProvider.updateFormat(format)
    }

    private fun formatToString(format: ResponseFormat): String {
        return when (format) {
            ResponseFormat.DEFAULT -> "дефолт"
            ResponseFormat.JSON -> "json"
        }
    }

    private fun stringToFormat(formatString: String): ResponseFormat {
        return when (formatString) {
            "дефолт" -> ResponseFormat.DEFAULT
            "json" -> ResponseFormat.JSON
            else -> ResponseFormat.DEFAULT
        }
    }
}

