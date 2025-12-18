package ru.mirtomsk.shared.speech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Android implementation of AudioRecorder
 */
class AndroidAudioRecorder(
    private val context: Context
) : AudioRecorder {
    private val mutex = Mutex()
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordingStartTime: Long = 0
    
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    override val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()
    
    private var durationUpdateJob: kotlinx.coroutines.Job? = null
    
    override suspend fun startRecording(): Boolean = mutex.withLock {
        if (_isRecording.value) {
            return false
        }
        
        // Check permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        
        return try {
            // Create output file
            val cacheDir = context.cacheDir
            val audioDir = File(cacheDir, "audio_recordings")
            audioDir.mkdirs()
            outputFile = File(audioDir, "recording_${System.currentTimeMillis()}.m4a")
            
            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }
            
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDuration.value = 0L
            
            // Start duration update job
            durationUpdateJob = kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
                while (isActive && _isRecording.value) {
                    delay(100)
                    _recordingDuration.update { System.currentTimeMillis() - recordingStartTime }
                }
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
            false
        }
    }
    
    override suspend fun stopRecording(): String? = mutex.withLock {
        if (!_isRecording.value) {
            return null
        }
        
        return try {
            durationUpdateJob?.cancel()
            durationUpdateJob = null
            
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            val filePath = outputFile?.absolutePath
            _isRecording.value = false
            _recordingDuration.value = 0L
            
            if (filePath != null && File(filePath).exists()) {
                filePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
            null
        }
    }
    
    override suspend fun cancelRecording() = mutex.withLock {
        durationUpdateJob?.cancel()
        durationUpdateJob = null
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
        mediaRecorder = null
        
        outputFile?.delete()
        outputFile = null
        
        _isRecording.value = false
        _recordingDuration.value = 0L
    }
    
    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
        outputFile?.delete()
        outputFile = null
        _isRecording.value = false
        _recordingDuration.value = 0L
    }
}

@Composable
actual fun createAudioRecorder(): AudioRecorder {
    val context = LocalContext.current
    return AndroidAudioRecorder(context)
}
