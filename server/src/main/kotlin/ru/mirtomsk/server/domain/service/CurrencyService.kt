package ru.mirtomsk.server.domain.service

/**
 * Domain service interface for currency exchange rate data
 */
interface CurrencyService {
    /**
     * Get current exchange rate for a currency pair
     * @param baseCurrency Base currency code (e.g., "USD", "EUR", "RUB")
     * @param targetCurrency Target currency code (e.g., "USD", "EUR", "RUB")
     * @return Exchange rate data or null if not available
     */
    suspend fun getExchangeRate(baseCurrency: String, targetCurrency: String): CurrencyRateData?
    
    /**
     * Get exchange rate for a currency pair on a specific date
     * @param baseCurrency Base currency code (e.g., "USD", "EUR", "RUB")
     * @param targetCurrency Target currency code (e.g., "USD", "EUR", "RUB")
     * @param date Date in format YYYY-MM-DD
     * @return Exchange rate data or null if not available
     */
    suspend fun getExchangeRateByDate(baseCurrency: String, targetCurrency: String, date: String): CurrencyRateData?
}

/**
 * Domain model for currency exchange rate
 */
data class CurrencyRateData(
    val baseCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    val date: String
)
