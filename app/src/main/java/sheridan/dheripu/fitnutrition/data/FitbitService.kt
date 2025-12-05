package sheridan.dheripu.fitnutrition.data

import android.content.Context
import android.content.SharedPreferences
import sheridan.dheripu.fitnutrition.BuildConfig
import sheridan.dheripu.fitnutrition.model.*
import java.text.SimpleDateFormat
import java.util.*

class FitbitService(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)
    private val api: FitbitApiService = FitbitRetrofitClient.api

    private val clientId = BuildConfig.FITBIT_CLIENT_ID
    private val clientSecret = BuildConfig.FITBIT_CLIENT_SECRET
    private val redirectUri = "fitnutrition://callback"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
    }

    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return getAccessToken() != null
    }

    /**
     * Generate OAuth authorization URL
     */
    fun generateAuthorizationUrl(): String {
        return "https://www.fitbit.com/oauth2/authorize?" +
                "client_id=$clientId&" +
                "response_type=code&" +
                "scope=activity+heartrate+profile+sleep&" +
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
                    userId = authResponse.userId
                )
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Failed to get token: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch daily activity data
     */
    suspend fun fetchDailyActivity(): Result<HealthMetrics> {
        val token = getAccessToken() ?: return Result.failure(Exception("Not authenticated"))
        val today = dateFormat.format(Date())

        return try {
            val response = api.getDailyActivitySummary(
                date = today,
                authHeader = "Bearer $token"
            )

            if (response.isSuccessful && response.body() != null) {
                val activityData = response.body()!!
                val metrics = HealthMetrics(
                    date = today,
                    steps = activityData.summary?.steps ?: 0,
                    distance = activityData.summary?.distances?.firstOrNull()?.distance ?: 0.0,
                    calories = activityData.summary?.caloriesOut ?: 0,
                    activeMinutes = activityData.summary?.veryActiveMinutes ?: 0
                )
                Result.success(metrics)
            } else {
                Result.failure(Exception("Failed to fetch activity: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch heart rate data
     */
    suspend fun fetchHeartRateData(): Result<Int> {
        val token = getAccessToken() ?: return Result.failure(Exception("Not authenticated"))
        val today = dateFormat.format(Date())

        return try {
            val response = api.getHeartRateData(
                date = today,
                authHeader = "Bearer $token"
            )

            if (response.isSuccessful && response.body() != null) {
                val heartRate = response.body()!!.activitiesHeart?.firstOrNull()?.value?.restingHeartRate ?: 0
                Result.success(heartRate)
            } else {
                Result.failure(Exception("Failed to fetch heart rate: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear stored user data
     */
    fun clearFitbitUser() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_ID)
            apply()
        }
    }

    // Private helper methods
    private fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    private fun saveTokens(accessToken: String, refreshToken: String, userId: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            apply()
        }
    }
}
