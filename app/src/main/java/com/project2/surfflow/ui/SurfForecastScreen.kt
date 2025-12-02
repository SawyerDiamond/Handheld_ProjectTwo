package com.project2.surfflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project2.surfflow.R
import com.project2.surfflow.data.LongIslandSurfSpots
import com.project2.surfflow.data.SurfConditions
import com.project2.surfflow.data.SurfSpot
import com.project2.surfflow.data.calculateSurfGrade
import com.project2.surfflow.data.celsiusToFahrenheit
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurfForecastScreen(
    viewModel: SurfViewModel = viewModel(),
    onNavigateToProfile: () -> Unit,
    onNavigateToAi: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spots = LongIslandSurfSpots.spots
    var selectedSpot by remember { mutableStateOf(spots.first()) }
    
    // Generate context string for AI
    val currentConditions = if (state.conditions.isNotEmpty()) getCurrentConditions(state.conditions) else null
    val contextString = remember(selectedSpot, currentConditions) {
        if (currentConditions != null) {
            "Current surf conditions at ${selectedSpot.name}: " +
            "Wave Height: ${String.format("%.1f", currentConditions.waveHeight)}m, " +
            "Wave Period: ${String.format("%.1f", currentConditions.wavePeriod)}s, " +
            "Wind Direction: ${currentConditions.windDirection}°, " +
            "Water Temp: ${String.format("%.0f", celsiusToFahrenheit(currentConditions.seaSurfaceTemperature))}°F."
        } else {
            "I am at ${selectedSpot.name} but I don't have the forecast data yet."
        }
    }
    
    LaunchedEffect(Unit) {
        if (state.conditions.isEmpty() && !state.isLoading) {
            viewModel.loadForecast(selectedSpot.latitude, selectedSpot.longitude)
        }
    }
    
    LaunchedEffect(selectedSpot) {
        viewModel.loadForecast(selectedSpot.latitude, selectedSpot.longitude)
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SurfFlow",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { onNavigateToAi(contextString) }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Advisor",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            LocationDropdown(
                spots = spots,
                selectedSpot = selectedSpot,
                onSpotSelected = { spot ->
                    selectedSpot = spot
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.error != null -> {
                    val errorMessage = state.error
                    ErrorMessage(errorMessage ?: "Unknown error") {
                        viewModel.loadForecast(selectedSpot.latitude, selectedSpot.longitude)
                    }
                }
                state.conditions.isNotEmpty() -> {
                    val currentConditions = getCurrentConditions(state.conditions)
                    val currentIndex = getCurrentConditionsIndex(state.conditions)
                    val futureForecast = if (currentIndex >= 0 && currentIndex < state.conditions.size - 1) {
                        state.conditions.subList(currentIndex + 1, minOf(currentIndex + 25, state.conditions.size))
                    } else {
                        state.conditions.take(24)
                    }
                    CurrentConditionsCard(currentConditions)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Next 6 Days Panel
                    val nextSixDays = remember(state.conditions) { getNextSixDaysNoon(state.conditions) }
                    if (nextSixDays.isNotEmpty()) {
                        NextSixDaysPanel(nextSixDays)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    ForecastList(futureForecast)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDropdown(
    spots: List<SurfSpot>,
    selectedSpot: SurfSpot,
    onSpotSelected: (SurfSpot) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var favoriteSpotName by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // Fetch favorite spot
    LaunchedEffect(user) {
        if (user != null) {
            val database = FirebaseDatabase.getInstance("https://surfflow-6f219-default-rtdb.firebaseio.com/")
            val myRef = database.getReference("users/${user.uid}/favorite_spot")
            myRef.get().addOnSuccessListener { snapshot ->
                favoriteSpotName = snapshot.value as? String
                // Auto-select favorite if it exists and current selection is default
                if (favoriteSpotName != null) {
                    val favSpot = spots.find { it.name == favoriteSpotName }
                    if (favSpot != null) {
                        onSpotSelected(favSpot)
                    }
                }
            }
        }
    }

    // Sort spots: Favorite first, then alphabetical
    val sortedSpots = remember(favoriteSpotName) {
        if (favoriteSpotName != null) {
            val fav = spots.find { it.name == favoriteSpotName }
            val others = spots.filter { it.name != favoriteSpotName }
            if (fav != null) listOf(fav) + others else spots
        } else {
            spots
        }
    }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedSpot.name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), // Increased radius
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            sortedSpots.forEach { spot ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (spot.name == favoriteSpotName) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Favorite",
                                    tint = Color(0xFFFFD700), // Gold/Yellow
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(spot.name)
                        }
                    },
                    onClick = {
                        onSpotSelected(spot)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CurrentConditionsCard(conditions: SurfConditions?) {
    if (conditions == null) return
    
    val grade = calculateSurfGrade(conditions)
    val gradeColor = Color(android.graphics.Color.parseColor(grade.colorHex))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Conditions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(conditions.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    color = gradeColor,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = grade.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ConditionItem(stringResource(R.string.wave_height), "${String.format("%.1f", conditions.waveHeight)} m")
                ConditionItem(stringResource(R.string.period), "${String.format("%.1f", conditions.wavePeriod)} s")
                ConditionItem(stringResource(R.string.direction), "${conditions.waveDirection.toInt()}°")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ConditionItem(stringResource(R.string.swell), "${String.format("%.1f", conditions.swellHeight)} m")
                ConditionItem(stringResource(R.string.swell_period), "${String.format("%.1f", conditions.swellPeriod)} s")
                ConditionItem(stringResource(R.string.water_temp), "${String.format("%.0f", celsiusToFahrenheit(conditions.seaSurfaceTemperature))}°F")
            }
        }
    }
}

@Composable
fun ConditionItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ForecastList(conditions: List<SurfConditions>) {
    Column {
        Text(
            text = "Hourly Forecast",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            conditions.forEach { condition ->
                ForecastItem(condition)
            }
        }
    }
}

@Composable
fun ForecastItem(condition: SurfConditions) {
    val grade = calculateSurfGrade(condition)
    val gradeColor = Color(android.graphics.Color.parseColor(grade.colorHex))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTime(condition.time, showDate = false), // No Date
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Wave: ${String.format("%.1f", condition.waveHeight)}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Period: ${String.format("%.1f", condition.wavePeriod)}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Surface(
                color = gradeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = grade.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = gradeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.error_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry_button))
            }
        }
    }
}

