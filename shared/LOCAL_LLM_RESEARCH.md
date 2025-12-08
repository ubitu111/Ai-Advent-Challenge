# Исследование локальных LLM моделей для MacBook M3 Pro

## Обзор

Данный документ содержит результаты исследования локальных языковых моделей, которые можно развернуть на ноутбуке MacBook M3 Pro для использования вместо Yandex Pro API.

## Рекомендуемые модели для M3 Pro

### 1. Llama 3.1 8B (Рекомендуется для начала)
- **Параметры**: 8 миллиардов
- **Требования RAM**: ~8-12 GB
- **Производительность**: Отличная для большинства задач, быстрая инференция
- **Качество**: Хорошее качество ответов, поддержка русского языка
- **Использование**: Универсальные задачи, программирование, чат

### 2. Mistral 7B
- **Параметры**: 7.3 миллиарда
- **Требования RAM**: ~8-10 GB
- **Производительность**: Высокая скорость, оптимизирована для кода
- **Качество**: Отличные результаты в кодировании и технических задачах
- **Использование**: Программирование, техническая документация

### 3. DeepSeek Coder 7B
- **Параметры**: 7 миллиардов
- **Требования RAM**: ~8-10 GB
- **Производительность**: Специализирована для кода
- **Качество**: Лучшие результаты в генерации и отладке кода
- **Использование**: Разработка, рефакторинг, отладка

### 4. Phi-3 Mini 3.8B
- **Параметры**: 3.8 миллиарда
- **Требования RAM**: ~4-6 GB
- **Производительность**: Очень быстрая, минимальные требования
- **Качество**: Хорошее для простых задач, компактная модель
- **Использование**: Легкие задачи, быстрые ответы

### 5. Gemma 7B
- **Параметры**: 7 миллиардов
- **Требования RAM**: ~8-10 GB
- **Производительность**: Оптимизирована Google для эффективности
- **Качество**: Хорошее качество, поддержка мультиязычности
- **Использование**: Исследования, NLP задачи

### 6. Llama 3.3 70B (Только для 48GB+ RAM)
- **Параметры**: 70 миллиардов
- **Требования RAM**: Минимум 48 GB
- **Производительность**: Медленнее, но лучшее качество
- **Качество**: Превосходное качество ответов
- **Использование**: Сложные задачи, требующие высокого качества

## Инструменты для развертывания

### 1. Ollama (Рекомендуется)

**Преимущества:**
- Простая установка и использование
- OpenAI-совместимый API
- Автоматическое управление моделями
- Поддержка streaming
- Кроссплатформенность

**Установка:**
```bash
# Скачать с https://ollama.com/download/mac
# Или через Homebrew:
brew install ollama

# Запустить сервер
ollama serve

# Скачать модель
ollama pull llama3.1:8b
ollama pull mistral:7b
ollama pull deepseek-coder:7b
```

**API Endpoint:**
- URL: `http://localhost:11434/v1/chat/completions`
- Формат: OpenAI-compatible API
- Streaming: Поддерживается

### 2. LM Studio

**Преимущества:**
- Графический интерфейс
- Удобный выбор и загрузка моделей
- Встроенный чат для тестирования
- OpenAI-compatible API сервер

**Установка:**
1. Скачать с https://lmstudio.ai/
2. Установить приложение
3. Загрузить модель через интерфейс
4. Включить Local Server в разделе Developer

**API Endpoint:**
- URL: `http://localhost:1234/v1/chat/completions`
- Формат: OpenAI-compatible API
- Streaming: Поддерживается

## Интеграция в проект

### Вариант 1: Интеграция через Ollama (Рекомендуется)

**Примечание**: В проекте уже используется `OllamaApiService` для RAG, поэтому инфраструктура частично готова.

#### Шаг 1: Создать новый сервис для локальных моделей

Создать файл `shared/src/commonMain/kotlin/ru/mirtomsk/shared/network/LocalChatApiService.kt`:

