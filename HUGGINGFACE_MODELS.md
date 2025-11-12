# Модели HuggingFace для Text Generation

Рекомендации по выбору моделей для сравнения разных уровней натренированности через HuggingFace Inference API.

## Интеграция в проект

Модели HuggingFace интегрированы в проект. Для использования:

1. **Добавьте HuggingFace токен в конфигурацию:**
   - Добавьте в `local.properties` (или в файл, из которого генерируется `api.properties`):
   ```
   huggingface.token=YOUR_HF_TOKEN_HERE
   ```

2. **Использование в коде:**
   ```kotlin
   // Получить репозиторий HuggingFace
   val huggingFaceRepository: HuggingFaceChatRepository = get()
   
   // Получить провайдер моделей
   val agentTypeProvider: AgentTypeProvider = get()
   
   // Выбрать модель
   agentTypeProvider.updateAgentType(AgentTypeDto.MISTRAL_7B_INSTRUCT)
   
   // Отправить сообщение
   val response = huggingFaceRepository.sendMessage("Привет!")
   ```

3. **Доступные модели:**
   - Используйте `AgentTypeDto` enum для выбора модели (объединяет Yandex GPT и HuggingFace)
   - `AgentTypeDto.getRecommendedForComparison()` - получить рекомендуемую тройку
   - `AgentTypeDto.getByLevel(ModelLevel.TOP)` - получить модели по уровню
   - `AgentTypeDto.getHuggingFaceModels()` - получить все HuggingFace модели
   - `AgentTypeDto.getYandexGptModels()` - получить все Yandex GPT модели

## Рекомендуемые модели

### 1. Топ-уровень (высокое качество, большие модели)

#### **meta-llama/Llama-2-7b-chat-hf**
- **Параметры**: 7B
- **Разработчик**: Meta AI
- **Особенности**: 
  - Одна из лучших открытых моделей для чата
  - Обучена на диалогах (chat-версия)
  - Высокое качество генерации текста
- **HuggingFace ID**: `meta-llama/Llama-2-7b-chat-hf`
- **API Endpoint**: `https://router.huggingface.co/hf-inference/v1/chat/completions` (model указывается в теле запроса)
- **Статус**: Может требовать запрос доступа на HuggingFace

#### **mistralai/Mistral-7B-Instruct-v0.2**
- **Параметры**: 7B
- **Разработчик**: Mistral AI
- **Особенности**:
  - Современная инструктивная модель
  - Отличное качество для инструкций и диалогов
  - Хорошо работает с промптами
- **HuggingFace ID**: `mistralai/Mistral-7B-Instruct-v0.2`
- **API Endpoint**: `https://router.huggingface.co/hf-inference/v1/chat/completions` (model указывается в теле запроса)

#### **bigscience/bloom-7b1**
- **Параметры**: 7.1B
- **Разработчик**: BigScience
- **Особенности**:
  - Мультиязычная модель (46 языков)
  - Хорошее качество генерации
  - Открытая и доступная
- **HuggingFace ID**: `bigscience/bloom-7b1`
- **API Endpoint**: `https://router.huggingface.co/hf-inference/v1/chat/completions` (model указывается в теле запроса)

### 2. Средний уровень (баланс качества и скорости)

#### **EleutherAI/gpt-j-6b**
- **Параметры**: 6B
- **Разработчик**: EleutherAI
- **Особенности**:
  - Хорошее качество для своего размера
  - Открытая альтернатива GPT-3
  - Хорошо работает с кодом
- **HuggingFace ID**: `EleutherAI/gpt-j-6b`
- **API Endpoint**: `https://router.huggingface.co/hf-inference/v1/chat/completions` (model указывается в теле запроса)

#### **microsoft/DialoGPT-medium**
- **Параметры**: 345M
- **Разработчик**: Microsoft
- **Особенности**:
  - Специализирована на диалогах
  - Средний размер, хорошая скорость
  - Хорошо для чат-приложений
