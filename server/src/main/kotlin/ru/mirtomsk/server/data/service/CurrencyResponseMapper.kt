package ru.mirtomsk.server.data.service

import org.slf4j.LoggerFactory
import ru.mirtomsk.server.data.model.ExchangeRateResponse
import ru.mirtomsk.server.domain.service.CurrencyRateData

/**
 * Mapper for converting ExchangeRateResponse to domain models
 */
object CurrencyResponseMapper {
    
    private val logger = LoggerFactory.getLogger(CurrencyResponseMapper::class.java)
    
    /**
     * Convert timestamp to date string (YYYY-MM-DD)
     */
    private fun timestampToDate(timestamp: Long): String {
        return java.time.Instant.ofEpochSecond(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    /**
     * Extract date from response (from timestamp or date field)
     */
    private fun extractDate(response: ExchangeRateResponse, fallbackDate: String? = null): String? {
        return response.timestamp?.let { timestampToDate(it) }
            ?: response.date
            ?: response.info?.timestamp?.let { timestampToDate(it) }
            ?: fallbackDate
    }
    
    /**
     * Map response to CurrencyRateData for single currency pair
     */
    fun mapToCurrencyRate(
        response: ExchangeRateResponse,
        baseCurrency: String,
        targetCurrency: String,
        fallbackDate: String? = null
    ): CurrencyRateData? {
        val base = baseCurrency.uppercase()
        val target = targetCurrency.uppercase()
        
        // Try new format with quotes first
        if (response.quotes != null) {
            val source = response.source ?: return null
            val quoteKey = if (source == base) "$source$target" else "$source$base"
            val rate = response.quotes[quoteKey]
            
            if (rate == null) {
                logger.warn("Курс для валюты $target не найден в ответе. Искали ключ: $quoteKey. Доступные ключи: ${response.quotes.keys.joinToString()}")
                return null
            }
            
            val date = extractDate(response, fallbackDate) ?: run {
                logger.warn("Дата не найдена в ответе API")
                return null
            }
            
            return CurrencyRateData(
                baseCurrency = base,
                targetCurrency = target,
                rate = rate,
                date = date
            )
        }
        
        // Fallback to old format with rates
        if (response.rates != null) {
            val rate = response.rates[target]
            if (rate == null) {
                logger.warn("Курс для валюты $target не найден в ответе. Доступные валюты: ${response.rates.keys.take(10).joinToString()}")
                return null
            }
            
            val date = extractDate(response, fallbackDate) ?: run {
                logger.warn("Дата не найдена в ответе API")
                return null
            }
            
            return CurrencyRateData(
                baseCurrency = base,
                targetCurrency = target,
                rate = rate,
                date = date
            )
        }

        val query = response.query
        val result = response.result
        if (query != null && result != null) {
            val date = extractDate(response, fallbackDate) ?: run {
                logger.warn("Дата не найдена в ответе API")
                return null
            }

            return CurrencyRateData(
                baseCurrency = query.from,
                targetCurrency = query.to,
                rate = result,
                date = date
            )
        }
        
        logger.error("Ответ API не содержит курсов валют (quotes = null, rates = null)")
        return null
    }
}

