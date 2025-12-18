#!/usr/bin/env python3
"""
Простой Whisper сервер с OpenAI-совместимым API (использует faster-whisper)
Использование: python whisper_server_faster.py
"""

from fastapi import FastAPI, File, UploadFile, Form
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from faster_whisper import WhisperModel
import tempfile
import os
import sys

app = FastAPI(title="Whisper API Server (Faster)")

# Добавить CORS для локальной разработки
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Глобальная переменная для модели
model = None

def load_model(model_name: str = "base"):
    """Загрузить модель Whisper (faster-whisper)"""
    global model
    print(f"Загрузка модели Whisper: {model_name}...")
    try:
        # Используем faster-whisper для лучшей производительности
        # device="cpu" для CPU, "cuda" для GPU
        # compute_type="int8" для быстрой работы на CPU
        device = os.getenv("WHISPER_DEVICE", "cpu")
        compute_type = os.getenv("WHISPER_COMPUTE_TYPE", "int8")
        
        model = WhisperModel(model_name, device=device, compute_type=compute_type)
        print(f"Модель {model_name} загружена успешно! (device={device}, compute_type={compute_type})")
    except Exception as e:
        print(f"Ошибка при загрузке модели: {e}")
        print("Доступные модели: tiny, base, small, medium, large-v3")
        print("Убедитесь, что faster-whisper установлен: pip install faster-whisper")
        sys.exit(1)

@app.on_event("startup")
async def startup_event():
    """Загрузить модель при старте сервера"""
    # Можно изменить модель через переменную окружения
    model_name = os.getenv("WHISPER_MODEL", "base")
    load_model(model_name)

@app.post("/v1/audio/transcriptions")
async def transcribe_audio(
    file: UploadFile = File(...),
    model_name: str = Form("whisper-1"),
    language: str = Form(None)
):
    """
    Транскрибировать аудио файл
    Совместимо с OpenAI Whisper API
    """
    try:
        # Сохранить временный файл
        file_extension = os.path.splitext(file.filename)[1] or ".wav"
        with tempfile.NamedTemporaryFile(delete=False, suffix=file_extension) as tmp_file:
            content = await file.read()
            tmp_file.write(content)
            tmp_path = tmp_file.name
        
        # Транскрибировать с помощью faster-whisper
        print(f"Транскрибирование файла: {file.filename}")
        segments, info = model.transcribe(
            tmp_path,
            language=language,
            beam_size=5
        )
        
        # Собрать текст из сегментов
        text = " ".join([segment.text for segment in segments])
        
        # Удалить временный файл
        os.unlink(tmp_path)
        
        print(f"Распознано: {text[:100]}...")  # Показать первые 100 символов
        
        return JSONResponse(content={"text": text})
    except Exception as e:
        print(f"Ошибка при транскрипции: {e}")
        import traceback
        traceback.print_exc()
        return JSONResponse(
            status_code=500,
            content={"error": {"message": str(e), "type": "server_error"}}
        )

@app.get("/health")
async def health():
    """Health check endpoint"""
    return {"status": "ok", "model_loaded": model is not None}

@app.get("/v1/models")
async def list_models():
    """Список доступных моделей (для совместимости с OpenAI API)"""
    return {
        "data": [
            {
                "id": "whisper-1",
                "object": "model",
                "created": 1677610602,
                "owned_by": "openai"
            }
        ],
        "object": "list"
    }

if __name__ == "__main__":
    import uvicorn
    import socket
    
    # Функция для проверки доступности порта
    def is_port_available(port):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            try:
                s.bind(('0.0.0.0', port))
                return True
            except OSError:
                return False
    
    # Порт можно изменить через переменную окружения
    default_port = int(os.getenv("PORT", "8000"))
    host = os.getenv("HOST", "0.0.0.0")
    
    # Проверить доступность порта
    port = default_port
    if not is_port_available(port):
        print(f"⚠️  Порт {port} уже занят, пробую альтернативные порты...")
        for alt_port in [8001, 8002, 8003, 8080, 8888]:
            if is_port_available(alt_port):
                port = alt_port
                print(f"✅ Использую порт {port}")
                break
        else:
            print(f"❌ Не удалось найти свободный порт. Остановите процесс на порту {default_port}")
            print(f"   Или укажите другой порт: PORT=8001 python whisper_server_faster.py")
            sys.exit(1)
    
    print(f"Запуск Whisper API сервера на http://{host}:{port}")
    print("Используйте Ctrl+C для остановки")
    print(f"\n💡 Если нужно использовать другой порт, установите переменную окружения:")
    print(f"   PORT=8001 python whisper_server_faster.py")
    
    uvicorn.run(app, host=host, port=port)
