package ru.mirtomsk.shared.speech

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for audio recording operations
 * Platform-specific implementations are provided in androidMain and desktopMain
 */
interface AudioRecorder {
    /**
     * State flow indicating if recording is in progress
     */
    val isRecording: StateFlow<Boolean>
    
    /**
     * State flow with current recording duration in milliseconds
     */
    val recordingDuration: StateFlow<Long>
    
    /**
     * Start audio recording
     * @return true if recording started successfully, false otherwise
     */
    suspend fun startRecording(): Boolean
    
    /**
     * Stop audio recording
     * @return Path to the recorded audio file, or null if recording failed
     */
    suspend fun stopRecording(): String?
    
    /**
     * Cancel current recording and delete the file
     */
    suspend fun cancelRecording()
}

@Composable
expect fun createAudioRecorder(): AudioRecorder
