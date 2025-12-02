package com.project2.surfflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project2.surfflow.data.SurfConditions
import com.project2.surfflow.data.SurfGrade
import com.project2.surfflow.data.calculateSurfGrade
import com.project2.surfflow.repository.SurfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SurfForecastState(
    val conditions: List<SurfConditions> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentLocation: Pair<Double, Double>? = null
)

class SurfViewModel : ViewModel() {
    private val repository = SurfRepository()
    
    private val _state = MutableStateFlow(SurfForecastState())
    val state: StateFlow<SurfForecastState> = _state.asStateFlow()
    
    fun loadForecast(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            repository.getSurfForecast(latitude, longitude)
                .onSuccess { conditions ->
                    _state.value = _state.value.copy(
                        conditions = conditions,
                        isLoading = false,
                        currentLocation = Pair(latitude, longitude)
                    )
                }
                .onFailure { exception ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load forecast"
                    )
                }
        }
    }
    
    fun getCurrentConditions(): SurfConditions? {
        return _state.value.conditions.firstOrNull()
    }
    
    fun getCurrentGrade(): SurfGrade? {
        return getCurrentConditions()?.let { calculateSurfGrade(it) }
    }
}

