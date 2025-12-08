package sheridan.dheripu.fitnutrition.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import java.text.SimpleDateFormat
import java.util.*

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val fitbitService = FitbitService(application.applicationContext)

    // State flows for UI–
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _healthMetrics = MutableStateFlow<HealthMetrics?>(null)
    val healthMetrics: StateFlow<HealthMetrics?> = _healthMetrics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkAuthentication()
    }

    /**
     * Check if user is authenticated with Fitbit
     */
    fun checkAuthentication() {
        _isAuthenticated.value = fitbitService.isUserAuthenticated()
    }

    /**
     * Get Fitbit OAuth authorization URL
     */
    fun getAuthorizationUrl(): String {
        return fitbitService.generateAuthorizationUrl()
    }

    /**
     * Handle OAuth callback with authorization code
     */
    fun handleAuthorizationCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = fitbitService.exchangeCodeForToken(code)

            result.onSuccess {
                _isAuthenticated.value = true
                // Automatically fetch metrics after successful login
                fetchHealthMetrics()
            }.onFailure { exception ->
                _errorMessage.value = "Login failed: ${exception.message}"
                _isAuthenticated.value = false
            }

            _isLoading.value = false
        }
    }

    /**
     * Fetch all health metrics from Fitbit
     */
    fun fetchHealthMetrics() {
        if (!_isAuthenticated.value) {
            _errorMessage.value = "Please login to Fitbit first"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Fetch activity data (steps, distance, calories, active minutes)
                val activityResult = fitbitService.fetchDailyActivity()

                activityResult.onSuccess { activityMetrics ->
                    // Fetch heart rate data
                    val heartRateResult = fitbitService.fetchHeartRateData()

                    heartRateResult.onSuccess { heartRate ->
                        // Combine all data
                        val combinedMetrics = activityMetrics.copy(
                            heartRate = heartRate,
                            lastSyncTime = getCurrentTimeString(),
                            activityLevel = calculateActivityLevel(activityMetrics.steps, activityMetrics.activeMinutes)
                        )

                        _healthMetrics.value = combinedMetrics

                    }.onFailure { exception ->
                        // Still show activity data even if heart rate fails
                        _healthMetrics.value = activityMetrics.copy(
                            lastSyncTime = getCurrentTimeString(),
                            activityLevel = calculateActivityLevel(activityMetrics.steps, activityMetrics.activeMinutes)
                        )
                        _errorMessage.value = "Could not fetch heart rate: ${exception.message}"
                    }

                }.onFailure { exception ->
                    _errorMessage.value = "Failed to fetch activity data: ${exception.message}"

                    // Check if token expired
                    if (exception.message?.contains("401") == true || 
                        exception.message?.contains("Unauthorized") == true) {
                        _isAuthenticated.value = false
                        _errorMessage.value = "Session expired. Please login again."
                    }
                }

            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Logout from Fitbit and clear all data
     */
    fun logout() {
        fitbitService.clearFitbitUser()
        _isAuthenticated.value = false
        _healthMetrics.value = null
        _errorMessage.value = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Calculate activity level based on steps and active minutes
     */
    private fun calculateActivityLevel(steps: Int, activeMinutes: Int): String {
        return when {
            steps >= 10000 && activeMinutes >= 30 -> "HIGH"
            steps >= 5000 && activeMinutes >= 15 -> "MODERATE"
            steps >= 2500 -> "LOW"
            else -> "SEDENTARY"
        }
    }

    /**
     * Get current time as formatted string
     */
    private fun getCurrentTimeString(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return formatter.format(Date())
    }
}
