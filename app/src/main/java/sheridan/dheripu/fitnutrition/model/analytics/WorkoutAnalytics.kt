package sheridan.dheripu.fitnutrition.model.analytics

import sheridan.dheripu.fitnutrition.model.WorkoutItem
import java.time.Instant
import java.time.ZoneId

/** Totals shown on the workout analytics screen for one reporting period. */
data class WorkoutAnalytics(
    val sessions: Int = 0,
    val exercisesLogged: Int = 0,
    val totalMinutes: Int = 0,
    val averageSessionMinutes: Int = 0,
    val totalSets: Int = 0,
    val totalReps: Int = 0
)

/**
 * Pure aggregation over logged workouts. A session is one calendar day that has at least one
 * logged exercise, which is how the screen counts "workouts".
 */
object WorkoutAnalyticsFactory {

    fun from(
        items: List<WorkoutItem>,
        days: Int,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): WorkoutAnalytics {
        val inPeriod = withinPeriod(items, days, now, zone)
        if (inPeriod.isEmpty()) return WorkoutAnalytics()

        val sessions = inPeriod.map { dayOf(it.completedAt, zone) }.distinct().size
        val totalMinutes = inPeriod.sumOf { it.time.coerceAtLeast(0) }

        return WorkoutAnalytics(
            sessions = sessions,
            exercisesLogged = inPeriod.size,
            totalMinutes = totalMinutes,
            averageSessionMinutes = totalMinutes / sessions,
            totalSets = inPeriod.sumOf { it.sets.coerceAtLeast(0) },
            totalReps = inPeriod.sumOf { it.sets.coerceAtLeast(0) * it.reps.coerceAtLeast(0) }
        )
    }

    /** The items logged in the last [days] calendar days, newest first. */
    fun withinPeriod(
        items: List<WorkoutItem>,
        days: Int,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): List<WorkoutItem> {
        val firstDay = dayOf(now, zone) - (days - 1).coerceAtLeast(0)
        return items
            .filter { dayOf(it.completedAt, zone) >= firstDay && it.completedAt <= now }
            .sortedByDescending { it.completedAt }
    }

    private fun dayOf(epochMillis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toEpochDay()
}
