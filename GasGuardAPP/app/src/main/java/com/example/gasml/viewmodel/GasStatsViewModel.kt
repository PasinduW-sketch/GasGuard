package com.example.gasml.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gasml.data.GasStatsRepository
import com.example.gasml.model.GasStats
import com.example.gasml.util.NetworkObserver
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GasStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GasStatsRepository()
    private val networkObserver = NetworkObserver(application)

    private val _stats = MutableStateFlow<GasStats?>(null)
    val stats: StateFlow<GasStats?> = _stats.asStateFlow()

    private val _networkStatus = MutableStateFlow(NetworkObserver.Status.Unavailable)
    val networkStatus: StateFlow<NetworkObserver.Status> = _networkStatus.asStateFlow()

    private val _daysRemaining = MutableStateFlow(0.0)
    val daysRemaining = _daysRemaining.asStateFlow()

    private val _refillDate = MutableStateFlow("Calculating...")
    val refillDate = _refillDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkObserver.observe.collect {
                _networkStatus.value = it
            }
        }
    }

    fun loadStats(unitId: String?) {
        if (unitId == null) {
            _stats.value = getMockStats("MOCK_UNIT")
            updateDerivedStats(_stats.value!!)
            return
        }
        
        val formattedId = if (unitId.startsWith("UNIT_")) unitId else "UNIT_$unitId"
        
        viewModelScope.launch {
            _isLoading.value = true
            repository.getUnitStats(formattedId).collectLatest { updatedStats ->
                // Clean data before emitting to UI to prevent "255%" errors
                val cleanedStats = updatedStats?.copy(
                    gasPercentage = updatedStats.gasPercentage.coerceIn(0.0, 100.0),
                    leakPercentage = updatedStats.leakPercentage.coerceIn(0.0, 100.0),
                    gasLevel = updatedStats.gasLevel.coerceIn(0L, 100L)
                )
                
                val finalStats = cleanedStats ?: getMockStats(formattedId)
                _stats.value = finalStats
                updateDerivedStats(finalStats)
                _isLoading.value = false
            }
        }
    }

    fun toggleLeakSimulation(unitId: String?, isLeak: Boolean) {
        val id = unitId ?: return
        val formattedId = if (id.startsWith("UNIT_")) id else "UNIT_$id"
        viewModelScope.launch {
            repository.simulateLeak(formattedId, isLeak)
        }
    }

    fun acknowledgeLeak(unitId: String?) {
        val id = unitId ?: return
        val formattedId = if (id.startsWith("UNIT_")) id else "UNIT_$id"
        viewModelScope.launch {
            repository.acknowledgeLeak(formattedId)
        }
    }

    private fun getMockStats(unitId: String): GasStats {
        return GasStats(
            unitId = unitId,
            currentWeight = 5.0,
            gasLevel = 100L,
            gasPercentage = 100.0,
            leakDetected = false,
            leakPercentage = 0.0,
            systemStatus = "NORMAL",
            temperature = 27.5,
            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),
            valveClosed = false,
            dailyUsage = 0.12,
            daysRemaining = 41L,
            estimatedCost = 1200.0,
            capacity = "5kg"
        )
    }

    private fun updateDerivedStats(stats: GasStats) {
        _daysRemaining.value = stats.daysRemaining.toDouble()

        if (stats.daysRemaining > 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, stats.daysRemaining.toInt())
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            _refillDate.value = sdf.format(calendar.time)
        } else {
            _refillDate.value = "N/A"
        }
    }

    fun formatESP32Timestamp(timestamp: Any?): String {
        return when (timestamp) {
            is String -> {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val date = inputFormat.parse(timestamp)
                    val outputFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
                    date?.let { outputFormat.format(it) } ?: "Just now"
                } catch (e: Exception) {
                    "Just now"
                }
            }
            is Timestamp -> {
                val outputFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
                outputFormat.format(timestamp.toDate())
            }
            else -> "Just now"
        }
    }
    
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
        return sdf.format(Date())
    }
}
