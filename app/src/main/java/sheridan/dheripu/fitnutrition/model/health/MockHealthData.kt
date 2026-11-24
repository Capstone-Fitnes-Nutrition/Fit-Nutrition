package sheridan.dheripu.fitnutrition.model.health

import sheridan.dheripu.fitnutrition.model.HealthMetrics
import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random

/**
 * Realistic fake daily metrics, so the Health tab, the alert rules and the health-driven parts
 * of the recommendation engine can be exercised without a Fitbit account or a watch.
 *
 * Deterministic: the same date always produces the same day, so a test can assert on it and a
 * demo looks the same twice. Weekends are more active than weekdays and one day in seven sleeps
 * badly, which is what makes the BR4 sleep streak and the BR5 heart-rate rules reachable.
 */
object MockHealthData {

    /** [days] days ending on [endDate], most recent first. */
    fun recentDays(endDate: LocalDate, days: Int): List<HealthMetrics> =
        (0 until days).map { day(endDate.minusDays(it.toLong())) }

    fun day(date: LocalDate): HealthMetrics {
        // Reason: seeding from the epoch day makes every field for a given date reproducible
        // without storing anything, so a mock day survives a reinstall and a test can hard-code it.
        val random = Random(date.toEpochDay())
        val weekend = date.dayOfWeek.value >= SATURDAY
        val baseSteps = if (weekend) WEEKEND_BASE_STEPS else WEEKDAY_BASE_STEPS
        val steps = baseSteps + random.nextInt(-STEP_SPREAD, STEP_SPREAD)
        val activeMinutes = (steps / STEPS_PER_ACTIVE_MINUTE) + random.nextInt(0, 12)
        val poorNight = abs(date.toEpochDay()) % POOR_SLEEP_EVERY == 0L
        val sleepMinutes = if (poorNight) {
            POOR_SLEEP_BASE_MINUTES + random.nextInt(0, 40)
        } else {
            GOOD_SLEEP_BASE_MINUTES + random.nextInt(0, 70)
        }
        return HealthMetrics(
            date = date.toString(),
            steps = steps,
            distance = (steps * METRES_PER_STEP / METRES_PER_KM).roundTo(2),
            calories = RESTING_CALORIES + (steps / STEPS_PER_CALORIE),
            heartRate = RESTING_HEART_RATE_BASE + random.nextInt(0, 12) +
                if (poorNight) POOR_SLEEP_HEART_RATE_PENALTY else 0,
            activeMinutes = activeMinutes,
            sleepMinutes = sleepMinutes,
            lastSyncTime = "Mock data for $date",
            activityLevel = ActivityLevel.from(steps, activeMinutes),
            timestamp = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
    }

    private fun Double.roundTo(decimals: Int): Double {
        var factor = 1.0
        repeat(decimals) { factor *= 10 }
        return Math.round(this * factor) / factor
    }

    private const val SATURDAY = 6
    private const val WEEKDAY_BASE_STEPS = 7_200
    private const val WEEKEND_BASE_STEPS = 11_500
    private const val STEP_SPREAD = 2_600
    private const val STEPS_PER_ACTIVE_MINUTE = 420
    private const val STEPS_PER_CALORIE = 22
    private const val RESTING_CALORIES = 1_650
    private const val METRES_PER_STEP = 0.78
    private const val METRES_PER_KM = 1_000
    private const val RESTING_HEART_RATE_BASE = 58
    private const val POOR_SLEEP_HEART_RATE_PENALTY = 6
    private const val POOR_SLEEP_EVERY = 7L
    private const val POOR_SLEEP_BASE_MINUTES = 280
    private const val GOOD_SLEEP_BASE_MINUTES = 400
}