fun getCurrentConditions(conditions: List<SurfConditions>): SurfConditions? {
    val index = getCurrentConditionsIndex(conditions)
    return if (index >= 0) conditions[index] else conditions.firstOrNull()
}

fun getCurrentConditionsIndex(conditions: List<SurfConditions>): Int {
    if (conditions.isEmpty()) return -1
    
    val now = System.currentTimeMillis()
    val inputFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
    )
    
    var closestIndex = -1
    var minTimeDiff = Long.MAX_VALUE
    
    for (i in conditions.indices) {
        val condition = conditions[i]
        for (inputFormat in inputFormats) {
            try {
                val forecastTime = inputFormat.parse(condition.time)?.time
                if (forecastTime != null) {
                    val timeDiff = kotlin.math.abs(forecastTime - now)

                    if (forecastTime >= now - 3600000 && timeDiff < minTimeDiff) {
                        minTimeDiff = timeDiff
                        closestIndex = i
                    }
                }
            } catch (e: Exception) {}
        }
    }
    return if (closestIndex >= 0) closestIndex else 0
}

//Used Gemini to help format: Parse ISO 8601  "2022-07-01T00:00" or "2022-07-01T00:00:00"
fun formatTime(timeString: String, showDate: Boolean = true): String {
    return try {
        val inputFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        )
        val outputPattern = if (showDate) "MMM d, h:mm a" else "h:mm a"
        val outputFormat = SimpleDateFormat(outputPattern, Locale.getDefault())
        
        for (inputFormat in inputFormats) {
            try {
                val date = inputFormat.parse(timeString)
                if (date != null) {
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {}
        }
        timeString
    } catch (e: Exception) {
        timeString
    }
}

@Composable
fun NextSixDaysPanel(conditions: List<SurfConditions>) {
    Column {
        Text(
            text = "Next 6 Days (Noon)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = conditions.chunked(3)
            rows.forEach { rowConditions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowConditions.forEach { condition ->
                        DayForecastCard(
                            condition = condition,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty space if last row has fewer than 3 items (though we expect 6)
                    repeat(3 - rowConditions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DayForecastCard(
    condition: SurfConditions,
    modifier: Modifier = Modifier
) {
    val grade = calculateSurfGrade(condition)
    val gradeColor = Color(android.graphics.Color.parseColor(grade.colorHex))
    val dateStr = try {
        val inputFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        )
        var parsedDate: java.util.Date? = null
        for (format in inputFormats) {
            try {
                parsedDate = format.parse(condition.time)
                if (parsedDate != null) break
            } catch (e: Exception) {}
        }
        
        if (parsedDate != null) {
            SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(parsedDate)
        } else {
            "Unknown"
        }
    } catch (e: Exception) {
        "Unknown"
    }

    Card(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = gradeColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = grade.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${String.format("%.1f", condition.waveHeight)}m",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${String.format("%.0f", condition.wavePeriod)}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

fun getNextSixDaysNoon(conditions: List<SurfConditions>): List<SurfConditions> {
    val result = mutableListOf<SurfConditions>()
    val inputFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
    )
    
    val seenDates = mutableSetOf<String>()
    val calendar = java.util.Calendar.getInstance()
    val todayDayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    
    for (condition in conditions) {
        for (format in inputFormats) {
            try {
                val date = format.parse(condition.time) ?: continue
                calendar.time = date
                val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)

                // Skip today, look for next days, target noon (12:00)
                // We accept 11:00, 12:00, 13:00 if exact noon is missing, but prefer 12
                if (dayOfYear != todayDayOfYear && !seenDates.contains(dateKey)) {
                     if (hour == 12) {
                        result.add(condition)
                        seenDates.add(dateKey)
                        break
                    }
                }
            } catch (e: Exception) {}
        }
        if (result.size >= 6) break
    }
    
    // If we missed some days because exact noon wasn't there, we could do a second pass, 
    // but for now let's assume the API returns hourly data so 12:00 should exist.
    
    return result
}