```kotlin
package ru.mirtomsk.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto

/**
 * API service for local LLM models (Ollama/LM Studio)
 * Uses OpenAI-compatible API format
 */
class LocalChatApiService(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrl: String = "http://localhost:11434", // Ollama default
    private val modelName: String = "llama3.1:8b",
) {
    
    /**
     * Request local LLM model with streaming response support
     */
    fun requestLocalLlmStream(request: AiRequest): Flow<String> = flow {
        // Convert Yandex format to OpenAI format
        val openAiRequest = convertToOpenAiFormat(request)
        val requestBody = json.encodeToString(
            OpenAiChatRequest.serializer(),
            openAiRequest
        )
        
        println("Local LLM Request: $requestBody")
        
        val response = httpClient.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            // Ollama doesn't require API key, but some clients expect it
            header("Authorization", "Bearer ollama")
            setBody(requestBody)
        }
        
        val responseText = response.bodyAsText()
        
        // Process streaming response (SSE format or JSON lines)
        responseText.lines()
            .filter { it.isNotBlank() }
            .forEach { line ->
                // Remove "data: " prefix if present (SSE format)
                val cleanedLine = line.removePrefix("data: ").trim()
                if (cleanedLine.isNotEmpty() && cleanedLine != "[DONE]") {
                    emit(cleanedLine)
                }
            }
    }
    
    /**
     * Request local LLM model and return the full response
     */
    suspend fun requestLocalLlm(request: AiRequest): String {
        val lines = mutableListOf<String>()
        requestLocalLlmStream(request).collect { line ->
            lines.add(line)
        }
        return lines.joinToString("\n")
    }
    
    /**
     * Convert Yandex GPT format to OpenAI format
     */
    private fun convertToOpenAiFormat(yandexRequest: AiRequest): OpenAiChatRequest {
        val messages = yandexRequest.messages.map { msg ->
            OpenAiMessage(
                role = when (msg.role) {
                    MessageRoleDto.SYSTEM -> "system"
                    MessageRoleDto.USER -> "user"
                    MessageRoleDto.ASSISTANT -> "assistant"
                },
                content = msg.text
            )
        }
        
        // Convert tools if present
        val tools = yandexRequest.tools?.map { tool ->
            OpenAiTool(
                type = tool.type,
                function = OpenAiFunction(
                    name = tool.function.name,
                    description = tool.function.description,
                    parameters = tool.function.parameters?.let { params ->
                        buildJsonObject {
                            put("type", params.type)
                            params.properties?.forEach { (key, prop) ->
                                put(key, buildJsonObject {
                                    prop.type?.let { put("type", it) }
                                    prop.description?.let { put("description", it) }
                                })
                            }
                            params.required?.let { put("required", it) }
                        }
                    }
                )
            )
        }
        
        return OpenAiChatRequest(
            model = modelName,
            messages = messages,
            temperature = yandexRequest.completionOptions.temperature,
            max_tokens = yandexRequest.completionOptions.maxTokens,
            stream = yandexRequest.completionOptions.stream,
            tools = tools
        )
    }
    
    @Serializable
    data class OpenAiChatRequest(
        val model: String,
        val messages: List<OpenAiMessage>,
        val temperature: Float,
        val max_tokens: Int,
        val stream: Boolean,
        val tools: List<OpenAiTool>? = null,
    )
    
    @Serializable
    data class OpenAiMessage(
        val role: String,
        val content: String,
    )
    
    @Serializable
    data class OpenAiTool(
        val type: String,
        val function: OpenAiFunction,
    )
    
    @Serializable
    data class OpenAiFunction(
        val name: String,
        val description: String? = null,
        val parameters: kotlinx.serialization.json.JsonObject? = null,
    )
}
```

#### Шаг 2: Создать маппер для ответов OpenAI формата

Создать файл `shared/src/commonMain/kotlin/ru/mirtomsk/shared/chat/repository/mapper/OpenAiResponseMapper.kt`:

