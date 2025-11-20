package ru.mirtomsk.server.data.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.slf4j.LoggerFactory
import ru.mirtomsk.server.data.model.ExchangeRateResponse
import ru.mirtomsk.server.domain.service.CurrencyRateData
import ru.mirtomsk.server.domain.service.CurrencyService

/**
 * Implementation of CurrencyService using ExchangeRate.host API
 * Free API, no registration required
 * Documentation: https://exchangerate.host/
 */
class ExchangeRateCurrencyService(
    private val httpClient: HttpClient
) : CurrencyService {
    
    companion object {
        private const val BASE_URL = "https://api.exchangerate.host"
        private const val API_KEY = "086b528229de1b0e5f257d67cbd10cd4"
        private val logger = LoggerFactory.getLogger(ExchangeRateCurrencyService::class.java)
    }
    
    override suspend fun getExchangeRate(baseCurrency: String, targetCurrency: String): CurrencyRateData? {
        return try {
            val base = baseCurrency.uppercase()
            val target = targetCurrency.uppercase()
            
            logger.debug("Запрос курса валют: $base -> $target")
            
            val response: ExchangeRateResponse = httpClient.get("$BASE_URL/convert") {
                parameter("access_key", API_KEY)
                parameter("from", base)
                parameter("to", target)
                parameter("amount", 1)
            }.body()

            logger.debug("Получен ответ от API: success=${response.success}, source=${response.source}, timestamp=${response.timestamp}, quotes count=${response.quotes?.size}")
            
            val result = CurrencyResponseMapper.mapToCurrencyRate(response, base, target)
            if (result != null) {
                logger.info("Успешно получен курс ${result.baseCurrency} -> ${result.targetCurrency}: ${result.rate} на дату ${result.date}")
            }
            result
        } catch (e: Exception) {
            logger.error("Ошибка при получении курса валют $baseCurrency -> $targetCurrency: ${e.message}", e)
            null
        }
    }
    
    override suspend fun getExchangeRateByDate(baseCurrency: String, targetCurrency: String, date: String): CurrencyRateData? {
        return try {
            val base = baseCurrency.uppercase()
            val target = targetCurrency.uppercase()
            
            logger.debug("Запрос исторического курса валют: $base -> $target на дату $date")
            
            val response: ExchangeRateResponse = httpClient.get("$BASE_URL/historical") {
                parameter("access_key", API_KEY)
                parameter("base", base)
                parameter("target", target)
                parameter("date", date)
            }.body()
            
            logger.debug("Получен ответ от API для даты $date: success=${response.success}, source=${response.source}, timestamp=${response.timestamp}, quotes count=${response.quotes?.size}")
            
            val result = CurrencyResponseMapper.mapToCurrencyRate(response, base, target, date)
            if (result != null) {
                logger.info("Успешно получен исторический курс ${result.baseCurrency} -> ${result.targetCurrency}: ${result.rate} на дату ${result.date}")
            }
            result
        } catch (e: Exception) {
            logger.error("Ошибка при получении исторического курса валют $baseCurrency -> $targetCurrency на дату $date: ${e.message}", e)
            null
        }
    }
}
