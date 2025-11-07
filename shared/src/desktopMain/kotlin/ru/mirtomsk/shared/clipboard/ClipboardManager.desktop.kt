package ru.mirtomsk.shared.clipboard

import androidx.compose.runtime.Composable
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Desktop implementation of ClipboardHelper
 */
class DesktopClipboardHelper : ClipboardHelper {
    override fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }
}

/**
 * Create ClipboardHelper for Desktop
 */
@Composable
actual fun createClipboardHelper(): ClipboardHelper = DesktopClipboardHelper()