```kotlin
package ru.mirtomsk.shared.chat.repository.mapper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import ru.mirtomsk.shared.chat.repository.model.AiMessage
import ru.mirtomsk.shared.chat.repository.model.AiResponse
import ru.mirtomsk.shared.chat.repository.model.AiToolCall
import ru.mirtomsk.shared.chat.repository.model.AiToolCallFunction
import ru.mirtomsk.shared.chat.repository.model.FunctionCall
import ru.mirtomsk.shared.chat.repository.model.MessageRoleDto
import ru.mirtomsk.shared.network.format.ResponseFormat

/**
 * Mapper for converting OpenAI-compatible API response to AiResponse
 */
class OpenAiResponseMapper(
    private val json: Json
) {
    
    /**
     * Parse OpenAI response format to AiResponse format
     */
    fun mapResponseBody(
        responseBody: String,
        format: ResponseFormat
    ): AiResponse {
        val lines = responseBody.lines().filter { it.isNotBlank() }
        
        var finalResponse: OpenAiChatResponse? = null
        
        // Process streaming response
        lines.forEach { line ->
            try {
                val response = json.decodeFromString<OpenAiChatResponse>(line)
                // Keep the last response
                if (response.choices.isNotEmpty()) {
                    finalResponse = response
                }
            } catch (e: Exception) {
                println("Error deserializing OpenAI response line: $line, error: ${e.message}")
            }
        }
        
        val response = finalResponse ?: throw IllegalStateException("No valid response received")
        
        // Convert to AiResponse format
        val choice = response.choices.firstOrNull()
            ?: throw IllegalStateException("No choices in response")
        
        val message = choice.message
        val role = when (message.role) {
            "system" -> MessageRoleDto.SYSTEM
            "user" -> MessageRoleDto.USER
            "assistant" -> MessageRoleDto.ASSISTANT
            else -> MessageRoleDto.ASSISTANT
        }
        
        // Handle tool calls if present
        val toolCalls = message.tool_calls?.map { toolCall ->
            // Parse function arguments from JSON string
            val argumentsMap = try {
                json.parseToJsonElement(toolCall.function?.arguments ?: "{}").jsonObject
            } catch (e: Exception) {
                buildJsonObject {}
            }
            
            AiToolCall(
                id = toolCall.id,
                type = toolCall.type,
                function = toolCall.function?.let { func ->
                    AiToolCallFunction(
                        name = func.name,
                        arguments = func.arguments
                    )
                },
                functionCall = toolCall.function?.let { func ->
                    FunctionCall(
                        name = func.name,
                        arguments = argumentsMap
                    )
                }
            )
        }
        
        val messageContent = message.content?.let {
            AiMessage.MessageContent.Text(it)
        }
        
        val aiMessage = AiMessage(
            role = role,
            text = messageContent,
            toolCalls = toolCalls,
            toolCallList = toolCalls?.let { 
                ru.mirtomsk.shared.chat.repository.model.ToolCallList(it) 
            }
        )
        
        return AiResponse(
            result = AiResponse.AiResult(
                alternatives = listOf(
                    AiResponse.AiAlternative(
                        message = aiMessage,
                        status = "FINAL"
                    )
                ),
                usage = AiResponse.AiUsage(
                    inputTextTokens = response.usage?.prompt_tokens?.toString() ?: "0",
                    completionTokens = response.usage?.completion_tokens?.toString() ?: "0",
                    totalTokens = response.usage?.total_tokens?.toString() ?: "0",
                    completionTokensDetails = AiResponse.CompletionTokensDetails(
                        reasoningTokens = "0"
                    )
                ),
                modelVersion = response.model ?: "local-model"
            )
        )
    }
    
    @kotlinx.serialization.Serializable
    data class OpenAiChatResponse(
        val id: String? = null,
        val model: String? = null,
        val choices: List<OpenAiChoice>,
        val usage: OpenAiUsage? = null,
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiChoice(
        val index: Int = 0,
        val message: OpenAiMessage,
        val finish_reason: String? = null,
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiMessage(
        val role: String,
        val content: String? = null,
        val tool_calls: List<OpenAiToolCall>? = null,
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiToolCall(
        val id: String,
        val type: String,
        val function: OpenAiFunctionCall? = null,
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiFunctionCall(
        val name: String,
        val arguments: String, // JSON string
    )
    
    @kotlinx.serialization.Serializable
    data class OpenAiUsage(
        val prompt_tokens: Int = 0,
        val completion_tokens: Int = 0,
        val total_tokens: Int = 0,
    )
}
```

#### Шаг 3: Обновить BaseAiAgent для поддержки локальных моделей

Добавить в `BaseAiAgent.kt` возможность выбора между Yandex и локальной моделью:

```kotlin
abstract class BaseAiAgent(
    override val name: String,
    override val systemPrompt: String,
    protected val chatApiService: ChatApiService,
    protected val apiConfig: ApiConfig,
    protected val ioDispatcher: CoroutineDispatcher,
    protected val yandexResponseMapper: AiResponseMapper,
    protected val formatProvider: ResponseFormatProvider,
    protected val temperatureProvider: TemperatureProvider,
    protected val maxTokensProvider: MaxTokensProvider,
    protected val chatCache: ChatCache,
    protected val mcpToolsProvider: McpToolsProvider,
    protected val mcpOrchestrator: McpOrchestrator,
    protected val json: Json,
    // Новые параметры для локальной модели
    private val useLocalModel: Boolean = false,
    private val localChatApiService: LocalChatApiService? = null,
    private val openAiResponseMapper: OpenAiResponseMapper? = null,
) : AiAgent {
    
    // В методе processMessage (около строки 106) заменить:
    // val responseBody = chatApiService.requestYandexGpt(request)
    // val response = yandexResponseMapper.mapResponseBody(responseBody, format)
    
    // На:
    val responseBody = if (useLocalModel && localChatApiService != null) {
        localChatApiService.requestLocalLlm(request)
    } else {
        chatApiService.requestYandexGpt(request)
    }
    
    val response = if (useLocalModel && openAiResponseMapper != null) {
        openAiResponseMapper.mapResponseBody(responseBody, format)
    } else {
        yandexResponseMapper.mapResponseBody(responseBody, format)
    }
    
    // Также обновить в цикле tool calls (около строки 209):
    val followUpResponseBody = if (useLocalModel && localChatApiService != null) {
        localChatApiService.requestLocalLlm(followUpRequest)
    } else {
        chatApiService.requestYandexGpt(followUpRequest)
    }
    
    currentResponse = if (useLocalModel && openAiResponseMapper != null) {
        openAiResponseMapper.mapResponseBody(followUpResponseBody, format)
    } else {
        yandexResponseMapper.mapResponseBody(followUpResponseBody, format)
    }
}
```

