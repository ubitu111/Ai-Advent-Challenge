# Локальная LLM модель на Android устройстве

## Обзор

Данный документ описывает подходы для развертывания локальной языковой модели (LLM) непосредственно на Android устройстве, без необходимости использования внешнего API или сервера.

## Быстрый старт

### Текущая реализация

В проекте уже реализована инфраструктура для использования локальной модели на Android:

1. **Android-специфичная реализация** (`LocalChatApiServiceFactory.android.kt`)
   - Использует llama.cpp через JNI
   - Автоматически загружает модель из assets или внешнего хранилища
   - Поддерживает fallback, если нативная библиотека недоступна

2. **JNI обертка** (`LlamaJniWrapper`)
   - Предоставляет Kotlin интерфейс для нативных функций llama.cpp
   - Автоматически определяет наличие нативной библиотеки
   - Выдает понятные ошибки, если библиотека не найдена

3. **DI интеграция**
   - Автоматически выбирает правильную реализацию для платформы
   - Android: on-device модель
   - Desktop: HTTP-based (Ollama)

### Что нужно сделать для полной работы

#### Шаг 1: Компиляция llama.cpp для Android

```bash
# Клонировать llama.cpp
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp

# Установить Android NDK (если еще не установлен)
# Android Studio обычно устанавливает NDK в ~/Library/Android/sdk/ndk/<version>

# Компиляция для arm64-v8a (современные устройства)
mkdir build-android-arm64
cd build-android-arm64
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-21 \
  -DCMAKE_BUILD_TYPE=Release \
  -DBUILD_SHARED_LIBS=ON

cmake --build . --config Release

# Скопировать libllama.so в проект
cp libllama.so ../../../androidApp/src/main/jniLibs/arm64-v8a/
```

#### Шаг 2: Добавить модель в проект

```bash
# Скачать квантованную модель (например, llama-3.1-8b-q4_0.gguf)
# Разместить в androidApp/src/main/assets/models/
mkdir -p androidApp/src/main/assets/models
# Скачать модель и поместить туда
```

Или загрузить модель при первом запуске (см. раздел "Загрузка моделей" ниже).

#### Шаг 3: Настроить конфигурацию

В `local.properties` (или через настройки приложения):

```properties
local.model.enabled=true
local.model.name=llama-3.1-8b-q4_0.gguf
```

#### Шаг 4: Собрать и запустить

```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

### Текущий статус

✅ **Реализовано:**
- Android-специфичная архитектура с expect/actual
- JNI обертка с fallback
- DI интеграция
- Автоматическая загрузка модели из assets
- Конвертация форматов (AiRequest → prompt → OpenAI response)

⚠️ **Требуется:**
- Компиляция llama.cpp для Android (см. инструкции выше)
- Добавление libllama.so в jniLibs
- Добавление GGUF модели в assets или внешнее хранилище

📝 **Примечание:** Приложение будет компилироваться и запускаться даже без нативной библиотеки, но выдаст понятное сообщение об ошибке при попытке использовать локальную модель.

## Варианты реализации

### 1. llama.cpp через JNI (Рекомендуется)

**Преимущества:**
- Высокая производительность благодаря нативной реализации
- Поддержка квантованных моделей (GGUF формат)
- Активное сообщество и хорошая документация
- Оптимизация для мобильных устройств

**Недостатки:**
- Требует компиляции нативного кода через NDK
- Необходимо управление моделями вручную
- Больший размер APK при включении библиотек

**Подход:**
1. Компиляция `llama.cpp` для Android через NDK
2. Создание JNI оберток для вызова нативных функций
3. Загрузка GGUF модели в assets или на внешнее хранилище
4. Интеграция в Kotlin код через JNI

**Ресурсы:**
- [llama.cpp](https://github.com/ggerganov/llama.cpp)
- [llama-jni](https://github.com/shixiangcap/llama-jni)
- [Offline.AI Android](https://github.com/weaktogeek/llama.cpp-android-java)

### 2. Llamatik (Kotlin Multiplatform)

**Преимущества:**
- Готовая Kotlin Multiplatform библиотека
- Упрощенная интеграция
- Поддержка Android, iOS, Desktop
- Не требует работы с JNI напрямую

**Недостатки:**
- Меньше контроля над настройками
- Зависимость от сторонней библиотеки
- Может быть менее оптимизирована для конкретных случаев

**Ресурсы:**
- [Llamatik](https://llamatik.com/)
- [Llamatik GitHub](https://github.com/llamatik/llamatik)

### 3. TensorFlow Lite

**Преимущества:**
- Официальная поддержка Google
- Хорошая интеграция с Android
- Оптимизация для мобильных устройств

**Недостатки:**
- Ограниченная поддержка LLM моделей
- Требует конвертации моделей в TFLite формат
- Меньше готовых LLM моделей

## Рекомендуемый подход: llama.cpp через JNI

### Архитектура решения

```
┌─────────────────────────────────────┐
│   Kotlin/Kotlin Multiplatform      │
│   (Shared Module)                   │
│                                     │
│   LocalChatApiService               │
│   (Android Implementation)          │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   Android Native Layer              │
│   (JNI Bindings)                    │
│                                     │
│   LlamaJniWrapper.kt                │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   Native C++ (llama.cpp)            │
│   (compiled .so library)            │
│                                     │
│   libllama.so                       │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   GGUF Model File                   │
│   (stored in assets or storage)    │
│                                     │
│   model.gguf                        │
└─────────────────────────────────────┘
```

### Шаги реализации

#### 1. Подготовка нативных библиотек

**Вариант A: Использование готовых библиотек**

Скачать предкомпилированные `.so` файлы для разных архитектур:
- `arm64-v8a` (современные Android устройства)
- `armeabi-v7a` (старые устройства)
- `x86_64` (эмуляторы)

**Вариант B: Компиляция через NDK**

```bash
# Клонировать llama.cpp
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp

