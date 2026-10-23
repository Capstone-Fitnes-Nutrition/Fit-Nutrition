package sheridan.dheripu.fitnutrition.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.ActivityLevel
import sheridan.dheripu.fitnutrition.model.health.HealthAlert
import sheridan.dheripu.fitnutrition.model.health.HealthAlertEvaluator
import sheridan.dheripu.fitnutrition.repository.HealthRepository
import java.text.SimpleDateFormat
import java.util.*

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val fitbitService = FitbitService(application.applicationContext)
    private val healthRepository = HealthRepository()

    // State flows for UI
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _healthMetrics = MutableStateFlow<HealthMetrics?>(null)
    val healthMetrics: StateFlow<HealthMetrics?> = _healthMetrics.asStateFlow()

    private val _weeklyMetrics = MutableStateFlow<List<HealthMetrics>>(emptyList())
    val weeklyMetrics: StateFlow<List<HealthMetrics>> = _weeklyMetrics.asStateFlow()

    /** BR4 and BR5 alerts, recomputed whenever today's or the week's metrics change. */
    val alerts: StateFlow<List<HealthAlert>> =
        combine(_healthMetrics, _weeklyMetrics) { today, week ->
            HealthAlertEvaluator.evaluate(today, week)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    companion object {
        private const val TAG = "HealthViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }

    init {
        Log.d(TAG, "HealthViewModel initialized")
        observeWeeklyHistory()
        checkAuthentication()
    }

    /**
     * Check if user is connected to Fitbit. An expired access token still counts when a
     * refresh token is stored, because the next sync renews it.
     */
    fun checkAuthentication() {
        val isAuth = fitbitService.isUserAuthenticated() || fitbitService.hasStoredSession()
        _isAuthenticated.value = isAuth
        Log.d(TAG, "checkAuthentication: isAuthenticated = $isAuth")

        if (isAuth) fetchHealthMetrics()
    }

    /**
     * Get Fitbit OAuth authorization URL
     */
    fun getAuthorizationUrl(): String = fitbitService.generateAuthorizationUrl()

    /**
     * Handle OAuth callback with authorization code
     * IMPORTANT: This must be called from your MainActivity or deep link handler
     */
    fun handleAuthorizationCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            fitbitService.exchangeCodeForToken(code)
                .onSuccess {
                    _isAuthenticated.value = true
                    _errorMessage.value = "Login successful! Fetching your data..."
                    fetchHealthMetrics()
                }.onFailure { exception ->
                    Log.e(TAG, "Token exchange failed: ${exception.message}", exception)
                    _errorMessage.value = "Login failed: ${exception.message}"
                    _isAuthenticated.value = false
                }

            _isLoading.value = false
        }
    }

    /**
     * Sync today's activity, heart rate and sleep from Fitbit, then save the day to history.
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
                val tokenResult = fitbitService.ensureFreshToken()
                if (tokenResult.isFailure) {
                    handleSessionExpired()
                    return@launch
                }
                fitbitService.fetchDailyActivity()
                    .onSuccess { activity -> completeSync(activity) }
                    .onFailure { exception ->
                        _errorMessage.value = "Failed to fetch activity data: ${exception.message}"
                        if (exception.message?.contains("401") == true ||
                            exception.message?.contains("Unauthorized") == true
                        ) {
                            handleSessionExpired()
                        }
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Adds heart rate and sleep to today's activity; either may be missing without failing. */
    private suspend fun completeSync(activity: HealthMetrics) {
        val warnings = mutableListOf<String>()
        val heartRate = fitbitService.fetchHeartRateData()
            .onFailure { warnings += "heart rate" }
            .getOrDefault(activity.heartRate)
        val sleepMinutes = fitbitService.fetchSleepData()
            .onFailure { warnings += "sleep" }
            .getOrDefault(0)

        val metrics = activity.copy(
            date = today(),
            heartRate = heartRate,
            sleepMinutes = sleepMinutes,
            lastSyncTime = getCurrentTimeString(),
            activityLevel = ActivityLevel.from(activity.steps, activity.activeMinutes),
            timestamp = System.currentTimeMillis()
        )
        _healthMetrics.value = metrics
        if (warnings.isNotEmpty()) {
            _errorMessage.value = "Could not fetch ${warnings.joinToString(" or ")} data."
        }
        saveToHistory(metrics)
    }

    private suspend fun saveToHistory(metrics: HealthMetrics) {
        healthRepository.saveHealthMetrics(metrics).first()
            .onFailure { Log.w(TAG, "Could not save health history: ${it.message}") }
    }

    private fun observeWeeklyHistory() {
        viewModelScope.launch {
            healthRepository.getWeeklyHealthMetrics().collect { result ->
                result.onSuccess { _weeklyMetrics.value = it }
                    .onFailure { Log.w(TAG, "Weekly history unavailable: ${it.message}") }
            }
        }
    }

    private fun handleSessionExpired() {
        Log.w(TAG, "Fitbit session could not be renewed - logging out")
        fitbitService.clearFitbitUser()
        _isAuthenticated.value = false
        _errorMessage.value = "Session expired. Please login again."
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

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /**
     * Get current time as formatted string
     */
    private fun getCurrentTimeString(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return formatter.format(Date())
    }
}
