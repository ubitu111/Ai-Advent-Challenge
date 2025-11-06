package ru.mirtomsk.shared.clipboard

import androidx.compose.runtime.Composable

/**
 * Interface for clipboard operations
 * Platform-specific implementations are provided in androidMain and desktopMain
 */
interface ClipboardHelper {
    fun copyToClipboard(text: String)
}

@Composable
expect fun createClipboardHelper(): ClipboardHelper