# Компиляция для Android
mkdir build-android
cd build-android
cmake .. -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
         -DANDROID_ABI=arm64-v8a \
         -DANDROID_PLATFORM=android-21 \
         -DCMAKE_BUILD_TYPE=Release

cmake --build . --config Release
```

#### 2. Структура проекта

```
androidApp/
├── src/
│   └── main/
│       ├── java/
│       │   └── ru/mirtomsk/androidapp/
│       │       └── llama/
│       │           └── LlamaJniWrapper.kt
│       ├── jniLibs/
│       │   ├── arm64-v8a/
│       │   │   └── libllama.so
│       │   ├── armeabi-v7a/
│       │   │   └── libllama.so
│       │   └── x86_64/
│       │       └── libllama.so
│       └── assets/
│           └── models/
│               └── llama-3.1-8b-q4_0.gguf
└── build.gradle.kts
```

#### 3. JNI обертка

Создать класс `LlamaJniWrapper.kt` для взаимодействия с нативной библиотекой:

```kotlin
package ru.mirtomsk.androidapp.llama

class LlamaJniWrapper {
    companion object {
        init {
            System.loadLibrary("llama")
        }
    }
    
    external fun initModel(modelPath: String): Long
    external fun generate(prompt: String, context: Long): String
    external fun freeModel(context: Long)
}
```

#### 4. Android-специфичная реализация LocalChatApiService

Создать `LocalChatApiService.android.kt` в shared модуле:

```kotlin
// shared/src/androidMain/kotlin/ru/mirtomsk/shared/network/LocalChatApiService.android.kt
package ru.mirtomsk.shared.network

import android.content.Context
import ru.mirtomsk.androidapp.llama.LlamaJniWrapper
import ru.mirtomsk.shared.chat.repository.model.AiRequest