#### Шаг 4: Обновить DI модуль

В `AppModule.kt` (файл `shared/src/commonMain/kotlin/ru/mirtomsk/shared/di/AppModule.kt`) добавить в `networkModule`:

```kotlin
val networkModule = module {
    single { NetworkModule.createHttpClient(enableLogging = true) }

    single {
        ChatApiService(
            httpClient = get(),
            apiConfig = get(),
            json = get<Json>(),
        )
    }
    
    // Добавить LocalChatApiService
    single<LocalChatApiService> {
        LocalChatApiService(
            httpClient = get(),
            json = get(),
            baseUrl = "http://localhost:11434", // Ollama default, можно вынести в конфиг
            modelName = "llama3.1:8b" // Можно вынести в конфиг
        )
    }
    
    // Добавить OpenAiResponseMapper
    single<OpenAiResponseMapper> {
        OpenAiResponseMapper(json = get())
    }

    // ... остальной код ...
}
```

Затем обновить создание агентов в том же файле (например, для `SimpleChatAgent` около строки 160):

```kotlin
single<SimpleChatAgent> {
    SimpleChatAgent(
        name = "Simple Chat",
        systemPrompt = Prompts.SIMPLE_CHAT_PROMPT,
        chatApiService = get(),
        apiConfig = get(),
        ioDispatcher = get<DispatchersProvider>().io,
        yandexResponseMapper = get<AiResponseMapper>(),
        formatProvider = get(),
        temperatureProvider = get(),
        maxTokensProvider = get(),
        chatCache = get(),
        mcpToolsProvider = get(),
        mcpOrchestrator = get(),
        json = get(),
        // Новые параметры для локальной модели
        useLocalModel = true, // Включить локальную модель (можно сделать настраиваемым)
        localChatApiService = get(),
        openAiResponseMapper = get(),
    )
}
```

Аналогично обновить другие агенты: `DeveloperAgent`, `CodeReviewAgent`, `BuildAgent`, `SupportAgent`, `DeveloperHelperAgent`.

### Вариант 2: Интеграция через LM Studio

Аналогично варианту 1, но изменить:
- `baseUrl = "http://localhost:1234"` (LM Studio использует порт 1234)
- `modelName` должен соответствовать модели, загруженной в LM Studio

## Конфигурация

### Добавить настройки в ApiConfig

Обновить `shared/src/commonMain/kotlin/ru/mirtomsk/shared/config/ApiConfig.kt`:

```kotlin
interface ApiConfig {
    val apiKey: String
    val keyId: String
    val mcpgateToken: String
    val useLocalModel: Boolean // Новое поле
    val localModelBaseUrl: String // Новое поле
    val localModelName: String // Новое поле
}

class ApiConfigImpl(
    override val apiKey: String,
    override val keyId: String,
    override val mcpgateToken: String,
    override val useLocalModel: Boolean = false,
    override val localModelBaseUrl: String = "http://localhost:11434",
    override val localModelName: String = "llama3.1:8b",
) : ApiConfig
```

Обновить `ApiConfigReader` для чтения новых настроек (если нужно из файла/переменных окружения).

### Пример использования через настройки

```kotlin
// В AppModule использовать настройки
single<LocalChatApiService> {
    val config = get<ApiConfig>()
    LocalChatApiService(
        httpClient = get(),
        json = get(),
        baseUrl = config.localModelBaseUrl,
        modelName = config.localModelName
    )
}

// В создании агентов использовать настройку
single<SimpleChatAgent> {
    val config = get<ApiConfig>()
    SimpleChatAgent(
        // ... existing parameters ...
        useLocalModel = config.useLocalModel,
        localChatApiService = if (config.useLocalModel) get() else null,
        openAiResponseMapper = if (config.useLocalModel) get() else null,
    )
}
```

### Добавить переключатель в Settings

