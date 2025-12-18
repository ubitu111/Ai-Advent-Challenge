package ru.mirtomsk.shared.speech

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.di.koinInject
import java.io.File

/**
 * Desktop implementation of SpeechRecognitionService
 * Uses Whisper API through OpenAI-compatible endpoint (OWhisper, Speaches, etc.)
 * 
 * Note: Ollama does not natively support Whisper models.
 * Use a separate Whisper server (OWhisper, Speaches) that provides OpenAI-compatible API.
 * 
 * Future enhancement: For macOS, consider adding Kotlin/Native target to use
 * macOS Speech Framework (SFSpeechRecognizer) for native offline speech recognition.
 * See MACOS_SPEECH_FRAMEWORK_SETUP.md for implementation details.
 */
class DesktopSpeechRecognitionService(
    private val httpClient: HttpClient,
    private val baseUrl: String? = null, // Whisper API endpoint (e.g., http://127.0.0.1:8000)
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SpeechRecognitionService {
    
    override suspend fun transcribeAudio(audioFilePath: String): String? {
        if (baseUrl == null) {
            println("Whisper base URL is not configured")
            return null
        }
        
        return transcribeWithWhisperAPI(audioFilePath)
    }
    
    /**
     * Transcribe using OpenAI-compatible Whisper API
     * Works with OWhisper, Speaches, or any OpenAI-compatible Whisper server
     * 
     * Setup instructions: See WHISPER_SETUP.md
     */
    private suspend fun transcribeWithWhisperAPI(audioFilePath: String): String? {
        return try {
            val audioFile = File(audioFilePath)
            if (!audioFile.exists()) {
                println("Audio file does not exist: $audioFilePath")
                return null
            }
            
            // Read audio file as bytes
            val audioBytes = audioFile.readBytes()
            
            // Determine file extension for content type
            val fileExtension = audioFile.extension.lowercase()
            val contentType = when (fileExtension) {
                "wav" -> "audio/wav"
                "mp3" -> "audio/mpeg"
                "m4a" -> "audio/mp4"
                "ogg" -> "audio/ogg"
                else -> "audio/wav" // Default
            }
            
            // Use OpenAI-compatible API endpoint
            // Endpoint: /v1/audio/transcriptions
            val response: WhisperResponse = httpClient.post("$baseUrl/v1/audio/transcriptions") {
                contentType(ContentType.MultiPart.FormData)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", audioBytes, Headers.build {
                                append(HttpHeaders.ContentType, contentType)
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"audio.$fileExtension\"")
                            })
                            append("model", "whisper-1")  // OpenAI-compatible model name
                            append("language", "ru")  // Optional: specify language for better accuracy
                        }
                    )
                )
            }.body()
            
            response.text
        } catch (e: Exception) {
            println("Error transcribing with Whisper API: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    @Serializable
    private data class WhisperResponse(
        val text: String
    )
}

@Composable
actual fun createSpeechRecognitionService(): SpeechRecognitionService {
    val httpClient: HttpClient = koinInject()
    val apiConfig: ApiConfig = koinInject()
    
    // Use Whisper server URL (default: http://127.0.0.1:8000)
    // Note: This is separate from Ollama URL
    // To configure, add whisper.base.url to local.properties
    // For setup instructions, see WHISPER_SETUP.md
    // 
    // If server uses different port (8001, 8002, etc.), update this URL
    // You can also check which port is used by running: lsof -i :8000
    val whisperBaseUrl = System.getenv("WHISPER_BASE_URL") 
        ?: "http://127.0.0.1:8000"  // Default Whisper server URL
    
    return DesktopSpeechRecognitionService(
        httpClient = httpClient,
        baseUrl = whisperBaseUrl
    )
}
