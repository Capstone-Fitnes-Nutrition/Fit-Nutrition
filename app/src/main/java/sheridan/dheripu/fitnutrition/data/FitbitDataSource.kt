package sheridan.dheripu.fitnutrition.data

import sheridan.dheripu.fitnutrition.model.HealthMetrics

/**
 * Everything the Health tab needs from a wearable, so the real Fitbit account and the offline
 * mock are interchangeable. Dates are ISO `yyyy-MM-dd` strings, matching the Realtime Database
 * keys under users/{uid}/health_metrics.
 */
interface FitbitDataSource {

    /** False when no credentials are configured, so the UI can say so instead of failing. */
    val isConfigured: Boolean

    /** True when this source can serve data right now. */
    fun isConnected(): Boolean

    /** Where to send the user to authorize; empty when the source needs no authorization. */
    fun authorizationUrl(): String

    /**
     * Finishes authorization. [state] is the value the redirect came back with and is checked
     * against the one [authorizationUrl] generated.
     */
    suspend fun completeAuthorization(code: String, state: String?): Result<Unit>

    /** One day of metrics, with activity level filled in. */
    suspend fun fetchDay(date: String): Result<HealthMetrics>

    /**
     * The last [days] days, most recent first. Days the source has no data for are left out
     * rather than returned as zeroes.
     */
    suspend fun fetchRecentDays(days: Int): List<HealthMetrics>

    fun disconnect()
}
