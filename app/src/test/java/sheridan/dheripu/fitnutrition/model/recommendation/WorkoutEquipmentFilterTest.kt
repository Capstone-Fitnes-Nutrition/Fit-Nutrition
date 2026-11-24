package sheridan.dheripu.fitnutrition.model.recommendation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.WorkoutRecommendationContext

class WorkoutEquipmentFilterTest {

    private fun context(vararg equipment: String) = WorkoutRecommendationContext(
        fitnessGoal = "Muscle Gain",
        activityLevel = "Very Active",
        availableEquipment = equipment.toList(),
        recentWorkouts = emptyList()
    )

    private fun exercise(equipment: String) =
        Exercise("1", "move", "chest", "", emptyList(), equipment)

    @Test
    fun `a blank equipment entry does not let every exercise through`() {
        // Reason: the match used needed.contains(owned), and contains("") is always true, so one
        // empty entry in the profile passed every barbell exercise for a user who owns nothing.
        val filter = WorkoutRules.hasEquipment

        assertFalse(filter.keep(context(""), exercise("barbell")))
        assertFalse(filter.keep(context("", "  "), exercise("dumbbell")))
    }

    @Test
    fun `owned equipment still matches, plural or singular`() {
        val filter = WorkoutRules.hasEquipment

        assertTrue(filter.keep(context("Dumbbells"), exercise("dumbbell")))
        assertTrue(filter.keep(context("barbell"), exercise("olympic barbell")))
    }

    @Test
    fun `body weight and gym access need no listed equipment`() {
        val filter = WorkoutRules.hasEquipment

        assertTrue(filter.keep(context(), exercise("body weight")))
        assertTrue(filter.keep(context(), exercise("")))
        assertTrue(filter.keep(context("Full gym"), exercise("barbell")))
    }

    @Test
    fun `equipment the user does not own is still excluded`() {
        val filter = WorkoutRules.hasEquipment

        assertFalse(filter.keep(context("Dumbbells"), exercise("barbell")))
    }
}
