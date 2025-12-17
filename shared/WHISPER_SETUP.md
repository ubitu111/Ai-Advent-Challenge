# Настройка Whisper для распознавания речи

## Важно: Ollama и Whisper

**Ollama официально не поддерживает модели Whisper напрямую.** Ollama предназначен для больших языковых моделей (LLM), а не для распознавания речи.

Для использования Whisper с вашим приложением есть несколько вариантов:

---

## Вариант 1: Использование отдельного Whisper сервера (Рекомендуется)

### 1.1. Установка OWhisper (OpenAI-совместимый API)

OWhisper - это локальный сервер для Whisper с поддержкой OpenAI-совместимого API.

**⚠️ Важно:** OWhisper доступен только через Docker, не через pip/pipx.

**Установка через Docker:**
```bash
# Скачать образ
docker pull hyprnote/owhisper:latest

# Или альтернативный образ
docker pull ghcr.io/0x00001f/owhisper:latest

# Запустить сервер
docker run -d \
  --name owhisper \
  -p 8000:8000 \
  -v $(pwd)/models:/app/models \
  hyprnote/owhisper:latest
```

**Проверка работы:**
```bash
# Проверить статус контейнера
docker ps | grep owhisper

# Проверить health endpoint (если доступен)
curl http://localhost:8000/health

# Или проверить основной endpoint
curl http://localhost:8000/v1/models
```

### 1.2. Альтернатива: Простой Python Whisper сервер (Рекомендуется для быстрого старта)

Если Docker недоступен, можно использовать простой Python сервер с OpenAI-совместимым API.

**Установка:**

⚠️ **Важно:** Используйте `pip` или `pip3`, а не `brew` для установки Python пакетов!

```bash
# Проверьте, какой Python используется
which python3
python3 --version

# Установить зависимости через pip
pip3 install fastapi uvicorn openai-whisper python-multipart

# Или использовать faster-whisper (быстрее, рекомендуется)
pip3 install fastapi uvicorn faster-whisper python-multipart

# Если pip3 не найден, попробуйте:
python3 -m pip install fastapi uvicorn openai-whisper python-multipart
```

**Рекомендуется использовать виртуальное окружение:**

```bash
# Создать виртуальное окружение
python3 -m venv whisper_env

# Активировать (macOS/Linux)
source whisper_env/bin/activate

# Установить зависимости
pip install fastapi uvicorn openai-whisper python-multipart

# Запустить сервер
python whisper_server.py

# Деактивировать окружение (когда закончите)
deactivate
```

**Создать файл `whisper_server.py`:**
```python
from fastapi import FastAPI, File, UploadFile, Form
from fastapi.responses import JSONResponse
import whisper
import tempfile
import os

app = FastAPI()

# Загрузить модель при старте (используйте "base" для быстрого старта)
model = whisper.load_model("base")

@app.post("/v1/audio/transcriptions")
async def transcribe_audio(
    file: UploadFile = File(...),
    model: str = Form("whisper-1"),
    language: str = Form(None)
):
    try:
        # Сохранить временный файл
        with tempfile.NamedTemporaryFile(delete=False, suffix='.wav') as tmp_file:
            content = await file.read()
            tmp_file.write(content)
            tmp_path = tmp_file.name
        
        # Транскрибировать
        result = model.transcribe(tmp_path, language=language)
        
        # Удалить временный файл
        os.unlink(tmp_path)
        
        return JSONResponse(content={"text": result["text"]})
    except Exception as e:
        return JSONResponse(
            status_code=500,
            content={"error": str(e)}
        )

@app.get("/health")
async def health():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

**Запуск:**
```bash
python whisper_server.py
```

**Или с faster-whisper (быстрее):**
```python
from faster_whisper import WhisperModel

# Вместо whisper.load_model используйте:
model = WhisperModel("base", device="cpu", compute_type="int8")
# Для транскрипции:
segments, info = model.transcribe(tmp_path, language=language)
text = " ".join([segment.text for segment in segments])
```

### 1.3. Альтернатива: Tiny OpenAI Whisper API

Простой сервер, эмулирующий OpenAI Whisper API.

**Установка:**
```bash
git clone https://github.com/morioka/tiny-openai-whisper-api.git
cd tiny-openai-whisper-api
pip install -r requirements.txt
python app.py
```

### 1.3. Обновление кода приложения

После установки Whisper сервера, обновите `local.properties`:

```properties
# URL для Whisper сервера (отдельно от Ollama)
whisper.base.url=http://127.0.0.1:8000
```

И обновите `SpeechRecognitionService.desktop.kt` для использования правильного endpoint:

```kotlin
// Использовать OpenAI-совместимый API
val response: WhisperResponse = httpClient.post("$baseUrl/v1/audio/transcriptions") {
    contentType(ContentType.MultiPart.FormData)
    setBody(
        MultiPartFormDataContent(
            formData {
                append("file", audioBytes, Headers.build {
                    append(HttpHeaders.ContentType, "audio/wav")
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"audio.wav\"")
                })
                append("model", "whisper-1")  // Или название вашей модели
                append("language", "ru")
            }
        )
    )
}.body()
```

---

## Вариант 2: Прямое использование Whisper через Python

### 2.1. Установка Whisper

```bash
# Установка Python зависимостей
pip install openai-whisper

