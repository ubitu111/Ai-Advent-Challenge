# Настройка macOS Speech Framework для распознавания речи

## Обзор

macOS Speech Framework (`SFSpeechRecognizer`) - это нативный API Apple для распознавания речи, который работает офлайн и не требует дополнительных серверов. Он доступен на macOS 10.15+.

## Текущая архитектура проекта

Ваш проект использует **JVM desktop** target, а не Kotlin/Native. Это озºначает, что для использования macOS Speech Framework есть несколько вариантов:

---

## Вариант 1: Добавить Kotlin/Native target для macOS (Рекомендуется для нативного опыта)

### Шаг 1: Обновить build.gradle.kts

Добавьте macOS target в `shared/build.gradle.kts`:

```kotlin
kotlin {
    androidTarget { ... }
    
    jvm("desktop") { ... }
    
    // Добавить macOS target
    macosX64("macos") {
        binaries {
            framework {
                baseName = "Shared"
            }
        }
    }
    
    // Или для Apple Silicon
    macosArm64("macos") {
        binaries {
            framework {
                baseName = "Shared"
            }
        }
    }
    
    sourceSets {
        // ... existing sourceSets ...
        
        val macosMain by creating {
            dependsOn(commonMain)
            // macOS-specific dependencies
        }
    }
}
```

### Шаг 2: Создать macOS реализацию

Создайте файл `shared/src/macosMain/kotlin/ru/mirtomsk/shared/speech/SpeechRecognitionService.macos.kt`:

```kotlin
package ru.mirtomsk.shared.speech

import androidx.compose.runtime.Composable
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Speech.*
import platform.Foundation.*
import platform.AVFoundation.*
import kotlin.coroutines.resume

/**
 * macOS implementation using Speech Framework
 */
class MacOSSpeechRecognitionService : SpeechRecognitionService {
    
    private var speechRecognizer: SFSpeechRecognizer? = null
    private var recognitionRequest: SFSpeechURLRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    
    init {
        // Initialize speech recognizer with Russian and English support
        val locale = NSLocale.localeWithLocaleIdentifier("ru_RU")
        speechRecognizer = SFSpeechRecognizer(locale)
        
        // Also support English
        if (speechRecognizer == null) {
            val englishLocale = NSLocale.localeWithLocaleIdentifier("en_US")
            speechRecognizer = SFSpeechRecognizer(englishLocale)
        }
    }
    
    override suspend fun transcribeAudio(audioFilePath: String): String? {
        return suspendCancellableCoroutine { continuation ->
            // Check authorization
            SFSpeechRecognizer.requestAuthorization { status ->
                when (status) {
                    SFSpeechRecognizerAuthorizationStatusAuthorized -> {
                        performRecognition(audioFilePath, continuation)
                    }
                    else -> {
                        println("Speech recognition authorization denied or not available")
                        continuation.resume(null)
                    }
                }
            }
        }
    }
    
    private fun performRecognition(
        audioFilePath: String,
        continuation: kotlin.coroutines.Continuation<String?>
    ) {
        val audioURL = NSURL.fileURLWithPath(audioFilePath)
        
        recognitionRequest = SFSpeechURLRecognitionRequest(audioURL)
        recognitionRequest?.shouldReportPartialResults = false
        
        recognitionTask = speechRecognizer?.recognitionTaskWithRequest(
            recognitionRequest!!
        ) { result, error ->
            if (error != null) {
                println("Speech recognition error: ${error.localizedDescription}")
                continuation.resume(null)
                return@recognitionTaskWithRequest
            }
            
            if (result != null && result.isFinal) {
                val text = result.bestTranscription.formattedString
                continuation.resume(text)
            }
        }
        
        continuation.invokeOnCancellation {
            recognitionTask?.cancel()
        }
    }
}

@Composable
actual fun createSpeechRecognitionService(): SpeechRecognitionService {
    return MacOSSpeechRecognitionService()
}
```

### Шаг 3: Обновить Info.plist

Добавьте в `Info.plist` вашего macOS приложения:

```xml
<key>NSSpeechRecognitionUsageDescription</key>
<string>This app needs access to speech recognition to transcribe your voice input.</string>
<key>NSMicrophoneUsageDescription</key>
<string>This app needs access to the microphone to record your voice.</string>
```

---

## Вариант 2: Использовать JNA для вызова нативных функций (Для JVM)

Если вы хотите остаться на JVM, можно использовать JNA для вызова нативных функций macOS.

### Шаг 1: Добавить JNA зависимость

В `shared/build.gradle.kts`:

```kotlin
val desktopMain by getting {
    dependencies {
        // ... existing dependencies ...
        
        // JNA for native macOS API calls
        implementation("net.java.dev.jna:jna:5.13.0")
        implementation("net.java.dev.jna:jna-platform:5.13.0")
    }
}
```

### Шаг 2: Создать JNA интерфейс

Создайте файл `shared/src/desktopMain/kotlin/ru/mirtomsk/shared/speech/MacOSSpeechJNA.kt`:

```kotlin
package ru.mirtomsk.shared.speech

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

interface SpeechFramework : Library {
    companion object {
        val INSTANCE: SpeechFramework = Native.load("Speech", SpeechFramework::class.java) as SpeechFramework
    }
    
    // Определите необходимые функции из Speech Framework
    // Это сложно, так как требует знания Objective-C runtime
}
```

**Примечание:** Этот подход сложен, так как требует глубокого понимания Objective-C runtime и не рекомендуется.

---

