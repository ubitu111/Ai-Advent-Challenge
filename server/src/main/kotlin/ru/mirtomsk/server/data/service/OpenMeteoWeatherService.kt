package ru.mirtomsk.server.data.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import ru.mirtomsk.server.data.model.CurrentWeatherResponse
import ru.mirtomsk.server.data.model.DailyForecastResponse
import ru.mirtomsk.server.data.model.GeocodingResponse
import ru.mirtomsk.server.data.model.HourlyForecastResponse
import ru.mirtomsk.server.domain.service.CurrentWeatherData
import ru.mirtomsk.server.domain.service.DailyWeatherData
import ru.mirtomsk.server.domain.service.HourlyWeatherData
import ru.mirtomsk.server.domain.service.WeatherService

/**
 * Implementation of WeatherService using Open Meteo API
 */
class OpenMeteoWeatherService(
    private val httpClient: HttpClient
) : WeatherService {
    
    companion object {
        private const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/v1"
        private const val WEATHER_BASE_URL = "https://api.open-meteo.com/v1"
        
        // Weather code mapping (WMO Weather interpretation codes)
        private val weatherCodeMap = mapOf(
            0 to "Ясно",
            1 to "Преимущественно ясно",
            2 to "Переменная облачность",
            3 to "Пасмурно",
            45 to "Туман",
            48 to "Осаждающийся иней",
            51 to "Легкая морось",
            53 to "Умеренная морось",
            55 to "Сильная морось",
            56 to "Легкая ледяная морось",
            57 to "Сильная ледяная морось",
            61 to "Небольшой дождь",
            63 to "Умеренный дождь",
            65 to "Сильный дождь",
            66 to "Легкий ледяной дождь",
            67 to "Сильный ледяной дождь",
            71 to "Небольшой снег",
            73 to "Умеренный снег",
            75 to "Сильный снег",
            77 to "Снежные зерна",
            80 to "Небольшой ливень",
            81 to "Умеренный ливень",
            82 to "Сильный ливень",
            85 to "Небольшой снегопад",
            86 to "Сильный снегопад",
            95 to "Гроза",
            96 to "Гроза с градом",
            99 to "Сильная гроза с градом"
        )
    }
    
    override suspend fun getCoordinates(city: String): Pair<Double, Double>? {
        return try {
            val response: GeocodingResponse = httpClient.get("$GEOCODING_BASE_URL/search") {
                parameter("name", city)
                parameter("count", 1)
                parameter("language", "ru")
                parameter("format", "json")
            }.body()
            
            val result = response.results?.firstOrNull()
            if (result?.latitude != null && result.longitude != null) {
                Pair(result.latitude, result.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getCurrentWeather(latitude: Double, longitude: Double): CurrentWeatherData? {
        return try {
            val response: CurrentWeatherResponse = httpClient.get("$WEATHER_BASE_URL/forecast") {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("current", "temperature_2m,weather_code,wind_speed_10m")
                parameter("timezone", "auto")
            }.body()
            
            val current = response.current ?: return null
            val temperature = current.temperature_2m ?: return null
            val weatherCode = current.weather_code ?: return null
            val windSpeed = current.wind_speed_10m ?: return null
            val time = current.time ?: return null
            
            CurrentWeatherData(
                temperature = temperature,
                weatherCode = weatherCode,
                windSpeed = windSpeed,
                time = time,
                description = weatherCodeMap[weatherCode] ?: "Неизвестно"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getHourlyForecast(
        latitude: Double,
        longitude: Double,
        hours: Int
    ): List<HourlyWeatherData>? {
        return try {
            val forecastDays = (hours / 24.0).coerceAtLeast(1.0).toInt().coerceAtMost(7)
            
            val response: HourlyForecastResponse = httpClient.get("$WEATHER_BASE_URL/forecast") {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("hourly", "temperature_2m,weather_code,wind_speed_10m")
                parameter("forecast_days", forecastDays)
                parameter("timezone", "auto")
            }.body()
            
            val hourly = response.hourly ?: return null
            val times = hourly.time ?: return null
            val temperatures = hourly.temperature_2m ?: return null
            val weatherCodes = hourly.weather_code ?: return null
            val windSpeeds = hourly.wind_speed_10m ?: return null
            
            val result = mutableListOf<HourlyWeatherData>()
            val count = minOf(hours, times.size, temperatures.size, weatherCodes.size, windSpeeds.size)
            
            for (i in 0 until count) {
                result.add(
                    HourlyWeatherData(
                        time = times[i],
                        temperature = temperatures[i],
                        weatherCode = weatherCodes[i],
                        windSpeed = windSpeeds[i],
                        description = weatherCodeMap[weatherCodes[i]] ?: "Неизвестно"
                    )
                )
            }
            
            result
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getDailyForecastByDate(
        latitude: Double,
        longitude: Double,
        date: String
    ): DailyWeatherData? {
        return try {
            // Open Meteo supports daily forecast up to 16 days ahead
            val response: DailyForecastResponse = httpClient.get("$WEATHER_BASE_URL/forecast") {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("daily", "temperature_2m_max,temperature_2m_min,weather_code,wind_speed_10m_max")
                parameter("forecast_days", 16)
                parameter("timezone", "auto")
            }.body()
            
            val daily = response.daily ?: return null
            val times = daily.time ?: return null
            val tempMax = daily.temperature_2m_max ?: return null
            val tempMin = daily.temperature_2m_min ?: return null
            val weatherCodes = daily.weather_code ?: return null
            val windSpeeds = daily.wind_speed_10m_max ?: return null
            
            // Find the index of the requested date
            val dateIndex = times.indexOfFirst { it.startsWith(date) }
            if (dateIndex == -1 || dateIndex >= tempMax.size || dateIndex >= tempMin.size || 
                dateIndex >= weatherCodes.size || dateIndex >= windSpeeds.size) {
                return null
            }
            
            DailyWeatherData(
                date = times[dateIndex],
                temperatureMax = tempMax[dateIndex],
                temperatureMin = tempMin[dateIndex],
                weatherCode = weatherCodes[dateIndex],
                windSpeedMax = windSpeeds[dateIndex],
                description = weatherCodeMap[weatherCodes[dateIndex]] ?: "Неизвестно"
            )
        } catch (e: Exception) {
            null
        }
    }
}