В `shared/src/commonMain/kotlin/ru/mirtomsk/shared/settings/SettingsViewModel.kt` можно добавить:

```kotlin
var useLocalModel by mutableStateOf(false)
    private set

fun toggleLocalModel() {
    useLocalModel = !useLocalModel
    // Сохранить настройку
}
```

## Сравнение производительности

| Модель | RAM | Скорость генерации | Качество | Поддержка функций |
|--------|-----|-------------------|----------|-------------------|
| Llama 3.1 8B | 8-12 GB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ |
| Mistral 7B | 8-10 GB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ |
| DeepSeek Coder 7B | 8-10 GB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ✅ |
| Phi-3 Mini 3.8B | 4-6 GB | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⚠️ Ограниченная |
| Gemma 7B | 8-10 GB | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ✅ |

## Рекомендации

1. **Для начала**: Используйте Llama 3.1 8B через Ollama - лучший баланс качества и производительности
2. **Для кода**: DeepSeek Coder 7B - специализированная модель для программирования
3. **Для скорости**: Phi-3 Mini 3.8B - если нужны быстрые ответы
4. **Для качества**: Llama 3.3 70B (если есть 48GB+ RAM)

## Проверка работы

### Тест Ollama

```bash
# Запустить Ollama
ollama serve

# В другом терминале проверить
curl http://localhost:11434/api/tags

# Протестировать модель
curl http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3.1:8b",
    "messages": [
      {"role": "user", "content": "Привет! Как дела?"}
    ]
  }'
```

### Тест LM Studio

1. Открыть LM Studio
2. Загрузить модель
3. Перейти в раздел "Developer"
4. Включить "Start server"
5. Проверить доступность на `http://localhost:1234/v1/models`

## Примечания

- Локальные модели могут быть медленнее, чем облачные API
- Качество ответов может отличаться от Yandex Pro
- Некоторые модели могут не поддерживать все функции (tool calling)
- Рекомендуется использовать квантованные версии моделей для экономии памяти
- Для production использования рассмотрите возможность кеширования ответов

## Быстрый старт

### Шаг 1: Установить Ollama

```bash
# macOS
brew install ollama
# или скачать с https://ollama.com/download/mac
```

### Шаг 2: Запустить Ollama и скачать модель

```bash
# Запустить сервер (в фоне или отдельном терминале)
ollama serve

# В другом терминале скачать модель
ollama pull llama3.1:8b
```

### Шаг 3: Проверить работу

```bash
# Проверить доступность API
curl http://localhost:11434/api/tags

# Протестировать модель
curl http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama3.1:8b",
    "messages": [
      {"role": "user", "content": "Привет! Как дела?"}
    ],
    "stream": false
  }'
```

### Шаг 4: Интегрировать в проект

1. Создать файлы `LocalChatApiService.kt` и `OpenAiResponseMapper.kt` (см. примеры выше)
2. Обновить `BaseAiAgent.kt` для поддержки локальных моделей
3. Обновить `AppModule.kt` для добавления новых сервисов
4. Обновить `ApiConfig.kt` для добавления настроек
5. Пересобрать проект

### Шаг 5: Переключиться на локальную модель

Установить `useLocalModel = true` в настройках или в `ApiConfig`.

## Примечания по интеграции

- **Существующая инфраструктура**: В проекте уже используется `OllamaApiService` для embeddings, поэтому инфраструктура для работы с Ollama частично готова
- **Совместимость API**: Ollama и LM Studio используют OpenAI-совместимый API, что упрощает интеграцию
- **Tool Calling**: Не все локальные модели поддерживают function calling. Проверьте документацию конкретной модели
- **Производительность**: Локальные модели могут быть медленнее облачных, но обеспечивают приватность и не требуют интернета

## Альтернативные подходы

### Использование существующего OllamaApiService

Можно расширить существующий `OllamaApiService` для поддержки chat completions:

```kotlin
// В OllamaApiService.kt добавить метод
suspend fun chatCompletion(request: ChatRequest): ChatResponse {
    val response: ChatResponse = httpClient.post("$baseUrl/v1/chat/completions") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()
    return response
}
```

### Гибридный подход

Можно использовать локальную модель для простых задач и Yandex Pro для сложных:

```kotlin
val useLocalModel = request.complexity < THRESHOLD
```

## Дополнительные ресурсы

- [Ollama Documentation](https://ollama.com/docs)
- [LM Studio Documentation](https://lmstudio.ai/docs)
- [Hugging Face Models](https://huggingface.co/models)
- [Ollama Model Library](https://ollama.com/library)
- [OpenAI API Format Specification](https://platform.openai.com/docs/api-reference/chat)