# Или через pipx
pipx install openai-whisper
```

### 2.2. Создание простого HTTP сервера

Создайте файл `whisper_server.py`:

```python
from flask import Flask, request, jsonify
import whisper
import tempfile
import os

app = Flask(__name__)
model = whisper.load_model("base")  # Используйте "tiny", "base", "small", "medium", "large"

@app.route('/v1/audio/transcriptions', methods=['POST'])
def transcribe():
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400
    
    file = request.files['file']
    language = request.form.get('language', 'ru')
    
    # Сохраняем временный файл
    with tempfile.NamedTemporaryFile(delete=False, suffix='.wav') as tmp_file:
        file.save(tmp_file.name)
        
        # Транскрибируем
        result = model.transcribe(tmp_file.name, language=language)
        
        # Удаляем временный файл
        os.unlink(tmp_file.name)
        
        return jsonify({'text': result['text']})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)
```

**Запуск сервера:**
```bash
python whisper_server.py
```

### 2.3. Использование Faster-Whisper (более быстрая версия)

```bash
pip install faster-whisper
```

Обновите `whisper_server.py`:

```python
from faster_whisper import WhisperModel

model = WhisperModel("base", device="cpu", compute_type="int8")
```

---

## Вариант 3: Использование Whisper.cpp (C++ реализация)

### 3.1. Установка Whisper.cpp

```bash
git clone https://github.com/ggerganov/whisper.cpp.git
cd whisper.cpp
make
```

### 3.2. Создание HTTP сервера

Используйте готовые обертки, например:
- [whisper-server](https://github.com/ggerganov/whisper.cpp/tree/master/examples/server)
- Или создайте свой HTTP сервер, который вызывает `whisper.cpp`

---

## Настройка в приложении

### Шаг 1: Обновить ApiConfig

Добавьте в `ApiConfig.kt`:

```kotlin
val whisperBaseUrl: String
```

### Шаг 2: Обновить конфигурацию

В `build.gradle.kts` добавьте:

```kotlin
val whisperBaseUrl: String = localProperties.getProperty("whisper.base.url") ?: "http://127.0.0.1:8000"
```

И в `generateApiConfig`:

```kotlin
appendLine("whisper.base.url=$whisperBaseUrl")
```

### Шаг 3: Обновить SpeechRecognitionService

Используйте `whisperBaseUrl` вместо `localModelBaseUrl` для Whisper сервера.

---

## Тестирование

### Проверка работы Whisper сервера

```bash
# Тест через curl
curl -X POST http://localhost:8000/v1/audio/transcriptions \
  -H "Content-Type: multipart/form-data" \
  -F "file=@test_audio.wav" \
  -F "model=whisper-1" \
  -F "language=ru"
```

### Проверка из приложения

1. Запустите Whisper сервер
2. Запустите приложение
3. Нажмите кнопку записи голоса
4. Говорите в микрофон
5. Остановите запись
6. Проверьте, что текст распознан и отправлен в чат

---

## Рекомендации

1. **Для разработки:** Используйте модель `tiny` или `base` - они быстрее
2. **Для production:** Используйте модель `small` или `medium` - лучше качество
3. **Для максимального качества:** Используйте модель `large-v3` - требует больше ресурсов

### Размеры моделей Whisper:

- `tiny` - ~39 MB, быстрая, низкое качество
- `base` - ~74 MB, средняя скорость, хорошее качество
- `small` - ~244 MB, медленнее, лучше качество
- `medium` - ~769 MB, медленная, высокое качество
- `large-v3` - ~1550 MB, очень медленная, максимальное качество

---

## Устранение проблем

### Проблема: Сервер не отвечает

**Решение:**
- Проверьте, что сервер запущен: `curl http://localhost:8000/health`
- Проверьте firewall настройки
- Убедитесь, что порт не занят другим процессом

### Проблема: Ошибка при загрузке модели

**Решение:**
- Убедитесь, что модель загружена: `whisper --help`
- Проверьте доступное место на диске
- Попробуйте использовать меньшую модель

### Проблема: Медленное распознавание

**Решение:**
- Используйте GPU ускорение (если доступно)
- Используйте `faster-whisper` вместо стандартного Whisper
- Используйте меньшую модель
- Оптимизируйте формат аудио (моно, 16kHz)

---

## Альтернативные решения

Если Whisper не подходит, рассмотрите:

1. **Vosk** - офлайн распознавание речи, поддерживает множество языков
2. **DeepSpeech** - открытая модель от Mozilla
3. **Wav2Vec2** - модель от Facebook

---

## Ссылки

- [OpenAI Whisper GitHub](https://github.com/openai/whisper)
- [Faster-Whisper](https://github.com/guillaumekln/faster-whisper)
- [Whisper.cpp](https://github.com/ggerganov/whisper.cpp)
- [OWhisper](https://github.com/0x00001f/owhisper)
- [Speaches](https://github.com/speaches-ai/speaches)
