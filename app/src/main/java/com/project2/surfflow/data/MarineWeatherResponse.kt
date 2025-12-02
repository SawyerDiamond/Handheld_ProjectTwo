package com.project2.surfflow.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MarineWeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("generationtime_ms") val generationTimeMs: Double,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int,
    val timezone: String,
    @SerialName("timezone_abbreviation") val timezoneAbbreviation: String,
    val hourly: HourlyData,
    @SerialName("hourly_units") val hourlyUnits: HourlyUnits
)

@Serializable
data class HourlyData(
    val time: List<String>,
    @SerialName("wave_height") val waveHeight: List<Double?>,
    @SerialName("wave_direction") val waveDirection: List<Double?>,
    @SerialName("wave_period") val wavePeriod: List<Double?>,
    @SerialName("swell_wave_height") val swellWaveHeight: List<Double?>,
    @SerialName("swell_wave_direction") val swellWaveDirection: List<Double?>,
    @SerialName("swell_wave_period") val swellWavePeriod: List<Double?>,
    @SerialName("wind_wave_height") val windWaveHeight: List<Double?>,
    @SerialName("wind_wave_direction") val windWaveDirection: List<Double?>,
    @SerialName("wind_wave_period") val windWavePeriod: List<Double?>,
    @SerialName("sea_surface_temperature") val seaSurfaceTemperature: List<Double?>
)

@Serializable
data class HourlyUnits(
    @SerialName("wave_height") val waveHeight: String,
    @SerialName("wave_direction") val waveDirection: String,
    @SerialName("wave_period") val wavePeriod: String
)

