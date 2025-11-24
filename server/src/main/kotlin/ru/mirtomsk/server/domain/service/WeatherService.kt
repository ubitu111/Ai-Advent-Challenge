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
    
    /**
     * Get daily forecast for a specific date
     * @param latitude Latitude
     * @param longitude Longitude
     * @param date Date in format YYYY-MM-DD
     * @return Daily weather data for the specified date, or null if not found
     */
    suspend fun getDailyForecastByDate(latitude: Double, longitude: Double, date: String): DailyWeatherData?
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

/**
 * Domain model for daily weather forecast
 */
data class DailyWeatherData(
    val date: String,
    val temperatureMax: Double,
    val temperatureMin: Double,
    val weatherCode: Int,
    val windSpeedMax: Double,
    val description: String
)