- **HuggingFace ID**: `microsoft/DialoGPT-medium`
- **API Endpoint**: `https://router.huggingface.co/hf-inference/v1/chat/completions` (model указывается в теле запроса)

### 3. Базовый уровень (компактные модели)

#### **gpt2**
- **Параметры**: 124M
- **Разработчик**: OpenAI
- **Особенности**:
  - Классическая базовая модель
  - Быстрая и легкая
  - Хорошо для простых задач
- **HuggingFace ID**: `gpt2`
- **API Endpoint**: `https://router.huggingface.co/hf-inference/v1/chat/completions` (model указывается в теле запроса)

#### **distilgpt2**
- **Параметры**: 82M
- **Разработчик**: HuggingFace
- **Особенности**:
  - Упрощенная версия GPT-2
  - Очень быстрая
  - Минимальные требования к ресурсам
- **HuggingFace ID**: `distilgpt2`
- **API Endpoint**: `https://router.huggingface.co/hf-inference/v1/chat/completions` (model указывается в теле запроса)

#### **TinyLlama/TinyLlama-1.1B-Chat-v1.0**
- **Параметры**: 1.1B
- **Разработчик**: TinyLlama Team
- **Особенности**:
  - Компактная, но более мощная чем GPT-2
  - Обучена на большом датасете
  - Хороший баланс для базового уровня
- **HuggingFace ID**: `TinyLlama/TinyLlama-1.1B-Chat-v1.0`
- **API Endpoint**: `https://router.huggingface.co/hf-inference/v1/chat/completions` (model указывается в теле запроса)

## Рекомендуемая тройка для сравнения

Для сравнения разных уровней натренированности рекомендуется использовать:

1. **Топ**: `mistralai/Mistral-7B-Instruct-v0.2` или `bigscience/bloom-7b1`
2. **Средний**: `EleutherAI/gpt-j-6b` или `microsoft/DialoGPT-medium`
3. **Базовый**: `TinyLlama/TinyLlama-1.1B-Chat-v1.0` или `gpt2`

## Использование HuggingFace Inference API

### Получение API токена

1. Зарегистрируйтесь на [HuggingFace](https://huggingface.co/)
2. Перейдите в [Settings → Access Tokens](https://huggingface.co/settings/tokens)
3. Создайте новый токен с правами чтения

### Формат запроса (новый Chat API)

Новый формат использует массив `messages` вместо `inputs`:

```json
{
  "messages": [
    {
      "role": "user",
      "content": "Ваш текст для генерации"
    }
  ],
  "model": "mistralai/Mistral-7B-Instruct-v0.2",
  "stream": false,
  "temperature": 0.7,
  "max_tokens": 100
}
```

### Пример запроса (cURL)

```bash
curl https://router.huggingface.co/hf-inference/v1/chat/completions \
  -H "Authorization: Bearer YOUR_HF_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {
        "role": "user",
        "content": "Привет, как дела?"
      }
    ],
    "model": "mistralai/Mistral-7B-Instruct-v0.2",
    "stream": false,
    "temperature": 0.7,
    "max_tokens": 50
  }'
```

### Формат ответа (новый Chat API)

```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "Сгенерированный текст модели..."
      }
    }
  ]
}
```

## Примечания

- **Новый API endpoint**: Используется новый router API (`https://router.huggingface.co/hf-inference`) вместо устаревшего `https://api-inference.huggingface.co`
- **Бесплатный тариф**: HuggingFace Inference API предоставляет бесплатный доступ, но с ограничениями по количеству запросов
- **Cold start**: При первом запросе модель может загружаться (cold start), что занимает время
- **Очередь**: На бесплатном тарифе запросы могут попадать в очередь
- **Доступ к моделям**: Некоторые модели (например, Llama-2) могут требовать запрос доступа на HuggingFace

## Альтернативные варианты

Если нужны модели без ограничений доступа:

1. **Топ**: `bigscience/bloom-7b1` (открытая, не требует запроса)
2. **Средний**: `EleutherAI/gpt-j-6b` (открытая)
3. **Базовый**: `gpt2` или `distilgpt2` (открытые)