class AndroidLocalChatApiService(
    private val context: Context,
    private val modelPath: String = "models/llama-3.1-8b-q4_0.gguf"
) : LocalChatApiService {
    private val llamaWrapper = LlamaJniWrapper()
    private var modelContext: Long? = null
    
    init {
        // Загрузить модель при инициализации
        val assetPath = context.assets.open(modelPath)
        val tempFile = File(context.cacheDir, "model.gguf")
        assetPath.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        modelContext = llamaWrapper.initModel(tempFile.absolutePath)
    }
    
    override suspend fun requestLocalLlm(request: AiRequest): String {
        val context = modelContext ?: throw IllegalStateException("Model not loaded")
        val prompt = convertToPrompt(request)
        return llamaWrapper.generate(prompt, context)
    }
    
    private fun convertToPrompt(request: AiRequest): String {
        // Конвертировать AiRequest в текстовый промпт
        // ...
    }
}
```

#### 5. Обновление DI модуля

В `AppModule.kt` добавить Android-специфичную реализацию:

```kotlin
val networkModule = module {
    // ...
    
    // Android-специфичная реализация для локальной модели
    single<LocalChatApiService>(named("androidLocal")) {
        val context = get<Context>()
        AndroidLocalChatApiService(
            context = context,
            modelPath = "models/llama-3.1-8b-q4_0.gguf"
        )
    }
    
    // Выбор реализации в зависимости от платформы
    single<LocalChatApiService> {
        if (Platform.isAndroid()) {
            get(named("androidLocal"))
        } else {
            // Desktop реализация через HTTP
            get(named("httpLocal"))
        }
    }
}
```

### Рекомендуемые модели для Android

#### 1. Llama 3.1 8B Q4_0 (Рекомендуется)
- **Размер**: ~4.5 GB
- **RAM**: 6-8 GB
- **Качество**: ⭐⭐⭐⭐
- **Скорость**: ⭐⭐⭐⭐
- **Использование**: Универсальные задачи

#### 2. Phi-3 Mini 3.8B Q4_0
- **Размер**: ~2.3 GB
- **RAM**: 4-6 GB
- **Качество**: ⭐⭐⭐
- **Скорость**: ⭐⭐⭐⭐⭐
- **Использование**: Быстрые ответы, ограниченная память

#### 3. Mistral 7B Q4_0
- **Размер**: ~4.1 GB
- **RAM**: 6-8 GB
- **Качество**: ⭐⭐⭐⭐
- **Скорость**: ⭐⭐⭐⭐
- **Использование**: Технические задачи, код

#### 4. Gemma 2B Q4_0
- **Размер**: ~1.4 GB
- **RAM**: 3-4 GB
- **Качество**: ⭐⭐⭐
- **Скорость**: ⭐⭐⭐⭐⭐
- **Использование**: Легкие задачи, старые устройства

### Загрузка моделей

#### Вариант 1: Включить в APK (assets)

**Преимущества:**
- Модель всегда доступна
- Не требует загрузки

**Недостатки:**
- Увеличивает размер APK
- Невозможно обновить без переустановки

**Реализация:**
```kotlin
// В build.gradle.kts
android {
    sourceSets {
        main {
            assets.srcDirs("src/main/assets")
        }
    }
}

// Скопировать модель в androidApp/src/main/assets/models/
```

#### Вариант 2: Загрузка при первом запуске

**Преимущества:**
- Меньший размер APK
- Возможность обновления модели
- Выбор модели пользователем

**Недостатки:**
- Требует интернет для первой загрузки
- Необходимо управление загрузкой

**Реализация:**
```kotlin
class ModelDownloader(private val context: Context) {
    suspend fun downloadModel(
        url: String,
        destination: File
    ): File = withContext(Dispatchers.IO) {
        val client = HttpClient()
        val response = client.get(url)
        destination.outputStream().use { output ->
            response.bodyAsChannel().copyTo(output)
        }
        destination
    }
}
```

### Оптимизация производительности

#### 1. Использование квантованных моделей

Квантованные модели (Q4_0, Q4_1, Q5_0) значительно уменьшают размер и требования к памяти:

```
llama-3.1-8b.gguf          ~15 GB (FP16)
llama-3.1-8b-q4_0.gguf     ~4.5 GB (Q4_0)
llama-3.1-8b-q8_0.gguf     ~8.5 GB (Q8_0)
```

#### 2. Настройка параметров инференса

```kotlin
class LlamaConfig {
    val nThreads = 4  // Количество потоков
    val nCtx = 2048   // Размер контекста
    val nBatch = 512  // Размер батча
    val nGpuLayers = 0 // Использование GPU (если доступно)
}
```

#### 3. Кеширование контекста

Избегать перезагрузки модели для каждого запроса:

```kotlin
class ModelManager {
    private var modelContext: Long? = null
    
