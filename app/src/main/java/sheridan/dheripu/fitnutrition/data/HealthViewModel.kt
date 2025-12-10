package sheridan.dheripu.fitnutrition.data

import android.app.Application
import android.util.Log
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

    // State flows for UI
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _healthMetrics = MutableStateFlow<HealthMetrics?>(null)
    val healthMetrics: StateFlow<HealthMetrics?> = _healthMetrics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    companion object {
        private const val TAG = "HealthViewModel"
    }

    init {
        Log.d(TAG, "HealthViewModel initialized")
        checkAuthentication()
    }

    /**
     * Check if user is authenticated with Fitbit
     */
    fun checkAuthentication() {
        val isAuth = fitbitService.isUserAuthenticated()
        _isAuthenticated.value = isAuth
        Log.d(TAG, "checkAuthentication: isAuthenticated = $isAuth")

        if (isAuth) {
            Log.d(TAG, "User is authenticated, fetching metrics automatically")
            fetchHealthMetrics()
        } else {
            Log.d(TAG, "User is NOT authenticated")
        }
    }

    /**
     * Get Fitbit OAuth authorization URL
     */
    fun getAuthorizationUrl(): String {
        val url = fitbitService.generateAuthorizationUrl()
        Log.d(TAG, "Generated authorization URL: $url")
        return url
    }

    /**
     * Handle OAuth callback with authorization code
     * IMPORTANT: This must be called from your MainActivity or deep link handler
     */
    fun handleAuthorizationCode(code: String) {
        Log.d(TAG, "handleAuthorizationCode called with code: ${code.take(10)}...")

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            Log.d(TAG, "Exchanging authorization code for access token...")
            val result = fitbitService.exchangeCodeForToken(code)

            result.onSuccess { authResponse ->
                Log.d(TAG, "✅ Token exchange SUCCESS!")
                Log.d(TAG, "User ID: ${authResponse.userId}")
                Log.d(TAG, "Token expires in: ${authResponse.expiresIn} seconds")

                _isAuthenticated.value = true
                _errorMessage.value = "Login successful! Fetching your data..."

                // Automatically fetch metrics after successful login
                Log.d(TAG, "Auto-fetching health metrics after login...")
                fetchHealthMetrics()

            }.onFailure { exception ->
                Log.e(TAG, "❌ Token exchange FAILED: ${exception.message}", exception)
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
        Log.d(TAG, "fetchHealthMetrics called")

        if (!_isAuthenticated.value) {
            Log.w(TAG, "Cannot fetch metrics - user not authenticated")
            _errorMessage.value = "Please login to Fitbit first"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            Log.d(TAG, "Starting health metrics fetch...")

            try {
                // Fetch activity data (steps, distance, calories, active minutes)
                Log.d(TAG, "Fetching daily activity data...")
                val activityResult = fitbitService.fetchDailyActivity()

                activityResult.onSuccess { activityMetrics ->
                    Log.d(TAG, "✅ Activity data received:")
                    Log.d(TAG, "  Steps: ${activityMetrics.steps}")
                    Log.d(TAG, "  Distance: ${activityMetrics.distance} km")
                    Log.d(TAG, "  Calories: ${activityMetrics.calories}")
                    Log.d(TAG, "  Active Minutes: ${activityMetrics.activeMinutes}")

                    // Fetch heart rate data
                    Log.d(TAG, "Fetching heart rate data...")
                    val heartRateResult = fitbitService.fetchHeartRateData()

                    heartRateResult.onSuccess { heartRate ->
                        Log.d(TAG, "✅ Heart rate received: $heartRate bpm")

                        // Combine all data
                        val combinedMetrics = activityMetrics.copy(
                            heartRate = heartRate,
                            lastSyncTime = getCurrentTimeString(),
                            activityLevel = calculateActivityLevel(activityMetrics.steps, activityMetrics.activeMinutes)
                        )

                        _healthMetrics.value = combinedMetrics
                        Log.d(TAG, "✅ All metrics combined and updated in UI")
                        Log.d(TAG, "Activity Level: ${combinedMetrics.activityLevel}")

                    }.onFailure { exception ->
                        Log.w(TAG, "⚠️ Heart rate fetch failed: ${exception.message}")
                        // Still show activity data even if heart rate fails
                        val metricsWithoutHR = activityMetrics.copy(
                            lastSyncTime = getCurrentTimeString(),
                            activityLevel = calculateActivityLevel(activityMetrics.steps, activityMetrics.activeMinutes)
                        )
                        _healthMetrics.value = metricsWithoutHR
                        _errorMessage.value = "Could not fetch heart rate: ${exception.message}"
                    }

                }.onFailure { exception ->
                    Log.e(TAG, "❌ Activity fetch failed: ${exception.message}", exception)
                    _errorMessage.value = "Failed to fetch activity data: ${exception.message}"

                    // Check if token expired
                    if (exception.message?.contains("401") == true ||
                        exception.message?.contains("Unauthorized") == true) {
                        Log.w(TAG, "Token expired or unauthorized - logging out")
                        _isAuthenticated.value = false
                        _errorMessage.value = "Session expired. Please login again."
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error: ${e.message}", e)
                _errorMessage.value = "Unexpected error: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "fetchHealthMetrics completed")
            }
        }
    }

    /**
     * Logout from Fitbit and clear all data
     */
    fun logout() {
        Log.d(TAG, "Logging out user")
        fitbitService.clearFitbitUser()
        _isAuthenticated.value = false
        _healthMetrics.value = null
        _errorMessage.value = null
        Log.d(TAG, "Logout complete - all data cleared")
    }

    /**
     * Clear error message
     */
    fun clearError() {
        Log.d(TAG, "Clearing error message")
        _errorMessage.value = null
    }

    /**
     * Calculate activity level based on steps and active minutes
     */
    private fun calculateActivityLevel(steps: Int, activeMinutes: Int): String {
        val level = when {
            steps >= 10000 && activeMinutes >= 30 -> "HIGH"
            steps >= 5000 && activeMinutes >= 15 -> "MODERATE"
            steps >= 2500 -> "LOW"
            else -> "SEDENTARY"
        }
        Log.d(TAG, "Calculated activity level: $level (steps=$steps, activeMin=$activeMinutes)")
        return level
    }

    /**
     * Get current time as formatted string
     */
    private fun getCurrentTimeString(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return formatter.format(Date())
    }
}
