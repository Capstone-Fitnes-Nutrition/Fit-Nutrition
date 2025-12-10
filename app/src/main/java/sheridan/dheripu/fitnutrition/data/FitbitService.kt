package sheridan.dheripu.fitnutrition.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
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
        private const val TAG = "FitbitService"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
    }

    init {
        Log.d(TAG, "FitbitService initialized")
        Log.d(TAG, "Client ID: ${clientId.take(5)}...")
        Log.d(TAG, "Redirect URI: $redirectUri")
    }

    /**
     * Check for user valid authentication token
     */
    fun isUserAuthenticated(): Boolean {
        val token = getAccessToken()
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
        val now = System.currentTimeMillis()
        val isValid = token != null && now < expiresAt

        Log.d(TAG, "isUserAuthenticated check:")
        Log.d(TAG, "  Has token: ${token != null}")
        Log.d(TAG, "  Token expires at: ${Date(expiresAt)}")
        Log.d(TAG, "  Current time: ${Date(now)}")
        Log.d(TAG, "  Is valid: $isValid")

        return isValid
    }

    /**
     * Generate Fitbit OAuth auth URL
     */
    fun generateAuthorizationUrl(): String {
        val url = "https://www.fitbit.com/oauth2/authorize?" +
                "client_id=$clientId&" +
                "response_type=code&" +
                "scope=activity+heartrate+profile+sleep+nutrition+weight&" +
                "redirect_uri=$redirectUri"

        Log.d(TAG, "Generated authorization URL")
        Log.d(TAG, "Full URL: $url")

        return url
    }

    /**
     * Generate Basic Authorization header
     * Format: "Basic Base64(client_id:client_secret)"
     */
    private fun getBasicAuthHeader(): String {
        val credentials = "$clientId:$clientSecret"
        val encodedCredentials = Base64.encodeToString(
            credentials.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        val header = "Basic $encodedCredentials"

        Log.d(TAG, "Generated Basic Auth header")
        Log.d(TAG, "Credentials format: client_id:client_secret")
        Log.d(TAG, "Header: Basic ${encodedCredentials.take(20)}...")

        return header
    }

    /**
     * Exchange authorization code for access token
     */
    suspend fun exchangeCodeForToken(code: String): Result<FitbitAuthResponse> {
        Log.d(TAG, "========== TOKEN EXCHANGE START ==========")
        Log.d(TAG, "Code: ${code.take(10)}...")
        Log.d(TAG, "Client ID: ${clientId.take(5)}...")
        Log.d(TAG, "Redirect URI: $redirectUri")

        return try {
            val basicAuthHeader = getBasicAuthHeader()

            Log.d(TAG, "Making API call to exchange code for token...")
            Log.d(TAG, "Using Basic Authorization header")

            val response = api.getAccessToken(
                basicAuth = basicAuthHeader,
                clientId = clientId,
                code = code,
                grantType = "authorization_code",
                redirectUri = redirectUri
            )

            Log.d(TAG, "API Response received:")
            Log.d(TAG, "  Response code: ${response.code()}")
            Log.d(TAG, "  Response message: ${response.message()}")
            Log.d(TAG, "  Is successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                Log.d(TAG, "✅ TOKEN EXCHANGE SUCCESS!")
                Log.d(TAG, "  Access Token: ${authResponse.accessToken.take(10)}...")
                Log.d(TAG, "  Refresh Token: ${authResponse.refreshToken.take(10)}...")
                Log.d(TAG, "  User ID: ${authResponse.userId}")
                Log.d(TAG, "  Expires in: ${authResponse.expiresIn} seconds")
                Log.d(TAG, "  Token type: ${authResponse.tokenType}")
                Log.d(TAG, "  Scope: ${authResponse.scope}")

                saveTokens(
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    userId = authResponse.userId,
                    expiresIn = authResponse.expiresIn
                )

                Log.d(TAG, "Tokens saved to SharedPreferences")
                Log.d(TAG, "========== TOKEN EXCHANGE END ==========")

                Result.success(authResponse)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ TOKEN EXCHANGE FAILED!")
                Log.e(TAG, "  Response code: ${response.code()}")
                Log.e(TAG, "  Response message: ${response.message()}")
                Log.e(TAG, "  Error body: $errorBody")
                Log.d(TAG, "========== TOKEN EXCHANGE END ==========")

                Result.failure(
                    Exception("Authentication failed: ${response.code()} - ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ TOKEN EXCHANGE EXCEPTION!", e)
            Log.e(TAG, "Exception: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Log.d(TAG, "========== TOKEN EXCHANGE END ==========")

            Result.failure(Exception("Network error during authentication: ${e.message}", e))
        }
    }

    /**
     * Fetch daily activity summary (steps, distance, calories, active minutes)
     */
    suspend fun fetchDailyActivity(): Result<HealthMetrics> {
        Log.d(TAG, "========== FETCH DAILY ACTIVITY START ==========")

        val token = getAccessToken()
        if (token == null) {
            Log.e(TAG, "❌ No access token found!")
            Log.d(TAG, "========== FETCH DAILY ACTIVITY END ==========")
            return Result.failure(Exception("Not authenticated"))
        }

        val today = dateFormat.format(Date())
        Log.d(TAG, "Fetching activity for date: $today")
        Log.d(TAG, "Using token: ${token.take(10)}...")

        return try {
            Log.d(TAG, "Making API call to Fitbit...")
            val response = api.getDailyActivitySummary(today, "Bearer $token")

            Log.d(TAG, "API Response received:")
            Log.d(TAG, "  Response code: ${response.code()}")
            Log.d(TAG, "  Response message: ${response.message()}")
            Log.d(TAG, "  Is successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val summary = data.summary

                Log.d(TAG, "✅ ACTIVITY DATA RECEIVED!")
                Log.d(TAG, "  Steps: ${summary?.steps}")
                Log.d(TAG, "  Distances count: ${summary?.distances?.size}")
                Log.d(TAG, "  First distance: ${summary?.distances?.firstOrNull()?.distance}")
                Log.d(TAG, "  Calories: ${summary?.caloriesOut}")
                Log.d(TAG, "  Very Active Minutes: ${summary?.veryActiveMinutes}")
                Log.d(TAG, "  Fairly Active Minutes: ${summary?.fairlyActiveMinutes}")

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

                Log.d(TAG, "Metrics object created: $metrics")
                Log.d(TAG, "========== FETCH DAILY ACTIVITY END ==========")
                Result.success(metrics)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ ACTIVITY FETCH FAILED!")
                Log.e(TAG, "  Response code: ${response.code()}")
                Log.e(TAG, "  Error body: $errorBody")
                Log.d(TAG, "========== FETCH DAILY ACTIVITY END ==========")

                handleApiError(response.code(), "Failed to fetch activity data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ACTIVITY FETCH EXCEPTION!", e)
            Log.e(TAG, "Message: ${e.message}")
            Log.d(TAG, "========== FETCH DAILY ACTIVITY END ==========")

            Result.failure(Exception("Network error fetching activity: ${e.message}", e))
        }
    }

    /**
     * Fetch heart rate data
     */
    suspend fun fetchHeartRateData(): Result<Int> {
        Log.d(TAG, "========== FETCH HEART RATE START ==========")

        val token = getAccessToken()
        if (token == null) {
            Log.e(TAG, "❌ No access token found!")
            Log.d(TAG, "========== FETCH HEART RATE END ==========")
            return Result.failure(Exception("Not authenticated"))
        }

        val today = dateFormat.format(Date())
        Log.d(TAG, "Fetching heart rate for date: $today")

        return try {
            Log.d(TAG, "Making API call to Fitbit...")
            val response = api.getHeartRateData(today, "Bearer $token")

            Log.d(TAG, "API Response received:")
            Log.d(TAG, "  Response code: ${response.code()}")
            Log.d(TAG, "  Is successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val heartRateData = response.body()!!
                val restingHeartRate = heartRateData.activitiesHeart
                    ?.firstOrNull()
                    ?.value
                    ?.restingHeartRate ?: 0

                Log.d(TAG, "✅ HEART RATE RECEIVED: $restingHeartRate bpm")
                Log.d(TAG, "========== FETCH HEART RATE END ==========")

                Result.success(restingHeartRate)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ HEART RATE FETCH FAILED!")
                Log.e(TAG, "  Error body: $errorBody")
                Log.d(TAG, "========== FETCH HEART RATE END ==========")

                handleApiError(response.code(), "Failed to fetch heart rate")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ HEART RATE FETCH EXCEPTION!", e)
            Log.e(TAG, "Message: ${e.message}")
            Log.d(TAG, "========== FETCH HEART RATE END ==========")

            Result.failure(Exception("Network error fetching heart rate: ${e.message}", e))
        }
    }

    /**
     * Fetch sleep data (optional - for future use)
     */
    suspend fun fetchSleepData(): Result<Int> {
        Log.d(TAG, "========== FETCH SLEEP DATA START ==========")

        val token = getAccessToken()
        if (token == null) {
            Log.e(TAG, "❌ No access token found!")
            Log.d(TAG, "========== FETCH SLEEP DATA END ==========")
            return Result.failure(Exception("Not authenticated"))
        }

        val today = dateFormat.format(Date())
        Log.d(TAG, "Fetching sleep for date: $today")

        return try {
            val response = api.getSleepData(today, "Bearer $token")

            Log.d(TAG, "API Response received:")
            Log.d(TAG, "  Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val sleepData = response.body()!!
                val totalMinutesAsleep = sleepData.summary?.totalMinutesAsleep?.toInt() ?: 0

                Log.d(TAG, "✅ SLEEP DATA RECEIVED: $totalMinutesAsleep minutes")
                Log.d(TAG, "========== FETCH SLEEP DATA END ==========")

                Result.success(totalMinutesAsleep)
            } else {
                Log.e(TAG, "❌ SLEEP FETCH FAILED!")
                Log.d(TAG, "========== FETCH SLEEP DATA END ==========")
                handleApiError(response.code(), "Failed to fetch sleep data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SLEEP FETCH EXCEPTION!", e)
            Log.d(TAG, "========== FETCH SLEEP DATA END ==========")
            Result.failure(Exception("Network error fetching sleep: ${e.message}", e))
        }
    }

    /**
     * Clear all stored Fitbit user data (logout)
     */
    fun clearFitbitUser() {
        Log.d(TAG, "Clearing all Fitbit user data from SharedPreferences")
        prefs.edit().clear().apply()
        Log.d(TAG, "User data cleared")
    }

    /**
     * Get stored access token
     */
    private fun getAccessToken(): String? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        Log.d(TAG, "getAccessToken: ${if (token != null) "Token exists (${token.take(10)}...)" else "No token"}")
        return token
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

        Log.d(TAG, "Saving tokens to SharedPreferences:")
        Log.d(TAG, "  Access Token: ${accessToken.take(10)}...")
        Log.d(TAG, "  Refresh Token: ${refreshToken.take(10)}...")
        Log.d(TAG, "  User ID: $userId")
        Log.d(TAG, "  Expires at: ${Date(expiresAt)}")

        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_ID, userId)
            putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            apply()
        }

        Log.d(TAG, "Tokens saved successfully")

        // Verify save
        val savedToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        Log.d(TAG, "Verification - Token retrieved: ${savedToken?.take(10)}...")
    }

    /**
     * Handle API error responses
     */
    private fun <T> handleApiError(code: Int, message: String): Result<T> {
        Log.e(TAG, "API Error - Code: $code, Message: $message")

        return when (code) {
            401 -> {
                Log.e(TAG, "401 Unauthorized - Token may be expired or invalid")
                Result.failure(Exception("Unauthorized: Please login again"))
            }
            403 -> {
                Log.e(TAG, "403 Forbidden - Check API permissions/scopes")
                Result.failure(Exception("Access forbidden: Check API permissions"))
            }
            429 -> {
                Log.e(TAG, "429 Rate Limit - Too many requests")
                Result.failure(Exception("Rate limit exceeded: Try again later"))
            }
            else -> {
                Log.e(TAG, "HTTP Error $code")
                Result.failure(Exception("$message (Error $code)"))
            }
        }
    }
}
