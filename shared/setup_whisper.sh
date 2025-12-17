#!/bin/bash
# Скрипт для быстрой настройки Whisper сервера

echo "🚀 Настройка Whisper сервера..."

# Проверка Python
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3 не найден. Установите Python 3.8 или выше."
    exit 1
fi

echo "✅ Python найден: $(python3 --version)"

# Создать виртуальное окружение (если не существует)
if [ ! -d "whisper_env" ]; then
    echo "📦 Создание виртуального окружения..."
    python3 -m venv whisper_env
fi

# Активировать виртуальное окружение
echo "🔧 Активация виртуального окружения..."
source whisper_env/bin/activate

# Обновить pip
echo "⬆️  Обновление pip..."
pip install --upgrade pip

# Установить зависимости
echo "📥 Установка зависимостей..."
echo "Выберите вариант:"
echo "1) Стандартный Whisper (openai-whisper)"
echo "2) Faster Whisper (быстрее, рекомендуется)"
read -p "Ваш выбор (1 или 2): " choice

# Базовые зависимости для обоих вариантов
echo "Установка базовых зависимостей..."
pip install fastapi uvicorn python-multipart

if [ "$choice" = "2" ]; then
    echo "Установка faster-whisper..."
    pip install faster-whisper
    SERVER_FILE="whisper_server_faster.py"
    echo "✅ Установлен faster-whisper"
else
    echo "Установка openai-whisper..."
    pip install openai-whisper
    SERVER_FILE="whisper_server.py"
    echo "✅ Установлен openai-whisper"
fi

# Проверить установку
echo ""
echo "🔍 Проверка установки..."
if python -c "import fastapi" 2>/dev/null; then
    echo "✅ FastAPI установлен"
else
    echo "❌ FastAPI не установлен"
    exit 1
fi

if [ "$choice" = "2" ]; then
    if python -c "import faster_whisper" 2>/dev/null; then
        echo "✅ faster-whisper установлен"
    else
        echo "❌ faster-whisper не установлен"
        exit 1
    fi
else
    if python -c "import whisper" 2>/dev/null; then
        echo "✅ openai-whisper установлен"
    else
        echo "❌ openai-whisper не установлен"
        exit 1
    fi
fi

echo ""
echo "✅ Установка завершена!"
echo ""
echo "Для запуска сервера выполните:"
echo "  source whisper_env/bin/activate"
echo "  python $SERVER_FILE"
echo ""
echo "Или используйте скрипт запуска:"
echo "  ./run_whisper.sh"
