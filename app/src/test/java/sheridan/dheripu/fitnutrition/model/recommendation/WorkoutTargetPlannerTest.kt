package sheridan.dheripu.fitnutrition.model.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import sheridan.dheripu.fitnutrition.model.WorkoutHistoryItem
import sheridan.dheripu.fitnutrition.model.WorkoutRecommendationContext

class WorkoutTargetPlannerTest {
    @Test
    fun weightLossStartsWithCardioThenRestedMuscleGroups() {
        val bodyParts = WorkoutTargetPlanner.bodyPartsFor(context("Weight Loss"))

        assertEquals(listOf("cardio", "chest", "back"), bodyParts)
    }

    @Test
    fun muscleGainSkipsMostRecentlyTrainedGroups() {
        val recent = listOf(trained("chest", 1L), trained("back", 2L))

        val bodyParts = WorkoutTargetPlanner.bodyPartsFor(context("Muscle Gain", recent))

        assertEquals(3, bodyParts.size)
        assertFalse(bodyParts.contains("chest"))
        assertFalse(bodyParts.contains("back"))
        assertFalse(bodyParts.contains("cardio"))
    }

    private fun context(goal: String, recent: List<WorkoutHistoryItem> = emptyList()) =
        WorkoutRecommendationContext(
            fitnessGoal = goal,
            activityLevel = "Moderately Active",
            availableEquipment = listOf("Body weight"),
            recentWorkouts = recent
        )

    private fun trained(bodyPart: String, at: Long) = WorkoutHistoryItem(
        exerciseName = "$bodyPart move",
        sets = 3,
        repetitions = 10,
        durationMinutes = 0,
        completedAtEpochMillis = at,
        bodyPart = bodyPart
    )
}
