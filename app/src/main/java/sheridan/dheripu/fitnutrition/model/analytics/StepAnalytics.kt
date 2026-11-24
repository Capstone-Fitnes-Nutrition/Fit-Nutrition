package sheridan.dheripu.fitnutrition.model.analytics

import sheridan.dheripu.fitnutrition.model.StepEntry
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Totals shown for manually entered steps over one reporting period. */
data class StepAnalytics(
    val daysLogged: Int = 0,
    val totalSteps: Int = 0,
    val averageSteps: Int = 0,
    val bestDate: String? = null,
    val bestSteps: Int = 0
)

/**
 * Pure aggregation over manually entered step counts. The average is per day that actually has
 * an entry, not per day in the period, so a user who logs twice a week is not averaged down to
 * nearly nothing.
 */
object StepAnalyticsFactory {

    fun from(
        entries: List<StepEntry>,
        days: Int,
        today: LocalDate = LocalDate.now()
    ): StepAnalytics {
        val inPeriod = withinPeriod(entries, days, today)
        if (inPeriod.isEmpty()) return StepAnalytics()

        val total = inPeriod.sumOf { it.steps }
        val best = inPeriod.maxByOrNull { it.steps }

        return StepAnalytics(
            daysLogged = inPeriod.size,
            totalSteps = total,
            averageSteps = total / inPeriod.size,
            bestDate = best?.date,
            bestSteps = best?.steps ?: 0
        )
    }

    /**
     * The entries dated within the last [days] calendar days, newest date first, at most one per
     * date. A record whose date does not parse, or whose count is negative, is dropped: blank or
     * malformed keys are a storage accident, not a zero-step day.
     */
    fun withinPeriod(
        entries: List<StepEntry>,
        days: Int,
        today: LocalDate = LocalDate.now()
    ): List<StepEntry> {
        val firstDay = today.minusDays((days - 1).coerceAtLeast(0).toLong())
        return entries
            .mapNotNull { entry -> parseDate(entry.date)?.let { date -> date to entry } }
            .filter { (date, entry) ->
                !date.isBefore(firstDay) && !date.isAfter(today) && entry.steps >= 0
            }
            .groupBy { (date, _) -> date }
            .mapNotNull { (_, sameDate) -> sameDate.maxByOrNull { (_, entry) -> entry.timestamp } }
            .sortedByDescending { (date, _) -> date }
            .map { (_, entry) -> entry }
    }

    private fun parseDate(raw: String): LocalDate? =
        try {
            LocalDate.parse(raw)
        } catch (e: DateTimeParseException) {
            null
        }
}
