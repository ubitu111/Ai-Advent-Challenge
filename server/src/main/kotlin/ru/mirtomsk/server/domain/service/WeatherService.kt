package ru.mirtomsk.server.domain.service

/**
 * Domain service interface for weather data
 */
interface WeatherService {
    /**
     * Get coordinates for a city name
     */
    suspend fun getCoordinates(city: String): Pair<Double, Double>?
    
    /**
     * Get current weather for coordinates
     */
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): CurrentWeatherData?
    
    /**
     * Get hourly forecast for coordinates
     */
    suspend fun getHourlyForecast(latitude: Double, longitude: Double, hours: Int = 24): List<HourlyWeatherData>?
}

/**
 * Domain model for current weather
 */
data class CurrentWeatherData(
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val time: String,
    val description: String
)

/**
 * Domain model for hourly weather forecast
 */
data class HourlyWeatherData(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val description: String
)

