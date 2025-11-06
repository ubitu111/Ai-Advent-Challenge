package ru.mirtomsk.shared.clipboard

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation of ClipboardHelper
 */
class AndroidClipboardHelper(
    private val context: Context
) : ClipboardHelper {
    override fun copyToClipboard(text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
        val clip = ClipData.newPlainText("Copied text", text)
        clipboardManager.setPrimaryClip(clip)
    }
}

/**
 * Create ClipboardHelper for Android
 * Should be called from Composable function to access LocalContext
 */
@Composable
actual fun createClipboardHelper(): ClipboardHelper {
    val context = LocalContext.current
    return AndroidClipboardHelper(context)
}

