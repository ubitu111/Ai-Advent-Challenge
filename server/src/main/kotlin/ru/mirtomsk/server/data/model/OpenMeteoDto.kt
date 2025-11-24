package ru.mirtomsk.server.data.model

import kotlinx.serialization.Serializable

/**
 * DTO for Open Meteo Geocoding API response
 */
@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@Serializable
data class GeocodingResult(
    val id: Int? = null,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val country: String? = null,
    val admin1: String? = null // State/Province
)

/**
 * DTO for Open Meteo Current Weather API response
 */
@Serializable
data class CurrentWeatherResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val current: CurrentWeather? = null
)

@Serializable
data class CurrentWeather(
    val time: String? = null,
    val temperature_2m: Double? = null,
    val weather_code: Int? = null,
    val wind_speed_10m: Double? = null
)

/**
 * DTO for Open Meteo Hourly Forecast API response
 */
@Serializable
data class HourlyForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val hourly: HourlyForecast? = null
)

@Serializable
data class HourlyForecast(
    val time: List<String>? = null,
    val temperature_2m: List<Double>? = null,
    val weather_code: List<Int>? = null,
    val wind_speed_10m: List<Double>? = null
)

/**
 * DTO for Open Meteo Daily Forecast API response
 */
@Serializable
data class DailyForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val daily: DailyForecast? = null
)

@Serializable
data class DailyForecast(
    val time: List<String>? = null,
    val temperature_2m_max: List<Double>? = null,
    val temperature_2m_min: List<Double>? = null,
    val weather_code: List<Int>? = null,
    val wind_speed_10m_max: List<Double>? = null
)
