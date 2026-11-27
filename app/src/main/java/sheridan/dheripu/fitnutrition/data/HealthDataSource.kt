package sheridan.dheripu.fitnutrition.data

import sheridan.dheripu.fitnutrition.model.HealthMetrics

/**
 * Everything the Health tab needs from a daily-metrics source, so Health Connect and the
 * offline mock are interchangeable. Dates are ISO `yyyy-MM-dd` strings, matching the Realtime
 * Database keys under users/{uid}/health_metrics.
 *
 * This replaced an OAuth-shaped interface built for the Fitbit Web API, which Google
 * decommissioned on 30 October 2026. Health Connect is an on-device store, so there is no
 * redirect to follow and no token to refresh: a source is reachable or it is not, and the only
 * gate is an Android runtime permission.
 */
interface HealthDataSource {

    /** False when this source cannot work on this device at all, so the UI can say why. */
    val isConfigured: Boolean

    /**
     * The Android permissions the UI must have granted before [fetchDay] can return anything.
     * Empty when the source needs none, as the mock does.
     */
    val requiredPermissions: Set<String>

    /** True when this source may read data right now. */
    suspend fun isConnected(): Boolean

    /** One day of metrics, with activity level filled in. */
    suspend fun fetchDay(date: String): Result<HealthMetrics>

    /**
     * The last [days] days, most recent first. Days the source has no data for are left out
     * rather than returned as zeroes.
     */
    suspend fun fetchRecentDays(days: Int): List<HealthMetrics>

    /** Forgets anything cached locally. Permissions themselves are revoked in Settings. */
    fun disconnect()
}
