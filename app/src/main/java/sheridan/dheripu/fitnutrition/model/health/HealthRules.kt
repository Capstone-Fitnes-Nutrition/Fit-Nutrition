package sheridan.dheripu.fitnutrition.model.health

import sheridan.dheripu.fitnutrition.model.HealthMetrics
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Daily activity level derived from Fitbit steps and active minutes. */
object ActivityLevel {
    fun from(steps: Int, activeMinutes: Int): String = when {
        steps >= 10_000 && activeMinutes >= 30 -> "HIGH"
        steps >= 5_000 && activeMinutes >= 15 -> "MODERATE"
        steps >= 2_500 -> "LOW"
        else -> "SEDENTARY"
    }
}

enum class HealthAlertType { HIGH_RESTING_HEART_RATE, LOW_RESTING_HEART_RATE, POOR_SLEEP_STREAK }

data class HealthAlert(val type: HealthAlertType, val title: String, val message: String)

/**
 * BR5: alerts when metrics leave recommended ranges. BR4: alerts after three consecutive
 * nights of poor sleep. A metric of 0 means Fitbit sent no data and never triggers an alert.
 */
object HealthAlertEvaluator {
    private const val HIGH_RESTING_HEART_RATE = 100
    private const val LOW_RESTING_HEART_RATE = 40
    private const val POOR_SLEEP_MINUTES = 360
    private const val POOR_SLEEP_STREAK = 3

    fun evaluate(today: HealthMetrics?, week: List<HealthMetrics>): List<HealthAlert> =
        listOfNotNull(heartRateAlert(today), sleepAlert(week))

    private fun heartRateAlert(today: HealthMetrics?): HealthAlert? {
        val heartRate = today?.heartRate ?: return null
        return when {
            heartRate > HIGH_RESTING_HEART_RATE -> HealthAlert(
                HealthAlertType.HIGH_RESTING_HEART_RATE,
                "High resting heart rate",
                "Your resting heart rate is $heartRate bpm, above the usual 60-100 bpm range. " +
                    "Rest, hydrate, and talk to a doctor if it stays high."
            )
            heartRate in 1 until LOW_RESTING_HEART_RATE -> HealthAlert(
                HealthAlertType.LOW_RESTING_HEART_RATE,
                "Low resting heart rate",
                "Your resting heart rate is $heartRate bpm. That can be normal for athletes, " +
                    "but see a doctor if you feel dizzy or tired."
            )
            else -> null
        }
    }

    private fun sleepAlert(week: List<HealthMetrics>): HealthAlert? {
        val nights = week.mapNotNull { metrics -> parseDate(metrics.date)?.let { it to metrics } }
            .filter { (_, metrics) -> metrics.sleepMinutes > 0 }
            .sortedByDescending { (date, _) -> date }
            .take(POOR_SLEEP_STREAK)
        if (nights.size < POOR_SLEEP_STREAK) return null
        val consecutive = nights.zipWithNext().all { (later, earlier) ->
            earlier.first.plusDays(1) == later.first
        }
        val allPoor = nights.all { (_, metrics) -> metrics.sleepMinutes < POOR_SLEEP_MINUTES }
        if (!consecutive || !allPoor) return null
        return HealthAlert(
            HealthAlertType.POOR_SLEEP_STREAK,
            "Poor sleep three nights in a row",
            "You slept under 6 hours for the last three nights. Try a consistent bedtime and " +
                "lighter training until you recover."
        )
    }

    private fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}
