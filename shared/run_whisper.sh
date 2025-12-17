#!/bin/bash
# Скрипт для запуска Whisper сервера

# Проверить, существует ли виртуальное окружение
if [ ! -d "whisper_env" ]; then
    echo "❌ Виртуальное окружение не найдено."
    echo "Запустите сначала: ./setup_whisper.sh"
    exit 1
fi

# Активировать виртуальное окружение
source whisper_env/bin/activate

# Проверить, что виртуальное окружение активировано
if [ -z "$VIRTUAL_ENV" ]; then
    echo "❌ Не удалось активировать виртуальное окружение"
    exit 1
fi

echo "✅ Виртуальное окружение активировано: $VIRTUAL_ENV"

# Проверить, не занят ли порт 8000
if lsof -ti :8000 > /dev/null 2>&1; then
    echo "⚠️  Порт 8000 занят. Остановка старого процесса..."
    ./stop_whisper.sh
    sleep 1
fi

# Проверить установку fastapi
if ! python -c "import fastapi" 2>/dev/null; then
    echo "❌ FastAPI не установлен в виртуальном окружении"
    echo "Установка зависимостей..."
    pip install fastapi uvicorn python-multipart
    
    # Проверить, какой сервер доступен и установить соответствующий пакет
    if [ -f "whisper_server_faster.py" ]; then
        pip install faster-whisper
    else
        pip install openai-whisper
    fi
fi

# Проверить, какой сервер доступен и какие пакеты установлены
if python -c "import faster_whisper" 2>/dev/null && [ -f "whisper_server_faster.py" ]; then
    SERVER_FILE="whisper_server_faster.py"
    echo "🚀 Запуск Whisper сервера (faster-whisper)..."
elif python -c "import whisper" 2>/dev/null && [ -f "whisper_server.py" ]; then
    SERVER_FILE="whisper_server.py"
    echo "🚀 Запуск Whisper сервера (openai-whisper)..."
else
    # Проверить, что хотя бы один пакет установлен
    if ! python -c "import whisper" 2>/dev/null && ! python -c "import faster_whisper" 2>/dev/null; then
        echo "❌ Whisper не установлен. Устанавливаю openai-whisper..."
        pip install openai-whisper
        SERVER_FILE="whisper_server.py"
    elif python -c "import whisper" 2>/dev/null; then
        SERVER_FILE="whisper_server.py"
        echo "🚀 Запуск Whisper сервера (openai-whisper)..."
    else
        SERVER_FILE="whisper_server_faster.py"
        echo "🚀 Запуск Whisper сервера (faster-whisper)..."
    fi
fi

# Запустить сервер (используем python, а не python3, так как виртуальное окружение активировано)
python "$SERVER_FILE"
