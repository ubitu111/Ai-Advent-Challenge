package ru.mirtomsk.server.data.model

import kotlinx.serialization.Serializable

/**
 * DTO for ExchangeRate.host API response
 * Supports both formats:
 * - /latest endpoint: { base, date, rates }
 * - /live endpoint: { success, source, timestamp, quotes }
 */
@Serializable
data class ExchangeRateResponse(
    val success: Boolean? = null,
    val base: String? = null,
    val source: String? = null,
    val date: String? = null,
    val timestamp: Long? = null,
    val rates: Map<String, Double>? = null,
    val quotes: Map<String, Double>? = null,
    val query: ExchangeRateQuery? = null,
    val result: Double? = null,
    val info: ExchangeRateInfo? = null,
)

@Serializable
data class ExchangeRateQuery(
    val from: String,
    val to: String,
    val amount: Int,
)

@Serializable
data class ExchangeRateInfo(
    val timestamp: Long,
    val quote: Double,
)
