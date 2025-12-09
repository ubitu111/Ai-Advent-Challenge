package ru.mirtomsk.shared.network

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.config.androidContext

/**
 * Android-specific implementation of LocalChatApiService
 * Uses on-device llama.cpp model via JNI
 */
actual fun createLocalChatApiService(
    json: Json,
    baseUrl: String?,
    modelName: String?,
): ILocalChatApiService {
    val context = androidContext
        ?: throw IllegalStateException("Android context not initialized. Call initAndroidContext() first.")
    
    return AndroidLocalChatApiService(
        context = context,
        json = json,
        modelName = modelName ?: "llama-3.1-8b-q4_0.gguf"
    )
}

/**
 * Android implementation that uses on-device llama.cpp model
 */
class AndroidLocalChatApiService(
    private val context: Context,
    private val json: Json,
    private val modelName: String,
) : ILocalChatApiService {
    
    private val llamaWrapper = LlamaJniWrapper()

    private val llamatik = Llama(context)
    private var modelContext: Long? = null
    private var isModelLoaded = false
    
    init {
        // Model will be loaded lazily on first request
    }
    
    private suspend fun ensureModelLoaded() {
        if (!isModelLoaded) {
            loadModel()
        }
    }
    
    private suspend fun loadModel() {
        try {
            // Try to load model from assets first
            val modelPath = try {
                val assetPath = "models/$modelName"
                context.assets.open(assetPath).use { input ->
                    val tempFile = java.io.File(context.cacheDir, modelName)
                    if (!tempFile.exists()) {
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile.absolutePath
                }
            } catch (e: Exception) {
                // If not in assets, try to load from external storage
                val externalModelPath = java.io.File(context.getExternalFilesDir(null), "models/$modelName")
                if (externalModelPath.exists()) {
                    externalModelPath.absolutePath
                } else {
                    throw IllegalStateException(
                        "Model file not found. Please place $modelName in assets/models/ or external storage/models/"
                    )
                }
            }
            
            modelContext = llamaWrapper.initModel(modelPath)
            isModelLoaded = true
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load model: ${e.message}", e)
        }
    }
    
    override fun requestLocalLlmStream(request: AiRequest): Flow<String> = flow {
        val fullResponse = requestLocalLlm(request)
        emit(fullResponse)
    }
    
    override suspend fun requestLocalLlm(request: AiRequest): String {
        ensureModelLoaded()
        
        val context = modelContext
            ?: throw IllegalStateException("Model not loaded")
        
        // Convert AiRequest to prompt format
        val prompt = convertToPrompt(request)
        
        // Generate response using llama.cpp
        val response = llamaWrapper.generate(
            prompt = prompt,
            context = context,
            temperature = request.completionOptions.temperature,
            maxTokens = request.completionOptions.maxTokens
        )
        
        // Convert response to OpenAI-compatible JSON format
        return convertToOpenAiResponse(response, request)
    }
    
    /**
     * Convert AiRequest to text prompt for llama.cpp
     */
    private fun convertToPrompt(request: AiRequest): String {
        val promptBuilder = StringBuilder()
        
        request.messages.forEach { message ->
            when (message.role) {
                ru.mirtomsk.shared.chat.repository.model.MessageRoleDto.SYSTEM -> {
                    promptBuilder.append("System: ${message.text}\n\n")
                }
                ru.mirtomsk.shared.chat.repository.model.MessageRoleDto.USER -> {
                    promptBuilder.append("User: ${message.text}\n\n")
                }
                ru.mirtomsk.shared.chat.repository.model.MessageRoleDto.ASSISTANT -> {
                    promptBuilder.append("Assistant: ${message.text}\n\n")
                }
            }
        }
        
        promptBuilder.append("Assistant: ")
        return promptBuilder.toString()
    }
    
    /**
     * Convert llama.cpp response to OpenAI-compatible JSON format
     */
    private fun convertToOpenAiResponse(
        response: String,
        request: AiRequest
    ): String {
        // Create OpenAI-compatible response format
        val openAiResponse = buildString {
            append("""{"id":"chatcmpl-local","object":"chat.completion","created":""")
            append(System.currentTimeMillis() / 1000)
            append(""","model":"$modelName","choices":[{"index":0,"message":{"role":"assistant","content":""")
            // Escape JSON string
            append(response.replace("\"", "\\\"").replace("\n", "\\n"))
            append(""""},"finish_reason":"stop"}],"usage":{"prompt_tokens":0,"completion_tokens":0,"total_tokens":0}}""")
        }
        
        return openAiResponse
    }
    
    /**
     * Free model resources
     */
    fun unloadModel() {
        modelContext?.let { ctx ->
            llamaWrapper.freeModel(ctx)
            modelContext = null
            isModelLoaded = false
        }
    }
}

/**
 * JNI wrapper for llama.cpp native library
 * This class provides Kotlin interface to native llama.cpp functions
 */
class LlamaJniWrapper {
    companion object {
        private var isNativeLibraryLoaded = false
        
        init {
            try {
                System.loadLibrary("llama")
                isNativeLibraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                // If native library is not available, we'll use a fallback implementation
                println("Warning: llama native library not found. Using fallback implementation.")
                println("Note: To use on-device model, you need to:")
                println("1. Compile llama.cpp for Android using NDK")
                println("2. Place libllama.so in src/main/jniLibs/<abi>/")
                println("3. Place GGUF model in assets/models/ or external storage")
                isNativeLibraryLoaded = false
            }
        }
    }
    
    /**
     * Initialize llama model from file path
     * @param modelPath Path to GGUF model file
     * @return Native context pointer (as Long)
     */
    fun initModel(modelPath: String): Long {
        return if (isNativeLibraryLoaded) {
            initModelNative(modelPath)
        } else {
            initModelFallback(modelPath)
        }
    }
    
    /**
     * Generate text using loaded model
     * @param prompt Input prompt
     * @param context Native context pointer
     * @param temperature Sampling temperature
     * @param maxTokens Maximum tokens to generate
     * @return Generated text
     */
    fun generate(
        prompt: String,
        context: Long,
        temperature: Float = 0.7f,
        maxTokens: Int = 512
    ): String {
        return if (isNativeLibraryLoaded) {
            generateNative(prompt, context, temperature, maxTokens)
        } else {
            generateFallback(prompt, context, temperature, maxTokens)
        }
    }
    
    /**
     * Free model resources
     * @param context Native context pointer
     */
    fun freeModel(context: Long) {
        if (isNativeLibraryLoaded) {
            freeModelNative(context)
        } else {
            freeModelFallback(context)
        }
    }
    
    /**
     * Native JNI methods (will be implemented in C++ via JNI)
     */
    private external fun initModelNative(modelPath: String): Long
    private external fun generateNative(
        prompt: String,
        context: Long,
        temperature: Float,
        maxTokens: Int
    ): String
    private external fun freeModelNative(context: Long)
    
    /**
     * Fallback implementation when native library is not available
     * This allows the app to compile and run even without the native library
     */
    private fun initModelFallback(modelPath: String): Long {
        println("Fallback: initModel called with path: $modelPath")
        throw IllegalStateException(
            "Native llama.cpp library is not available. " +
            "Please compile and include libllama.so to use on-device model inference. " +
            "See README.md in androidApp for instructions."
        )
    }
    
    private fun generateFallback(
        prompt: String,
        context: Long,
        temperature: Float,
        maxTokens: Int
    ): String {
        throw IllegalStateException(
            "Native llama.cpp library is not available. " +
            "Please compile and include libllama.so to use on-device model inference."
        )
    }
    
    private fun freeModelFallback(context: Long) {
        // No-op for fallback
    }
}
