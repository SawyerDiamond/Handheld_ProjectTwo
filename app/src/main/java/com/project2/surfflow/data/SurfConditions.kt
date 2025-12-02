package com.project2.surfflow.data

data class SurfConditions(
    val waveHeight: Double,
    val wavePeriod: Double,
    val waveDirection: Double,
    val swellHeight: Double,
    val swellPeriod: Double,
    val swellDirection: Double,
    val windWaveHeight: Double,
    val windDirection: Double,
    val seaSurfaceTemperature: Double,
    val time: String
)

enum class SurfGrade(val displayName: String, val colorHex: String) {
    EXCELLENT("Excellent", "#4CAF50"),
    GOOD("Good", "#8BC34A"),
    FAIR("Fair", "#FFC107"),
    POOR("Poor", "#FF9800"),
    FLAT("Flat", "#9E9E9E")
}

fun celsiusToFahrenheit(celsius: Double): Double {
    return (celsius * 9.0 / 5.0) + 32.0
}

fun calculateSurfGrade(conditions: SurfConditions): SurfGrade {
    val waveHeight = conditions.waveHeight
    val wavePeriod = conditions.wavePeriod
    
    // Flat conditions
    if (waveHeight < 0.3) {
        return SurfGrade.FLAT
    }
    
    // Calculate score based on multiple factors (stricter criteria)
    var score = 0.0
    
    // Wave height scoring (ideal: 1.0m - 2.0m for most breaks)
    // Stricter ranges, less generous scoring
    when {
        waveHeight in 1.0..2.0 -> score += 25.0  // Ideal range
        waveHeight in 0.8..1.0 || waveHeight in 2.0..2.5 -> score += 15.0  // Good
        waveHeight in 0.5..0.8 || waveHeight in 2.5..3.0 -> score += 8.0   // Fair
        waveHeight in 0.3..0.5 || waveHeight in 3.0..4.0 -> score += 3.0     // Poor
    }
    
    // Wave period scoring (stricter - longer periods are crucial)
    // 10+ seconds is excellent, 8-10 is good, 6-8 is fair, below 6 is poor
    when {
        wavePeriod >= 12.0 -> score += 30.0  // Excellent - long period swell
        wavePeriod >= 10.0 -> score += 20.0  // Good
        wavePeriod >= 8.0 -> score += 12.0   // Fair
        wavePeriod >= 6.0 -> score += 6.0    // Below average
        wavePeriod >= 4.0 -> score += 2.0    // Poor
        else -> score += 0.0                 // Very poor
    }
    
    // Swell vs wind wave ratio (more swell is better, but stricter)
    val swellRatio = if (conditions.waveHeight > 0) {
        conditions.swellHeight / conditions.waveHeight
    } else {
        0.0
    }
    when {
        swellRatio >= 0.8 -> score += 20.0  // Excellent - mostly swell
        swellRatio >= 0.6 -> score += 12.0  // Good
        swellRatio >= 0.4 -> score += 6.0   // Fair
        swellRatio >= 0.2 -> score += 2.0   // Poor - too much wind wave
        else -> score += 0.0                // Very poor
    }
    
    // Additional penalty for very short periods (makes conditions choppy)
    if (wavePeriod < 6.0 && waveHeight > 0.5) {
        score -= 10.0  // Penalty for short period with decent height (choppy)
    }
    
    // Ensure score doesn't go negative
    score = score.coerceAtLeast(0.0)
    
    // Stricter thresholds for grades
    return when {
        score >= 65.0 -> SurfGrade.EXCELLENT  // Raised from 80
        score >= 45.0 -> SurfGrade.GOOD       // Raised from 60
        score >= 25.0 -> SurfGrade.FAIR       // Raised from 40
        else -> SurfGrade.POOR
    }
}