    suspend fun getModel(): Long {
        if (modelContext == null) {
            modelContext = loadModel()
        }
        return modelContext!!
    }
}
```

### Управление памятью

#### Проверка доступной памяти

```kotlin
class MemoryManager(private val context: Context) {
    fun getAvailableMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem
    }
    
    fun canLoadModel(modelSize: Long): Boolean {
        return getAvailableMemory() > modelSize * 2 // Запас в 2 раза
    }
}
```

#### Освобождение памяти

```kotlin
class ModelManager {
    fun unloadModel() {
        modelContext?.let { context ->
            llamaWrapper.freeModel(context)
            modelContext = null
            System.gc() // Принудительная сборка мусора
        }
    }
}
```

### Обработка ошибок

```kotlin
sealed class LocalModelError : Exception() {
    object ModelNotFound : LocalModelError()
    object InsufficientMemory : LocalModelError()
    object ModelLoadFailed : LocalModelError()
    data class GenerationError(val message: String) : LocalModelError()
}

class AndroidLocalChatApiService {
    suspend fun requestLocalLlm(request: AiRequest): String {
        return try {
            val context = modelContext ?: throw LocalModelError.ModelNotFound
            llamaWrapper.generate(convertToPrompt(request), context)
        } catch (e: OutOfMemoryError) {
            throw LocalModelError.InsufficientMemory
        } catch (e: Exception) {
            throw LocalModelError.GenerationError(e.message ?: "Unknown error")
        }
    }
}
```

### Тестирование

#### Unit тесты

```kotlin
@Test
fun testModelLoading() {
    val context = InstrumentationRegistry.getInstrumentation().context
    val service = AndroidLocalChatApiService(context)
    assertNotNull(service.modelContext)
}
```

#### Интеграционные тесты

```kotlin
@Test
fun testModelGeneration() = runTest {
    val context = InstrumentationRegistry.getInstrumentation().context
    val service = AndroidLocalChatApiService(context)
    val request = AiRequest(/* ... */)
    val response = service.requestLocalLlm(request)
    assertTrue(response.isNotEmpty())
}
```

### Альтернативный подход: Llamatik

Если не хотите работать с JNI напрямую, можно использовать готовую библиотеку Llamatik:

#### Добавление зависимости

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.llamatik:llamatik-android:1.0.0")
}
```

#### Использование

```kotlin
import com.llamatik.Llamatik

class AndroidLocalChatApiService(
    private val context: Context
) {
    private val llamatik = Llamatik(context)
    
    suspend fun requestLocalLlm(request: AiRequest): String {
        val model = llamatik.loadModel("models/llama-3.1-8b-q4_0.gguf")
        val prompt = convertToPrompt(request)
        return model.generate(prompt)
    }
}
```

## Сравнение подходов

| Критерий | llama.cpp + JNI | Llamatik | TensorFlow Lite |
|----------|----------------|----------|-----------------|
| Производительность | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| Сложность интеграции | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Контроль | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| Размер APK | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Поддержка моделей | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |

## Рекомендации

1. **Для начала**: Используйте Llamatik для быстрой интеграции
2. **Для production**: Используйте llama.cpp + JNI для максимальной производительности
3. **Для экспериментов**: Начните с небольшой модели (Phi-3 Mini или Gemma 2B)
4. **Для production**: Используйте Llama 3.1 8B Q4_0 для баланса качества и производительности

## Дополнительные ресурсы

- [llama.cpp GitHub](https://github.com/ggerganov/llama.cpp)
- [llama.cpp Android Examples](https://github.com/ggerganov/llama.cpp/tree/master/examples/android)
- [Llamatik Documentation](https://llamatik.com/docs)
- [GGUF Model Format](https://github.com/ggerganov/ggml/blob/master/docs/gguf.md)
- [Android NDK Documentation](https://developer.android.com/ndk)

## Примечания

- Локальные модели требуют значительного объема памяти (минимум 4-6 GB RAM)
- Производительность зависит от мощности устройства
- Рекомендуется использовать квантованные модели для экономии памяти
- Для лучшей производительности используйте устройства с 8+ GB RAM
- Модели можно хранить на внешнем хранилище для экономии места в APK
