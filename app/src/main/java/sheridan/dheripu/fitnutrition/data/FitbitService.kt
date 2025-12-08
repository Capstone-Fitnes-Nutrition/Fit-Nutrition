package sheridan.dheripu.fitnutrition.data

import android.content.Context
import android.content.SharedPreferences
import sheridan.dheripu.fitnutrition.BuildConfig
import sheridan.dheripu.fitnutrition.model.FitbitAuthResponse
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service class to handle all Fitbit API interactions
 * Manages authentication, token storage, and data fetching
 */
class FitbitService(private val context: Context) {

    private val prefs: SharedPreferences = 
        context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)

    private val api = FitbitRetrofitClient.api
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Fetch credentials from BuildConfig (local.properties)
    private val clientId = BuildConfig.FITBIT_CLIENT_ID
    private val clientSecret = BuildConfig.FITBIT_CLIENT_SECRET
    private val redirectUri = BuildConfig.FITBIT_REDIRECT_URI

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
    }

    /**
     * Check if user has valid authentication token
     */
    fun isUserAuthenticated(): Boolean {
        val token = getAccessToken()
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
        return token != null && System.currentTimeMillis() < expiresAt
    }

    /**
     * Generate Fitbit OAuth authorization URL
     */
    fun generateAuthorizationUrl(): String {
        return "https://www.fitbit.com/oauth2/authorize?" +
                "client_id=$clientId&" +
                "response_type=code&" +
                "scope=activity+heartrate+profile+sleep+nutrition+weight&" +
                "redirect_uri=$redirectUri"
    }

    /**
     * Exchange authorization code for access token
     */
    suspend fun exchangeCodeForToken(code: String): Result<FitbitAuthResponse> {
        return try {
            val response = api.getAccessToken(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                grantType = "authorization_code",
                redirectUri = redirectUri
            )

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                saveTokens(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    userId = authResponse.userId,
                    expiresIn = authResponse.expiresIn
                )
                Result.success(authResponse)
            } else {
                Result.failure(
                    Exception("Authentication failed: ${response.code()} - ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error during authentication: ${e.message}", e))
        }
    }

    /**
     * Fetch daily activity summary (steps, distance, calories, active minutes)
     */
    suspend fun fetchDailyActivity(): Result<HealthMetrics> {
        val token = getAccessToken() 
            ?: return Result.failure(Exception("Not authenticated"))

        val today = dateFormat.format(Date())

        return try {
            val response = api.getDailyActivitySummary(today, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val summary = data.summary

                val metrics = HealthMetrics(
                    date = today,
                    steps = summary?.steps ?: 0,
                    distance = summary?.distances?.firstOrNull()?.distance ?: 0.0,
                    calories = summary?.caloriesOut ?: 0,
                    activeMinutes = (summary?.veryActiveMinutes ?: 0) + 
                                   (summary?.fairlyActiveMinutes ?: 0),
                    heartRate = 0, // Will be fetched separately
                    lastSyncTime = "",
                    activityLevel = "UNKNOWN"
                )

                Result.success(metrics)
            } else {
                handleApiError(response.code(), "Failed to fetch activity data")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error fetching activity: ${e.message}", e))
        }
    }

    /**
     * Fetch heart rate data
     */
    suspend fun fetchHeartRateData(): Result<Int> {
        val token = getAccessToken() 
            ?: return Result.failure(Exception("Not authenticated"))

        val today = dateFormat.format(Date())

        return try {
            val response = api.getHeartRateData(today, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val heartRateData = response.body()!!
                val restingHeartRate = heartRateData.activitiesHeart
                    ?.firstOrNull()
                    ?.value
                    ?.restingHeartRate ?: 0

                Result.success(restingHeartRate)
            } else {
                handleApiError(response.code(), "Failed to fetch heart rate")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error fetching heart rate: ${e.message}", e))
        }
    }

    /**
     * Fetch sleep data (optional - for future use)
     */
    suspend fun fetchSleepData(): Result<Int> {
        val token = getAccessToken() 
            ?: return Result.failure(Exception("Not authenticated"))

        val today = dateFormat.format(Date())

        return try {
            val response = api.getSleepData(today, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val sleepData = response.body()!!
                val totalMinutesAsleep = sleepData.summary?.totalMinutesAsleep?.toInt() ?: 0

                Result.success(totalMinutesAsleep)
            } else {
                handleApiError(response.code(), "Failed to fetch sleep data")
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error fetching sleep: ${e.message}", e))
        }
    }

    /**
     * Clear all stored Fitbit user data (logout)
     */
    fun clearFitbitUser() {
        prefs.edit().clear().apply()
    }

    /**
     * Get stored access token
     */
    private fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Save authentication tokens to SharedPreferences
     */
    private fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: String,
        expiresIn: Long
    ) {
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)

        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            apply()
        }
    }

    /**
     * Handle API error responses
     */
    private fun <T> handleApiError(code: Int, message: String): Result<T> {
        return when (code) {
            401 -> Result.failure(Exception("Unauthorized: Please login again"))
            403 -> Result.failure(Exception("Access forbidden: Check API permissions"))
            429 -> Result.failure(Exception("Rate limit exceeded: Try again later"))
            else -> Result.failure(Exception("$message (Error $code)"))
        }
    }
}
