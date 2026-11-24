package sheridan.dheripu.fitnutrition.data

import android.app.Application
import android.content.Context
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
import sheridan.dheripu.fitnutrition.BuildConfig
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.HealthAlert
import sheridan.dheripu.fitnutrition.model.health.HealthAlertEvaluator
import sheridan.dheripu.fitnutrition.repository.HealthRepository
import java.time.LocalDate
import java.time.ZoneId

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = application.applicationContext
        .getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)

    private val realSource = FitbitService(application.applicationContext)
    private val mockSource = MockFitbitDataSource()
    private val healthRepository = HealthRepository()

    private val _useMockData = MutableStateFlow(
        settings.getBoolean(KEY_USE_MOCK, BuildConfig.USE_MOCK_FITBIT)
    )

    /** True while the Health tab is reading the offline mock instead of a real Fitbit account. */
    val useMockData: StateFlow<Boolean> = _useMockData.asStateFlow()

    private val source: FitbitDataSource
        get() = if (_useMockData.value) mockSource else realSource

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

    /** False when no Fitbit credentials are configured, so the UI can say which keys are missing. */
    val isFitbitConfigured: Boolean get() = source.isConfigured

    init {
        observeWeeklyHistory()
        checkAuthentication()
    }

    /**
     * Whether the Health tab reads the mock or a real account. Kept in SharedPreferences so the
     * choice survives a restart, and switching resyncs straight away.
     */
    fun setUseMockData(enabled: Boolean) {
        if (_useMockData.value == enabled) return
        settings.edit().putBoolean(KEY_USE_MOCK, enabled).apply()
        _useMockData.value = enabled
        _healthMetrics.value = null
        _errorMessage.value = null
        checkAuthentication()
    }

    /**
     * Whether the chosen source can serve data. An expired access token still counts when a
     * refresh token is stored, because the next sync renews it.
     */
    fun checkAuthentication() {
        val connected = source.isConnected()
        _isAuthenticated.value = connected
        if (connected) fetchHealthMetrics()
    }

    fun getAuthorizationUrl(): String = source.authorizationUrl()

    /**
     * Finishes the OAuth redirect. [state] comes from the redirect and is checked against the
     * value the authorization URL was built with.
     */
    fun handleAuthorizationCode(code: String, state: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            source.completeAuthorization(code, state)
                .onSuccess {
                    _isAuthenticated.value = true
                    _errorMessage.value = "Fitbit connected. Syncing your data."
                    syncRecentDays()
                }
                .onFailure { error ->
                    Log.e(TAG, "Fitbit authorization failed: ${error.message}")
                    _errorMessage.value = error.message ?: "Could not connect Fitbit."
                    _isAuthenticated.value = false
                }
            _isLoading.value = false
        }
    }

    /** Syncs today, then backfills the rest of the week in the background. */
    fun fetchHealthMetrics() {
        if (!_isAuthenticated.value) {
            _errorMessage.value = "Connect Fitbit first."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                source.fetchDay(today())
                    .onSuccess { metrics ->
                        _healthMetrics.value = metrics
                        saveToHistory(metrics)
                        backfillWeek()
                    }
                    .onFailure { error ->
                        _errorMessage.value = error.message ?: "Could not sync Fitbit."
                        if (error.message?.contains("expired", ignoreCase = true) == true) {
                            handleSessionExpired()
                        }
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Pulls the last [HISTORY_DAYS] days and stores each one, so the week view has history. */
    fun syncRecentDays() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val days = source.fetchRecentDays(HISTORY_DAYS)
                days.firstOrNull { it.date == today() }?.let { _healthMetrics.value = it }
                days.forEach { saveToHistory(it) }
                if (days.isEmpty()) {
                    _errorMessage.value = "Fitbit has no data for the last week."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Reason: today's sync is what the user waits for, so the older days are fetched after it
    // rather than before, and a failure on them leaves today's numbers on screen.
    private suspend fun backfillWeek() {
        val known = _weeklyMetrics.value.map { it.date }.toSet()
        if (known.size >= HISTORY_DAYS) return
        source.fetchRecentDays(HISTORY_DAYS)
            .filterNot { it.date in known }
            .forEach { saveToHistory(it) }
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
        source.disconnect()
        _isAuthenticated.value = false
        _errorMessage.value = "Your Fitbit session expired. Connect again."
    }

    fun logout() {
        source.disconnect()
        _isAuthenticated.value = false
        _healthMetrics.value = null
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun today(): String = LocalDate.now(ZoneId.systemDefault()).toString()

    companion object {
        private const val TAG = "HealthViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val HISTORY_DAYS = 7
        private const val SETTINGS_PREFS = "health_settings"
        private const val KEY_USE_MOCK = "use_mock_fitbit"
    }
}
