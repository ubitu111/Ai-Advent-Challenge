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
      },
      {
        "name": "get_currency_rate",
        "description": "Получить текущий курс обмена валют",
        "inputSchema": {
          "type": "object",
          "properties": {
            "base_currency": {
              "type": "string",
              "description": "Базовая валюта (например: USD, EUR, RUB)"
            },
            "target_currency": {
              "type": "string",
              "description": "Целевая валюта (например: USD, EUR, RUB)"
            }
          },
          "required": ["base_currency", "target_currency"]
        }
      },
      {
        "name": "get_currency_rate_historical",
        "description": "Получить курс обмена валют за определенный прошлый день",
        "inputSchema": {
          "type": "object",
          "properties": {
            "base_currency": {
              "type": "string",
              "description": "Базовая валюта (например: USD, EUR, RUB)"
            },
            "target_currency": {
              "type": "string",
              "description": "Целевая валюта (например: USD, EUR, RUB)"
            },
            "date": {
              "type": "string",
              "description": "Дата в формате YYYY-MM-DD (например: 2024-01-15)"
            }
          },
          "required": ["base_currency", "target_currency", "date"]
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

4. **get_currency_rate** - Получить текущий курс обмена валют
   - Параметры: `base_currency` (string, обязательный), `target_currency` (string, обязательный)
   - Пример: `{"base_currency": "USD", "target_currency": "RUB"}` - получить курс доллара к рублю

5. **get_currency_rate_historical** - Получить курс обмена валют за определенный прошлый день
   - Параметры: `base_currency` (string, обязательный), `target_currency` (string, обязательный), `date` (string, обязательный)
   - Формат даты: YYYY-MM-DD (например: 2024-01-15)
   - Пример: `{"base_currency": "USD", "target_currency": "RUB", "date": "2024-01-15"}` - получить курс доллара к рублю на 15 января 2024 года

6. **git_status** - Получить статус GitHub репозитория (информация о репозитории, последний коммит, ветка по умолчанию)
   - Параметры: нет
   - Требует настройки GitHub (см. раздел "Настройка GitHub API")

7. **git_log** - Получить историю коммитов из GitHub репозитория (аналог git log)
   - Параметры: `limit` (number, опционально) - максимальное количество коммитов (по умолчанию: 30, максимум: 100), `branch` (string, опционально) - имя ветки
   - Пример: `{"limit": 10, "branch": "main"}` - получить последние 10 коммитов из ветки main
   - Требует настройки GitHub (см. раздел "Настройка GitHub API")

8. **git_branch** - Получить список веток GitHub репозитория (аналог git branch)
   - Параметры: нет
   - Требует настройки GitHub (см. раздел "Настройка GitHub API")

## Настройка GitHub API

Для использования инструментов GitHub API (`git_status`, `git_log`, `git_branch`) необходимо настроить подключение к GitHub репозиторию.

### Шаг 1: Создание Personal Access Token (PAT)

1. Перейдите на GitHub: https://github.com/settings/tokens
2. Нажмите "Generate new token" → "Generate new token (classic)"
3. Задайте название токена (например: "MCP Server")
4. Выберите срок действия токена (рекомендуется: "No expiration" или "90 days")
5. Выберите необходимые права доступа:
   - Для публичных репозиториев: `public_repo` (доступ к публичным репозиториям)
   - Для приватных репозиториев: `repo` (полный доступ к репозиториям)
6. Нажмите "Generate token"
7. **ВАЖНО**: Скопируйте токен сразу, он больше не будет показан! Токен выглядит как `ghp_xxxxxxxxxxxxxxxxxxxx`

### Шаг 2: Настройка переменных окружения

Есть два способа настройки:

#### Способ 1: Переменные окружения (рекомендуется)

Установите следующие переменные окружения перед запуском сервера:

**Linux/macOS:**
```bash
export GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxx 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
export GITHUB_OWNER=username
export GITHUB_REPO=my-repo
```

**Windows (PowerShell):**
```powershell
$env:GITHUB_TOKEN="ghp_xxxxxxxxxxxxxxxxxxxx"
$env:GITHUB_OWNER="username"
$env:GITHUB_REPO="my-repo"
```

**Windows (CMD):**
```cmd
set GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxx
set GITHUB_OWNER=username
set GITHUB_REPO=my-repo
```

#### Способ 2: Системные свойства

Запустите сервер с параметрами:

```bash
./gradlew :server:run -Dgithub.token=ghp_xxxxxxxxxxxxxxxxxxxx -Dgithub.owner=username -Dgithub.repo=my-repo
```

### Шаг 3: Проверка настройки

После настройки и запуска сервера, проверьте работу GitHub инструментов:

```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "git_status"
    }
  }'
```

Если настройка выполнена правильно, вы получите информацию о репозитории. В противном случае будет возвращена ошибка с указанием на необходимость настройки.

### Параметры конфигурации

| Параметр | Описание | Пример |
|----------|----------|--------|
| `GITHUB_TOKEN` | Personal Access Token для аутентификации в GitHub API | `ghp_xxxxxxxxxxxxxxxxxxxx` |
| `GITHUB_OWNER` | Владелец репозитория (username или organization) | `octocat` или `github` |
| `GITHUB_REPO` | Название репозитория | `Hello-World` |

### Безопасность

⚠️ **ВАЖНО**: Никогда не коммитьте токен в репозиторий!

- Используйте переменные окружения или системные свойства
- Добавьте `.env` файлы в `.gitignore` (если используете)
- Для production используйте секретные менеджеры (AWS Secrets Manager, HashiCorp Vault и т.д.)
- Регулярно обновляйте токены
- Используйте минимально необходимые права доступа

### Примеры использования

**Получить статус репозитория:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "git_status"
    }
  }'
```

**Получить последние 10 коммитов:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "git_log",
      "arguments": {
        "limit": 10
      }
    }
  }'
```

**Получить коммиты из конкретной ветки:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "git_log",
      "arguments": {
        "limit": 20,
        "branch": "develop"
      }
    }
  }'
```

**Получить список всех веток:**
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "git_branch"
    }
  }'
```

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

