# Локальный сервер

Простой HTTP-сервер на Ktor для локальной разработки.

## Запуск сервера

### Через Gradle

```bash
./gradlew :server:run
```

Или из корня проекта:

```bash
./gradlew server:run
```

### Через IDE

1. Откройте файл `server/src/main/kotlin/ru/mirtomsk/server/Application.kt`
2. Найдите функцию `main()`
3. Запустите её (Run/Debug)

## Конфигурация

Сервер запускается на:
- **Хост**: `0.0.0.0` (доступен со всех сетевых интерфейсов)
- **Порт**: `8080`

Для доступа с локальной машины используйте:
- `http://localhost:8080`
- `http://127.0.0.1:8080`

Для доступа с других устройств в той же сети используйте IP-адрес вашего компьютера:
- `http://<ваш-ip-адрес>:8080`

## API Эндпоинты

### GET /hello

Тестовый эндпоинт, возвращающий приветствие.

**Запрос:**
```http
GET http://localhost:8080/hello
```

**Ответ:**
```
привет мир
```

**Пример использования:**
```bash
curl http://localhost:8080/hello
```

## MCP (Model Context Protocol) Эндпоинты

Сервер поддерживает протокол MCP для расширения возможностей AI агентов. Все MCP запросы используют JSON-RPC 2.0 протокол.

### POST /mcp

Основной эндпоинт для работы с MCP протоколом. Поддерживает следующие методы:

#### 1. initialize

Инициализация MCP сессии.

**Запрос:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {}
}
```

**Ответ:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools": {
        "listChanged": false
      }
    },
    "serverInfo": {
      "name": "ai-advent-challenge-server",
      "version": "1.0.0"
    }
  }
}
```

#### 2. tools/list

Получить список доступных инструментов.

**Запрос:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}
```

**Ответ:**
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "get_weather",
        "description": "Получить текущую погоду для указанного города",
        "inputSchema": {
          "type": "object",
          "properties": {
            "city": {
              "type": "string",
              "description": "Название города"
            }
          },
          "required": ["city"]
        }
      },
      {
        "name": "calculate",
        "description": "Выполнить математические вычисления",
        "inputSchema": {
          "type": "object",
          "properties": {
            "expression": {
              "type": "string",
              "description": "Математическое выражение для вычисления"
            }
          },
          "required": ["expression"]
        }
      },
      {
        "name": "get_time",
        "description": "Получить текущее время",
        "inputSchema": {
          "type": "object",
          "properties": {},
          "required": []
        }
      }
    ]
  }
}
```

#### 3. tools/call

Вызвать инструмент с указанными параметрами.

**Запрос:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "get_weather",
    "arguments": {
      "city": "Москва"
    }
  }
}
```

**Ответ:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "Погода в Москва: Солнечно, +15°C"
      }
    ],
    "isError": false
  }
}
```

**Примеры использования:**

Получить список инструментов:
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list"
  }'
```

Вызвать инструмент:
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "get_time"
    }
  }'
```

## Доступные инструменты

1. **get_weather** - Получить текущую погоду для указанного города
   - Параметры: `city` (string, обязательный)

2. **calculate** - Выполнить математические вычисления
   - Параметры: `expression` (string, обязательный)

3. **get_time** - Получить текущее время
   - Параметры: нет

## Архитектура

Сервер построен по принципам чистой архитектуры:

- **Domain слой** (`domain/`) - бизнес-логика, модели и интерфейсы
- **Data слой** (`data/`) - реализации репозиториев и DTO модели
- **Presentation слой** (`presentation/`) - контроллеры и роутинг

## Особенности

- Сервер поддерживает CORS, что позволяет обращаться к нему из веб-приложений и мобильных приложений
- Настроена поддержка JSON для всех эндпоинтов
- Сервер работает на Netty engine
- Реализована поддержка MCP протокола (Model Context Protocol) для расширения возможностей AI агентов
- Код следует принципам чистой архитектуры и чистого кода

## Остановка сервера

Нажмите `Ctrl+C` в терминале, где запущен сервер.

