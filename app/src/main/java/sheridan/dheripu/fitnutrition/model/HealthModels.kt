package sheridan.dheripu.fitnutrition.model

/**
 * One day of health metrics, as the Health tab shows them and as they are stored in the
 * Realtime Database under users/{uid}/health_metrics/{yyyy-MM-dd}.
 *
 * Source-neutral on purpose: Health Connect, the offline mock and the companion demo panel all
 * produce this same shape, so nothing downstream knows or cares where a day came from.
 */
data class HealthMetrics(
    val date: String = "",
    val steps: Int = 0,
    val distance: Double = 0.0,
    val calories: Int = 0,
    val heartRate: Int = 0,
    val activeMinutes: Int = 0,
    val sleepMinutes: Int = 0,
    val lastSyncTime: String = "",
    val activityLevel: String = "UNKNOWN",
    val timestamp: Long = System.currentTimeMillis()
)
