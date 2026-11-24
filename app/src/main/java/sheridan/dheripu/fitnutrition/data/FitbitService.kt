package sheridan.dheripu.fitnutrition.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import sheridan.dheripu.fitnutrition.BuildConfig
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.ActivityLevel
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The real Fitbit account: OAuth 2.0, token storage and refresh, and the daily activity, heart
 * rate and sleep reads.
 *
 * Nothing here logs a token, a code or the client secret. Fitbit's token response carries live
 * credentials, and logcat is readable by the user and by anything with log access on the device.
 */
class FitbitService(
    context: Context,
    private val api: FitbitApiService = FitbitRetrofitClient.api,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: () -> Long = System::currentTimeMillis
) : FitbitDataSource {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)

    private val clientId = BuildConfig.FITBIT_CLIENT_ID
    private val clientSecret = BuildConfig.FITBIT_CLIENT_SECRET
    private val redirectUri = BuildConfig.FITBIT_REDIRECT_URI

    override val isConfigured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank() && redirectUri.isNotBlank()

    /**
     * True while the stored access token is still good. The skew stops a token that expires in
     * the next minute from being used for a call that would then come back 401.
     */
    fun hasValidAccessToken(): Boolean =
        accessToken() != null && clock() < prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0) - EXPIRY_SKEW

    /**
     * True when tokens are stored, even if the access token has expired; the refresh token can
     * still renew the session.
     */
    fun hasStoredSession(): Boolean =
        accessToken() != null && refreshToken() != null

    override fun isConnected(): Boolean = hasValidAccessToken() || hasStoredSession()

    /**
     * The Fitbit consent URL. A fresh `state` is generated and stored on every call;
     * [completeAuthorization] rejects a redirect that does not carry it back, which is what
     * stops another app from feeding us an authorization code of its own.
     */
    override fun authorizationUrl(): String {
        if (!isConfigured) return ""
        val state = newState()
        prefs.edit().putString(KEY_AUTH_STATE, state).apply()
        return Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("state", state)
            .build()
            .toString()
    }

    override suspend fun completeAuthorization(code: String, state: String?): Result<Unit> {
        if (!isConfigured) return Result.failure(Exception(NOT_CONFIGURED))
        val expected = prefs.getString(KEY_AUTH_STATE, null)
        if (expected == null || state != expected) {
            Log.w(TAG, "Rejecting Fitbit redirect: state did not match")
            return Result.failure(
                Exception("Could not verify the Fitbit sign-in. Start the connection again.")
            )
        }
        prefs.edit().remove(KEY_AUTH_STATE).apply()
        return try {
            val response = api.getAccessToken(
                basicAuth = basicAuthHeader(),
                clientId = clientId,
                code = code,
                grantType = "authorization_code",
                redirectUri = redirectUri
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                saveTokens(body.accessToken, body.refreshToken, body.userId, body.expiresIn)
                Result.success(Unit)
            } else {
                apiError(response, "Could not connect your Fitbit account")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Fitbit token exchange failed", error)
            Result.failure(Exception("Network error while connecting Fitbit.", error))
        }
    }

    /**
     * Renews the access token when it has expired. Fitbit refresh tokens are single use, so the
     * new pair replaces the stored one.
     */
    suspend fun ensureFreshToken(): Result<Unit> {
        if (!isConfigured) return Result.failure(Exception(NOT_CONFIGURED))
        if (hasValidAccessToken()) return Result.success(Unit)
        val refreshToken = refreshToken()
            ?: return Result.failure(Exception(SESSION_EXPIRED))

        return try {
            val response = api.refreshAccessToken(
                basicAuth = basicAuthHeader(),
                grantType = "refresh_token",
                refreshToken = refreshToken
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                saveTokens(body.accessToken, body.refreshToken, body.userId, body.expiresIn)
                Log.d(TAG, "Fitbit access token refreshed")
                Result.success(Unit)
            } else {
                apiError(response, "Could not refresh your Fitbit session")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Fitbit token refresh failed", error)
            Result.failure(Exception("Network error while refreshing the Fitbit session.", error))
        }
    }

    /**
     * One day of metrics. Activity is required; heart rate and sleep are added when Fitbit has
     * them, because a watch worn only in the daytime reports no sleep and that is not an error.
     */
    override suspend fun fetchDay(date: String): Result<HealthMetrics> {
        val fresh = ensureFreshToken()
        fresh.exceptionOrNull()?.let { return Result.failure(it) }
        val token = accessToken() ?: return Result.failure(Exception(SESSION_EXPIRED))
        val authHeader = "Bearer $token"

        val activity = try {
            val response = api.getDailyActivitySummary(date, authHeader)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return apiError(response, "Could not fetch your Fitbit activity")
            }
            body.summary
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Fitbit activity fetch failed", error)
            return Result.failure(Exception("Network error fetching Fitbit activity.", error))
        }

        val steps = activity?.steps ?: 0
        val activeMinutes =
            (activity?.veryActiveMinutes ?: 0) + (activity?.fairlyActiveMinutes ?: 0)
        return Result.success(
            HealthMetrics(
                date = date,
                steps = steps,
                distance = activity?.distances
                    ?.firstOrNull { it.activity.equals(TOTAL_DISTANCE, ignoreCase = true) }
                    ?.distance
                    ?: activity?.distances?.firstOrNull()?.distance
                    ?: 0.0,
                calories = activity?.caloriesOut ?: 0,
                heartRate = restingHeartRate(date, authHeader),
                activeMinutes = activeMinutes,
                sleepMinutes = sleepMinutes(date, authHeader),
                lastSyncTime = syncFormatter.format(LocalDate.now(zone)),
                activityLevel = ActivityLevel.from(steps, activeMinutes),
                timestamp = clock()
            )
        )
    }

    /**
     * The last [days] days. A day Fitbit has nothing for is skipped rather than stored as
     * zeroes, so the alert rules do not read a gap as a day of no sleep.
     */
    override suspend fun fetchRecentDays(days: Int): List<HealthMetrics> {
        val today = LocalDate.now(zone)
        return (0 until days).mapNotNull { offset ->
            fetchDay(dateFormatter.format(today.minusDays(offset.toLong()))).getOrNull()
                ?.takeIf { it.steps > 0 || it.sleepMinutes > 0 || it.heartRate > 0 }
        }
    }

    override fun disconnect() {
        prefs.edit().clear().apply()
    }

    /** Null is not an error here: Fitbit omits resting heart rate until it has enough samples. */
    private suspend fun restingHeartRate(date: String, authHeader: String): Int = try {
        api.getHeartRateData(date, authHeader).body()
            ?.activitiesHeart
            ?.firstOrNull()
            ?.value
            ?.restingHeartRate
            ?: 0
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w(TAG, "Fitbit heart rate unavailable for $date: ${error.message}")
        0
    }

    private suspend fun sleepMinutes(date: String, authHeader: String): Int = try {
        val body = api.getSleepData(date, authHeader).body()
        body?.summary?.totalMinutesAsleep?.toInt()
            ?: body?.sleep?.sumOf { it.minutesAsleep }
            ?: 0
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w(TAG, "Fitbit sleep unavailable for $date: ${error.message}")
        0
    }

    private fun accessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    private fun refreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    private fun basicAuthHeader(): String {
        val encoded = Base64.encodeToString(
            "$clientId:$clientSecret".toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        return "Basic $encoded"
    }

    private fun newState(): String {
        val bytes = ByteArray(STATE_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: String,
        expiresIn: Long
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putLong(KEY_TOKEN_EXPIRES_AT, clock() + expiresIn * 1_000)
            .apply()
    }

    private fun <T> apiError(response: Response<*>, fallback: String): Result<T> {
        Log.e(TAG, "Fitbit request failed with status ${response.code()}")
        val message = when (response.code()) {
            400 -> "Fitbit rejected the request. Check FITBIT_REDIRECT_URI matches your " +
                "Fitbit app settings."
            401 -> SESSION_EXPIRED
            403 -> "Fitbit denied access. Check the scopes on your Fitbit app."
            429 -> "Fitbit rate limit reached. Try again in an hour."
            else -> "$fallback (error ${response.code()})."
        }
        return Result.failure(Exception(message))
    }

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val syncFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy")

    companion object {
        private const val TAG = "FitbitService"
        private const val AUTHORIZE_URL = "https://www.fitbit.com/oauth2/authorize"
        private const val SCOPES = "activity heartrate profile sleep weight"
        private const val TOTAL_DISTANCE = "total"
        private const val STATE_BYTES = 24
        private const val EXPIRY_SKEW = 60_000L
        private const val NOT_CONFIGURED =
            "Fitbit is not configured. Add FITBIT_CLIENT_ID and FITBIT_CLIENT_SECRET to " +
                "local.properties."
        private const val SESSION_EXPIRED = "Your Fitbit session expired. Connect again."

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val KEY_AUTH_STATE = "auth_state"
    }
}
