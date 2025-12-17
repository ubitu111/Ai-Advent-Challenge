package ru.mirtomsk.shared.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android implementation of SpeechRecognitionService using SpeechRecognizer
 * Note: Android SpeechRecognizer only supports live recognition from microphone,
 * not file-based recognition. For file-based recognition, use Whisper API or similar.
 */
class AndroidSpeechRecognitionService(
    private val context: Context
) : SpeechRecognitionService {
    
    override suspend fun transcribeAudio(audioFilePath: String): String? {
        // Android SpeechRecognizer doesn't support file-based recognition directly
        // It only works with live audio from microphone
        // For file-based recognition, we would need to use Whisper API or similar
        // For now, we'll use live recognition instead
        return startListening()
    }
    
    /**
     * Start live speech recognition from microphone
     * This is the primary method for Android
     */
    suspend fun startListening(): String? = suspendCancellableCoroutine { continuation ->
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        var isCompleted = false
        
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            
            override fun onError(error: Int) {
                if (!isCompleted) {
                    isCompleted = true
                    speechRecognizer.destroy()
                    continuation.resume(null)
                }
            }
            
            override fun onResults(results: Bundle?) {
                if (!isCompleted) {
                    isCompleted = true
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()
                    speechRecognizer.destroy()
                    continuation.resume(text)
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        
        speechRecognizer.setRecognitionListener(listener)
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU,en-US") // Support Russian and English
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        continuation.invokeOnCancellation {
            if (!isCompleted) {
                isCompleted = true
                speechRecognizer.cancel()
                speechRecognizer.destroy()
            }
        }
        
        speechRecognizer.startListening(intent)
    }
}

@Composable
actual fun createSpeechRecognitionService(): SpeechRecognitionService {
    val context = LocalContext.current
    return AndroidSpeechRecognitionService(context)
}
