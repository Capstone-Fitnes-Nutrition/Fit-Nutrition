package sheridan.dheripu.fitnutrition.model

/**
 * One calendar day's manually entered step count.
 *
 * Stored in the Realtime Database at users/{uid}/manual_steps/{yyyy-MM-dd}, so entering steps
 * again on the same day replaces the old number rather than adding a second record.
 *
 * Reason: kept in its own subtree instead of the Fitbit [HealthMetrics] record for the same day.
 * The two sources would otherwise overwrite each other whenever a user both wore the tracker and
 * typed a number.
 */
data class StepEntry(
    val date: String = "",
    val steps: Int = 0,
    val timestamp: Long = 0L
)
