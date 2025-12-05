package sheridan.dheripu.fitnutrition.data

import android.content.Context
import android.content.SharedPreferences
import sheridan.dheripu.fitnutrition.BuildConfig
import sheridan.dheripu.fitnutrition.model.FitbitAuthResponse
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FitbitService(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)
    private val api = FitbitRetrofitClient.api

    private val clientId = BuildConfig.FITBIT_CLIENT_ID
    private val clientSecret = BuildConfig.FITBIT_CLIENT_SECRET
    private val redirectUri = "fitnutrition://callback"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
    }

    fun isUserAuthenticated(): Boolean = getAccessToken() != null

    fun generateAuthorizationUrl(): String {
        return "https://www.fitbit.com/oauth2/authorize?" +
                "client_id=$clientId&" +
                "response_type=code&" +
                "scope=activity+heartrate+profile+sleep&" +
                "redirect_uri=$redirectUri"
    }

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
                saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.userId)
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Authentication failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDailyActivity(): Result<HealthMetrics> {
        val token = getAccessToken() ?: return Result.failure(Exception("Not authenticated"))
        val today = dateFormat.format(Date())

        return try {
            val response = api.getDailyActivitySummary(today, "Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val metrics = HealthMetrics(
                    date = today,
                    steps = data.summary?.steps ?: 0,
                    distance = data.summary?.distances?.firstOrNull()?.distance ?: 0.0,
                    calories = data.summary?.caloriesOut ?: 0,
                    activeMinutes = data.summary?.veryActiveMinutes ?: 0
                )
                Result.success(metrics)
            } else {
                Result.failure(Exception("Failed to fetch activity: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchHeartRateData(): Result<Int> {
        val token = getAccessToken() ?: return Result.failure(Exception("Not authenticated"))
        val today = dateFormat.format(Date())

        return try {
            val response = api.getHeartRateData(today, "Bearer $token")

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

    fun clearFitbitUser() {
        prefs.edit().clear().apply()
    }

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