## Вариант 3: Использовать существующий Whisper API (Текущее решение)

Текущая реализация уже использует Whisper API через HTTP. Это работает на всех платформах, включая macOS.

**Преимущества:**
- Уже реализовано
- Работает на всех платформах
- Не требует нативных зависимостей

**Недостатки:**
- Требует запущенный Whisper сервер
- Не работает офлайн (если сервер не локальный)

---

## Вариант 4: Гибридный подход (Рекомендуется)

Создайте реализацию, которая:
1. **Приоритет 1:** Использует macOS Speech Framework (если доступен на Kotlin/Native)
2. **Приоритет 2:** Использует Whisper API (fallback)

### Обновленная реализация

Обновите `SpeechRecognitionService.desktop.kt`:

```kotlin
class DesktopSpeechRecognitionService(
    private val httpClient: HttpClient,
    private val baseUrl: String? = null,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SpeechRecognitionService {
    
    override suspend fun transcribeAudio(audioFilePath: String): String? {
        // Try macOS Speech Framework first (if available)
        val macosResult = tryMacOSSpeechFramework(audioFilePath)
        if (macosResult != null) {
            return macosResult
        }
        
        // Fallback to Whisper API
        if (baseUrl != null) {
            return transcribeWithWhisperAPI(audioFilePath)
        }
        
        return null
    }
    
    private suspend fun tryMacOSSpeechFramework(audioFilePath: String): String? {
        // Detect if running on macOS
        val osName = System.getProperty("os.name", "").lowercase()
        if (!osName.contains("mac")) {
            return null
        }
        
        // Try to use macOS Speech Framework
        // This would require JNA or Kotlin/Native implementation
        // For now, return null to use Whisper fallback
        return null
    }
    
    // ... existing transcribeWithWhisperAPI method ...
}
```

---

## Рекомендации

### Для текущего проекта (JVM Desktop):

**Используйте Whisper API** - это самое простое и надежное решение:
- Уже реализовано
- Работает на всех платформах
- Хорошее качество распознавания
- Можно настроить локальный сервер для офлайн работы

### Для нативного macOS приложения:

**Добавьте Kotlin/Native target** и используйте macOS Speech Framework:
- Работает офлайн
- Нативная интеграция
- Лучшая производительность
- Не требует дополнительных серверов

---

## Настройка разрешений

### Для Kotlin/Native macOS приложения:

1. Откройте Xcode проект (если используется)
2. Добавьте в `Info.plist`:
   ```xml
   <key>NSSpeechRecognitionUsageDescription</key>
   <string>This app needs access to speech recognition to transcribe your voice input.</string>
   <key>NSMicrophoneUsageDescription</key>
   <string>This app needs access to the microphone to record your voice.</string>
   ```

### Для JVM приложения:

Разрешения запрашиваются автоматически при первом использовании микрофона.

---

## Тестирование

### Проверка доступности Speech Framework

```kotlin
// В Kotlin/Native
val speechRecognizer = SFSpeechRecognizer()
val isAvailable = speechRecognizer?.isAvailable ?: false
println("Speech recognition available: $isAvailable")
```

### Проверка авторизации

```kotlin
SFSpeechRecognizer.requestAuthorization { status ->
    when (status) {
        SFSpeechRecognizerAuthorizationStatusAuthorized -> {
            println("Authorized")
        }
        SFSpeechRecognizerAuthorizationStatusDenied -> {
            println("Denied")
        }
        SFSpeechRecognizerAuthorizationStatusRestricted -> {
            println("Restricted")
        }
        SFSpeechRecognizerAuthorizationStatusNotDetermined -> {
            println("Not determined")
        }
    }
}
```

---

## Поддерживаемые языки

macOS Speech Framework поддерживает множество языков:
- Русский (ru_RU)
- Английский (en_US)
- И многие другие

Список доступных языков можно получить:

```kotlin
val availableLocales = SFSpeechRecognizer.supportedLocales()
availableLocales.forEach { locale ->
    println("Supported locale: ${locale.localeIdentifier}")
}
```

---

## Устранение проблем

### Проблема: Speech Framework не доступен

**Решение:**
- Убедитесь, что macOS версия 10.15 или выше
- Проверьте, что приложение запросило разрешения
- Проверьте настройки приватности в System Preferences

### Проблема: Авторизация отклонена

**Решение:**
- Перейдите в System Preferences > Security & Privacy > Privacy > Speech Recognition
- Разрешите доступ для вашего приложения

### Проблема: Низкое качество распознавания

**Решение:**
- Убедитесь, что используется правильная локаль (ru_RU для русского)
- Проверьте качество аудио (частота дискретизации, формат)
- Используйте хороший микрофон

---

## Сравнение решений

| Решение | Офлайн | Качество | Сложность | Рекомендация |
|---------|--------|----------|-----------|--------------|
| macOS Speech Framework | ✅ | ⭐⭐⭐⭐ | Средняя | Для нативных приложений |
| Whisper API | ⚠️* | ⭐⭐⭐⭐⭐ | Низкая | Для JVM приложений |
| JNA | ✅ | ⭐⭐⭐⭐ | Высокая | Не рекомендуется |

*Whisper может работать офлайн, если сервер запущен локально

---

## Ссылки

- [Apple Speech Framework Documentation](https://developer.apple.com/documentation/speech)
- [Kotlin/Native Apple Framework](https://kotlinlang.org/docs/apple-framework.html)
- [SFSpeechRecognizer Reference](https://developer.apple.com/documentation/speech/sfspeechrecognizer)
