package ru.mirtomsk.shared.speech

import androidx.compose.runtime.Composable

/**
 * Service for speech recognition (voice to text)
 * Platform-specific implementations are provided in androidMain and desktopMain
 */
interface SpeechRecognitionService {
    /**
     * Transcribe audio file to text
     * @param audioFilePath Path to the audio file
     * @return Transcribed text or null if recognition failed
     */
    suspend fun transcribeAudio(audioFilePath: String): String?
}

@Composable
expect fun createSpeechRecognitionService(): SpeechRecognitionService
