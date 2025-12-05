package sheridan.dheripu.fitnutrition.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthViewModel(
    private val fitbitService: FitbitService,
    private val repository: HealthRepository
) : ViewModel() {

    // State flows
    private val _healthMetrics = MutableStateFlow<HealthMetrics?>(null)
    val healthMetrics: StateFlow<HealthMetrics?> = _healthMetrics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _weeklyMetrics = MutableStateFlow<List<HealthMetrics>>(emptyList())
    val weeklyMetrics: StateFlow<List<HealthMetrics>> = _weeklyMetrics.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        checkAuthentication()
        observeHealthMetrics()
    }

    private fun checkAuthentication() {
        _isAuthenticated.value = fitbitService.isUserAuthenticated()
    }

    private fun observeHealthMetrics() {
        viewModelScope.launch {
            repository.observeHealthMetrics().collect { result ->
                result.onSuccess { metrics ->
                    _healthMetrics.value = metrics
                }.onFailure { e ->
                    // Handle error silently or log as needed
                }
            }
        }
    }

    fun getAuthorizationUrl(): String {
        return fitbitService.generateAuthorizationUrl()
    }

    fun handleAuthorizationCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = fitbitService.exchangeCodeForToken(code)
                result.onSuccess {
                    _isAuthenticated.value = true
                    fetchTodayMetrics()
                }.onFailure { e ->
                    _error.value = "Authentication failed: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchTodayMetrics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val activityResult = fitbitService.fetchDailyActivity()
                activityResult.onSuccess { activity ->
                    val heartRateResult = fitbitService.fetchHeartRateData()
                    heartRateResult.onSuccess { heartRate ->
                        val metrics = activity.copy(heartRate = heartRate)
                        _healthMetrics.value = metrics
                        // ... rest of save logic
                    }.onFailure { e ->
                        _error.value = "Failed to fetch heart rate: ${e.message}"
                    }
                }.onFailure { e ->
                    _error.value = "Failed to fetch activity: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error fetching metrics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        fitbitService.clearFitbitUser()
        _isAuthenticated.value = false
        _healthMetrics.value = null
        _weeklyMetrics.value = emptyList()
    }
}

// Update factory
class HealthViewModelFactory(
    private val fitbitService: FitbitService,
    private val repository: HealthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HealthViewModel(fitbitService, repository) as T
    }
}
