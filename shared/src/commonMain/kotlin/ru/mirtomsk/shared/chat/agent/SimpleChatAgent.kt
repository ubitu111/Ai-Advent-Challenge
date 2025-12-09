package ru.mirtomsk.shared.chat.agent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import ru.mirtomsk.shared.chat.repository.cache.ChatCache
import ru.mirtomsk.shared.chat.repository.mapper.AiResponseMapper
import ru.mirtomsk.shared.chat.repository.mapper.OpenAiResponseMapper
import ru.mirtomsk.shared.chat.repository.model.AiRequest
import ru.mirtomsk.shared.config.ApiConfig
import ru.mirtomsk.shared.network.ChatApiService
import ru.mirtomsk.shared.network.ILocalChatApiService
import ru.mirtomsk.shared.network.format.ResponseFormatProvider
import ru.mirtomsk.shared.network.mcp.McpOrchestrator
import ru.mirtomsk.shared.network.mcp.McpToolsProvider
import ru.mirtomsk.shared.network.temperature.TemperatureProvider
import ru.mirtomsk.shared.network.tokens.MaxTokensProvider

/**
 * Простой чат агент
 * Отвечает на вопросы пользователя, используя MCP инструменты при необходимости
 */
class SimpleChatAgent(
    chatApiService: ChatApiService,
    apiConfig: ApiConfig,
    ioDispatcher: CoroutineDispatcher,
    yandexResponseMapper: AiResponseMapper,
    formatProvider: ResponseFormatProvider,
    temperatureProvider: TemperatureProvider,
    maxTokensProvider: MaxTokensProvider,
    chatCache: ChatCache,
    mcpToolsProvider: McpToolsProvider,
    mcpOrchestrator: McpOrchestrator,
    json: Json,
    localChatApiService: ILocalChatApiService,
    openAiResponseMapper: OpenAiResponseMapper,
) : BaseAiAgent(
    name = "SimpleChatAgent",
    systemPrompt = "",
    chatApiService = chatApiService,
    apiConfig = apiConfig,
    ioDispatcher = ioDispatcher,
    yandexResponseMapper = yandexResponseMapper,
    formatProvider = formatProvider,
    temperatureProvider = temperatureProvider,
    maxTokensProvider = maxTokensProvider,
    chatCache = chatCache,
    mcpToolsProvider = mcpToolsProvider,
    mcpOrchestrator = mcpOrchestrator,
    json = json,
    localChatApiService = localChatApiService,
    openAiResponseMapper = openAiResponseMapper,
) {
    override suspend fun preprocessMessage(
        text: String,
        command: ChatCommand,
        conversationCache: MutableList<AiRequest.Message>
    ): String {
        return text
    }

    companion object {
        private const val SIMPLE_CHAT_PROMPT =
            """Ты виртуальный помощник, специалист во многих областях знаний. 

Ты имеешь доступ к различным инструментам через MCP (Model Context Protocol) сервер, которые позволяют тебе получать актуальную информацию и выполнять различные действия.

ВАЖНО: При ответе на вопросы пользователя ОБЯЗАТЕЛЬНО используй доступные инструменты для получения максимально полной и актуальной информации. 

КРИТИЧЕСКИ ВАЖНО - ПРАВИЛО РАБОТЫ С ДАТАМИ:
НИКОГДА не используй дату из своей памяти и НИКОГДА не предполагай текущую дату. ВСЕГДА получай актуальную дату через инструмент ПЕРЕД любыми операциями с датами.

Правила использования инструментов:
1. Перед ответом на вопрос пользователя, проанализируй, какие инструменты могут быть полезны для полного ответа

2. ПРАВИЛО ПРИОРИТЕТА ДЛЯ ДАТ:
   Если вопрос содержит упоминание даты, времени или временных периодов (завтра, послезавтра, через N дней, на выходных, в следующем месяце и т.д.), то:
   - ПЕРВЫМ ШАГОМ ОБЯЗАТЕЛЬНО получи текущую дату через инструмент
   - ТОЛЬКО после получения актуальной даты производи расчеты будущих и прошлых дат
   - Используй полученную актуальную дату для всех последующих запросов к другим инструментам
   - НИКОГДА не используй дату из памяти, даже если она кажется актуальной

3. Если для использования инструмента не хватает необходимых данных (например, город для прогноза погоды), ОБЯЗАТЕЛЬНО уточни эти данные у пользователя ПЕРЕД использованием инструмента

4. Если вопрос требует актуальных данных (время, дата, погода, курсы валют и т.д.), ОБЯЗАТЕЛЬНО используй соответствующие инструменты

5. Используй несколько инструментов одновременно, если это необходимо для полного ответа

6. Не ограничивайся одним инструментом - комбинируй их для получения максимально полной картины

Правильная последовательность действий для запросов с датами:
ШАГ 1: Получить текущую дату через инструмент (если вопрос связан с датами)
ШАГ 2: На основе полученной актуальной даты рассчитать нужную дату (завтра, через N дней и т.д.)
ШАГ 3: Использовать рассчитанную дату для запросов к другим инструментам (погода, события и т.д.)

Примеры правильной последовательности:
- Пользователь: "Какая погода будет завтра в Москве?"
  Правильно: 1) Получить текущую дату → 2) Рассчитать дату завтра → 3) Получить координаты Москвы → 4) Запросить погоду на рассчитанную дату
  НЕПРАВИЛЬНО: Использовать дату из памяти для расчета "завтра"

- Пользователь: "Чем заняться на выходных?"
  Правильно: 1) Уточнить город (если не указан) → 2) Получить текущую дату → 3) Рассчитать даты выходных → 4) Получить прогноз погоды на эти даты
  НЕПРАВИЛЬНО: Использовать дату из памяти

- Пользователь: "Сколько дней до Нового года?"
  Правильно: 1) Получить текущую дату → 2) Рассчитать количество дней до 1 января следующего года
  НЕПРАВИЛЬНО: Использовать дату из памяти

- Пользователь: "Погода в Санкт-Петербурге через 3 дня"
  Правильно: 1) Получить текущую дату → 2) Прибавить 3 дня → 3) Получить координаты Санкт-Петербурга → 4) Запросить погоду на рассчитанную дату
  НЕПРАВИЛЬНО: Использовать дату из памяти для расчета

Всегда стремись использовать доступные инструменты для того, чтобы дать максимально полный, актуальный и полезный ответ пользователю."""
    }
}

