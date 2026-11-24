package sheridan.dheripu.fitnutrition.model.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MockHealthDataTest {

    private val anyDay = LocalDate.of(2026, 3, 11)

    @Test
    fun `the same date always produces the same day`() {
        assertEquals(MockHealthData.day(anyDay), MockHealthData.day(anyDay))
    }

    @Test
    fun `different dates produce different days`() {
        assertTrue(MockHealthData.day(anyDay).steps != MockHealthData.day(anyDay.plusDays(1)).steps)
    }

    @Test
    fun `recentDays returns the requested count ending on the given date, newest first`() {
        val days = MockHealthData.recentDays(anyDay, 7)

        assertEquals(7, days.size)
        assertEquals(anyDay.toString(), days.first().date)
        assertEquals(anyDay.minusDays(6).toString(), days.last().date)
    }

    @Test
    fun `every generated day carries plausible metrics`() {
        MockHealthData.recentDays(anyDay, 30).forEach { metrics ->
            assertTrue("steps ${metrics.steps}", metrics.steps in 3_000..15_000)
            assertTrue("distance ${metrics.distance}", metrics.distance in 2.0..13.0)
            assertTrue("calories ${metrics.calories}", metrics.calories in 1_700..2_500)
            assertTrue("heart rate ${metrics.heartRate}", metrics.heartRate in 50..85)
            assertTrue("active ${metrics.activeMinutes}", metrics.activeMinutes in 5..60)
            assertTrue("sleep ${metrics.sleepMinutes}", metrics.sleepMinutes in 240..500)
        }
    }

    @Test
    fun `activity level matches the steps and active minutes of the day`() {
        MockHealthData.recentDays(anyDay, 14).forEach { metrics ->
            assertEquals(
                ActivityLevel.from(metrics.steps, metrics.activeMinutes),
                metrics.activityLevel
            )
        }
    }

    @Test
    fun `a week of mock data includes at least one poor night, so the sleep rule is reachable`() {
        val week = MockHealthData.recentDays(anyDay, 7)

        assertTrue(week.any { it.sleepMinutes < 360 })
    }

    @Test
    fun `mock data never raises a heart rate alert, because the range stays healthy`() {
        val week = MockHealthData.recentDays(anyDay, 7)

        val alerts = HealthAlertEvaluator.evaluate(week.first(), week)

        assertTrue(alerts.none { it.type == HealthAlertType.HIGH_RESTING_HEART_RATE })
        assertTrue(alerts.none { it.type == HealthAlertType.LOW_RESTING_HEART_RATE })
    }
}
