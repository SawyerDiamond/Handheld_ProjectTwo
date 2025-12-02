package com.project2.surfflow.repository

import com.project2.surfflow.data.MarineWeatherResponse
import com.project2.surfflow.data.SurfConditions
import com.project2.surfflow.network.NetworkModule

class SurfRepository {
    private val api = NetworkModule.marineWeatherApi
    
    suspend fun getSurfForecast(latitude: Double, longitude: Double): Result<List<SurfConditions>> {
        return try {
            val response = api.getMarineWeather(latitude, longitude)
            val conditions = parseMarineWeatherResponse(response)
            Result.success(conditions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseMarineWeatherResponse(response: MarineWeatherResponse): List<SurfConditions> {
        val hourly = response.hourly
        val conditions = mutableListOf<SurfConditions>()
        
        val timeList = hourly.time
        val waveHeightList = hourly.waveHeight
        val waveDirectionList = hourly.waveDirection
        val wavePeriodList = hourly.wavePeriod
        val swellHeightList = hourly.swellWaveHeight
        val swellDirectionList = hourly.swellWaveDirection
        val swellPeriodList = hourly.swellWavePeriod
        val windWaveHeightList = hourly.windWaveHeight
        val windWaveDirectionList = hourly.windWaveDirection
        val seaSurfaceTempList = hourly.seaSurfaceTemperature
        
        val maxSize = timeList.size
        
        for (i in 0 until maxSize) {
            conditions.add(
                SurfConditions(
                    waveHeight = waveHeightList.getOrNull(i) ?: 0.0,
                    wavePeriod = wavePeriodList.getOrNull(i) ?: 0.0,
                    waveDirection = waveDirectionList.getOrNull(i) ?: 0.0,
                    swellHeight = swellHeightList.getOrNull(i) ?: 0.0,
                    swellPeriod = swellPeriodList.getOrNull(i) ?: 0.0,
                    swellDirection = swellDirectionList.getOrNull(i) ?: 0.0,
                    windWaveHeight = windWaveHeightList.getOrNull(i) ?: 0.0,
                    windDirection = windWaveDirectionList.getOrNull(i) ?: 0.0,
                    seaSurfaceTemperature = seaSurfaceTempList.getOrNull(i) ?: 0.0,
                    time = timeList.getOrNull(i) ?: ""
                )
            )
        }
        
        return conditions
    }
}

