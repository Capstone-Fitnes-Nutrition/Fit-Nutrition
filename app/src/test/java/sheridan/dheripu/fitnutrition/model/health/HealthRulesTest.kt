package sheridan.dheripu.fitnutrition.model.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sheridan.dheripu.fitnutrition.model.HealthMetrics

class HealthRulesTest {
    @Test
    fun activityLevelFollowsStepAndActiveMinuteThresholds() {
        assertEquals("HIGH", ActivityLevel.from(steps = 10_000, activeMinutes = 30))
        assertEquals("MODERATE", ActivityLevel.from(steps = 10_000, activeMinutes = 29))
        assertEquals("MODERATE", ActivityLevel.from(steps = 5_000, activeMinutes = 15))
        assertEquals("LOW", ActivityLevel.from(steps = 2_500, activeMinutes = 0))
        assertEquals("SEDENTARY", ActivityLevel.from(steps = 2_499, activeMinutes = 60))
    }

    @Test
    fun restingHeartRateOutsideRangeRaisesAlert() {
        val high = HealthAlertEvaluator.evaluate(HealthMetrics(heartRate = 104), emptyList())
        val low = HealthAlertEvaluator.evaluate(HealthMetrics(heartRate = 38), emptyList())
        val missing = HealthAlertEvaluator.evaluate(HealthMetrics(heartRate = 0), emptyList())
        val normal = HealthAlertEvaluator.evaluate(HealthMetrics(heartRate = 62), emptyList())

        assertEquals(listOf(HealthAlertType.HIGH_RESTING_HEART_RATE), high.map { it.type })
        assertEquals(listOf(HealthAlertType.LOW_RESTING_HEART_RATE), low.map { it.type })
        assertTrue(missing.isEmpty())
        assertTrue(normal.isEmpty())
    }

    @Test
    fun threeConsecutivePoorNightsRaiseSleepAlert() {
        val week = listOf(
            night("2026-10-23", 300),
            night("2026-10-22", 320),
            night("2026-10-21", 340),
            night("2026-10-20", 480)
        )

        val alerts = HealthAlertEvaluator.evaluate(null, week)

        assertEquals(listOf(HealthAlertType.POOR_SLEEP_STREAK), alerts.map { it.type })
    }

    @Test
    fun sleepAlertNeedsThreeConsecutiveDaysWithData() {
        val gap = listOf(
            night("2026-10-23", 300), night("2026-10-21", 300), night("2026-10-20", 300)
        )
        val twoNights = listOf(night("2026-10-23", 300), night("2026-10-22", 300))
        val noData = listOf(night("2026-10-23", 0), night("2026-10-22", 0), night("2026-10-21", 0))

        assertTrue(HealthAlertEvaluator.evaluate(null, gap).isEmpty())
        assertTrue(HealthAlertEvaluator.evaluate(null, twoNights).isEmpty())
        assertTrue(HealthAlertEvaluator.evaluate(null, noData).isEmpty())
    }

    private fun night(date: String, minutes: Int) =
        HealthMetrics(date = date, sleepMinutes = minutes)
}
