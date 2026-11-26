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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import sheridan.dheripu.fitnutrition.BuildConfig
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.HealthAlert
import sheridan.dheripu.fitnutrition.model.health.HealthAlertEvaluator
import sheridan.dheripu.fitnutrition.repository.HealthRepository
import sheridan.dheripu.fitnutrition.repository.MockHealthOverrideRepository
import java.time.LocalDate
import java.time.ZoneId

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = application.applicationContext
        .getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)

    private val realSource = HealthConnectDataSource(application.applicationContext)
    private val mockSource = MockHealthDataSource(overrides = MockHealthOverrideRepository())
    private val healthRepository = HealthRepository()

    private val _useMockData = MutableStateFlow(
        settings.getBoolean(KEY_USE_MOCK, BuildConfig.USE_MOCK_HEALTH)
    )

    /** True while the Health tab is reading the offline mock instead of Health Connect. */
    val useMockData: StateFlow<Boolean> = _useMockData.asStateFlow()

    private val source: HealthDataSource
        get() = if (_useMockData.value) mockSource else realSource

    /** What the screen must ask Health Connect for before a real sync can return anything. */
    val requiredPermissions: Set<String> get() = source.requiredPermissions

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

    /** False when the chosen source cannot work on this device, so the UI can say why. */
    val isHealthSourceConfigured: Boolean get() = source.isConfigured

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
     * Whether the chosen source may read data right now. For Health Connect that is a
     * permission check, so it runs off the main thread and syncs once it comes back granted.
     */
    fun checkAuthentication() {
        viewModelScope.launch {
            val connected = source.isConnected()
            _isAuthenticated.value = connected
            if (connected) fetchHealthMetrics()
        }
    }

    /**
     * Called when the user comes back from the Health Connect permission sheet. Granted or
     * not, the answer is whatever [checkAuthentication] now finds.
     */
    fun onPermissionResult(granted: Set<String>) {
        if (granted.isEmpty()) {
            _errorMessage.value = "Health Connect access was not granted."
            _isAuthenticated.value = false
            return
        }
        _errorMessage.value = null
        checkAuthentication()
    }

    /** Syncs today, then saves and backfills the rest of the week without blocking the screen. */
    fun fetchHealthMetrics() {
        if (!_isAuthenticated.value) {
            _errorMessage.value = "Connect Health Connect first."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = source.fetchDay(today())
            // Reason: the spinner ends when the numbers are in hand. It used to wait for the
            // Realtime Database write too, so a database that never answered left the tab
            // spinning over metrics it had already loaded, and the offline mock could not be
            // demonstrated at all.
            _isLoading.value = false
            result
                .onSuccess { metrics ->
                    _healthMetrics.value = metrics
                    persistInBackground(metrics)
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Could not sync your health data."
                }
        }
    }

    /** Storing history is a background concern; the user never waits on it. */
    private fun persistInBackground(today: HealthMetrics) {
        viewModelScope.launch {
            saveToHistory(today)
            val days = source.fetchRecentDays(HISTORY_DAYS)
            showLocally(days)
            days.forEach { saveToHistory(it) }
        }
    }

    /** Pulls the last [HISTORY_DAYS] days and stores each one, so the week view has history. */
    fun syncRecentDays() {
        viewModelScope.launch {
            _isLoading.value = true
            val days = try {
                source.fetchRecentDays(HISTORY_DAYS)
            } finally {
                _isLoading.value = false
            }
            if (days.isEmpty()) {
                _errorMessage.value = "No health data for the last week."
                return@launch
            }
            days.firstOrNull { it.date == today() }?.let { _healthMetrics.value = it }
            showLocally(days)
            days.forEach { saveToHistory(it) }
        }
    }

    /**
     * Shows the fetched week straight away instead of waiting for it to round-trip through the
     * Realtime Database. The database listener still wins once it reports, and it is the only
     * source of days this session did not fetch.
     */
    private fun showLocally(days: List<HealthMetrics>) {
        if (days.isEmpty()) return
        val byDate = (days + _weeklyMetrics.value).associateBy { it.date }
        _weeklyMetrics.value = byDate.values.sortedByDescending { it.date }.take(HISTORY_DAYS)
    }

    private suspend fun saveToHistory(metrics: HealthMetrics) {
        // Reason: saveHealthMetrics only emits when Firebase calls back. Unreachable rules or
        // no network meant it never did, and the caller suspended for good.
        val saved = withTimeoutOrNull(SAVE_TIMEOUT_MILLIS) {
            healthRepository.saveHealthMetrics(metrics).first()
        }
        when {
            saved == null -> Log.w(TAG, "Timed out saving health history for ${metrics.date}")
            saved.isFailure -> Log.w(TAG, "Could not save health history: ${saved.exceptionOrNull()?.message}")
        }
    }

    private fun observeWeeklyHistory() {
        viewModelScope.launch {
            healthRepository.getWeeklyHealthMetrics().collect { result ->
                result.onSuccess { _weeklyMetrics.value = it }
                    .onFailure { Log.w(TAG, "Weekly history unavailable: ${it.message}") }
            }
        }
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
        private const val SAVE_TIMEOUT_MILLIS = 5_000L
        private const val SETTINGS_PREFS = "health_settings"
        private const val KEY_USE_MOCK = "use_mock_health"
    }
}
