package com.project2.surfflow.network

import com.project2.surfflow.data.MarineWeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MarineWeatherApi {
    @GET("v1/marine")
    suspend fun getMarineWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "wave_height,wave_direction,wave_period,swell_wave_height,swell_wave_direction,swell_wave_period,wind_wave_height,wind_wave_direction,wind_wave_period,sea_surface_temperature",
        @Query("forecast_days") forecastDays: Int = 7
    ): MarineWeatherResponse
}

