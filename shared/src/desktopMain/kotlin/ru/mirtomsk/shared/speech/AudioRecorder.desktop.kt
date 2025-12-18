package ru.mirtomsk.shared.speech

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
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
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Desktop implementation of AudioRecorder using Java Sound API
 */
class DesktopAudioRecorder : AudioRecorder {
    private val mutex = Mutex()
    private var targetDataLine: TargetDataLine? = null
    private var outputFile: File? = null
    private var recordingStartTime: Long = 0
    
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _recordingDuration = MutableStateFlow(0L)
    override val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()
    
    private var durationUpdateJob: kotlinx.coroutines.Job? = null
    private var recordingJob: kotlinx.coroutines.Job? = null
    
    override suspend fun startRecording(): Boolean = mutex.withLock {
        if (_isRecording.value) {
            return false
        }
        
        return try {
            // Audio format: 16-bit PCM, 44100 Hz, mono
            val audioFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100.0f,
                16,
                1,
                2,
                44100.0f,
                false
            )
            
            val dataLineInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
            if (!AudioSystem.isLineSupported(dataLineInfo)) {
                return false
            }
            
            targetDataLine = AudioSystem.getLine(dataLineInfo) as TargetDataLine
            targetDataLine!!.open(audioFormat)
            targetDataLine!!.start()
            
            // Create output file
            val tempDir = System.getProperty("java.io.tmpdir")
            val audioDir = File(tempDir, "audio_recordings")
            audioDir.mkdirs()
            outputFile = File(audioDir, "recording_${System.currentTimeMillis()}.wav")
            
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDuration.value = 0L
            
            // Start duration update job
            durationUpdateJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && _isRecording.value) {
                    delay(100)
                    _recordingDuration.update { System.currentTimeMillis() - recordingStartTime }
                }
            }
            
            // Start recording job
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val audioInputStream = javax.sound.sampled.AudioInputStream(targetDataLine)
                    AudioSystem.write(
                        audioInputStream,
                        AudioFileFormat.Type.WAVE,
                        outputFile!!
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
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
            
            targetDataLine?.apply {
                stop()
                close()
            }
            targetDataLine = null
            
            recordingJob?.join()
            recordingJob = null
            
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
            targetDataLine?.apply {
                stop()
                close()
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
        targetDataLine = null
        
        recordingJob?.cancel()
        recordingJob = null
        
        outputFile?.delete()
        outputFile = null
        
        _isRecording.value = false
        _recordingDuration.value = 0L
    }
    
    private fun cleanup() {
        try {
            targetDataLine?.close()
        } catch (e: Exception) {
            // Ignore
        }
        targetDataLine = null
        recordingJob?.cancel()
        recordingJob = null
        outputFile?.delete()
        outputFile = null
        _isRecording.value = false
        _recordingDuration.value = 0L
    }
}

@Composable
actual fun createAudioRecorder(): AudioRecorder {
    return DesktopAudioRecorder()
}
