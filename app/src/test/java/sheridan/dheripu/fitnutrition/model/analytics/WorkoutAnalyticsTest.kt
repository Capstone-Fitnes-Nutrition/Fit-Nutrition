package sheridan.dheripu.fitnutrition.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import java.time.ZoneId

class WorkoutAnalyticsTest {

    @Test
    fun totalsCountOneSessionPerDay() {
        val analytics = WorkoutAnalyticsFactory.from(
            items = listOf(
                workout("Squat", sets = 3, reps = 10, minutes = 20, daysAgo = 0),
                workout("Bench", sets = 4, reps = 8, minutes = 25, daysAgo = 0),
                workout("Row", sets = 5, reps = 5, minutes = 15, daysAgo = 2)
            ),
            days = 7,
            now = NOW,
            zone = ZONE
        )

        assertEquals(2, analytics.sessions)
        assertEquals(3, analytics.exercisesLogged)
        assertEquals(60, analytics.totalMinutes)
        assertEquals(30, analytics.averageSessionMinutes)
        assertEquals(12, analytics.totalSets)
        assertEquals(30 + 32 + 25, analytics.totalReps)
    }

    @Test
    fun itemsOutsideThePeriodAreIgnored() {
        val items = listOf(
            workout("Squat", minutes = 20, daysAgo = 1),
            workout("Deadlift", minutes = 30, daysAgo = 9)
        )

        val weekly = WorkoutAnalyticsFactory.from(items, days = 7, now = NOW, zone = ZONE)
        val monthly = WorkoutAnalyticsFactory.from(items, days = 30, now = NOW, zone = ZONE)

        assertEquals(20, weekly.totalMinutes)
        assertEquals(50, monthly.totalMinutes)
    }

    @Test
    fun theSeventhDayBackIsStillInTheWeeklyPeriod() {
        val items = listOf(workout("Squat", minutes = 10, daysAgo = 6))

        val weekly = WorkoutAnalyticsFactory.withinPeriod(items, days = 7, now = NOW, zone = ZONE)

        assertEquals(1, weekly.size)
    }

    @Test
    fun emptyHistoryGivesZeroesRatherThanDividingByZero() {
        val analytics = WorkoutAnalyticsFactory.from(emptyList(), days = 7, now = NOW, zone = ZONE)

        assertEquals(WorkoutAnalytics(), analytics)
    }

    @Test
    fun recentItemsComeFirst() {
        val items = listOf(
            workout("Older", daysAgo = 3),
            workout("Newest", daysAgo = 0)
        )

        val ordered = WorkoutAnalyticsFactory.withinPeriod(items, days = 7, now = NOW, zone = ZONE)

        assertEquals("Newest", ordered.first().exercise.name)
        assertTrue(ordered.first().completedAt > ordered.last().completedAt)
    }

    @Test
    fun negativeOrMissingValuesDoNotSubtractFromTotals() {
        val analytics = WorkoutAnalyticsFactory.from(
            items = listOf(
                workout("Plank", sets = 0, reps = 0, minutes = 5, daysAgo = 0),
                workout("Broken", sets = -2, reps = -3, minutes = -10, daysAgo = 0)
            ),
            days = 7,
            now = NOW,
            zone = ZONE
        )

        assertEquals(5, analytics.totalMinutes)
        assertEquals(0, analytics.totalSets)
        assertEquals(0, analytics.totalReps)
    }

    private fun workout(
        name: String,
        sets: Int = 3,
        reps: Int = 10,
        minutes: Int = 20,
        daysAgo: Int = 0
    ) = WorkoutItem(
        exercise = Exercise(id = name, name = name),
        sets = sets,
        reps = reps,
        time = minutes,
        completedAt = NOW - daysAgo * DAY
    )

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
        val ZONE: ZoneId = ZoneId.of("America/Toronto")

        /** 2026-10-25 12:00 in [ZONE], so day boundaries are unambiguous. */
        const val NOW = 1_792_944_000_000L
    }
}
