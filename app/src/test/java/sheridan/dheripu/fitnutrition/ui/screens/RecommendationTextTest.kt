package sheridan.dheripu.fitnutrition.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.model.recommendation.MealPick
import sheridan.dheripu.fitnutrition.model.recommendation.PrescribedExercise

class RecommendationTextTest {
    @Test
    fun missingFieldsBecomeActionableInstructions() {
        assertEquals(
            "Choose a dietary preference (or No preference)",
            RecommendationText.missingField("dietary preference")
        )
        assertEquals("List available workout equipment",
            RecommendationText.missingField("available equipment"))
        assertEquals("something new", RecommendationText.missingField("something new"))
    }

    @Test
    fun mealSummaryShowsPortionCaloriesAndProtein() {
        val pick = MealPick(recipe(), servings = 1.5, calories = 540, proteinGrams = 31.6,
            carbohydratesGrams = 0.0, fatGrams = 0.0)

        assertEquals("1.5 servings · 540 kcal · 31 g protein", RecommendationText.summary(pick))
    }

    @Test
    fun exerciseSummaryUsesRepsOrMinutes() {
        val strength = PrescribedExercise(Exercise(name = "push up"), sets = 3, reps = 12,
            durationMinutes = 0)
        val cardio = PrescribedExercise(Exercise(name = "run", bodyPart = "cardio"), sets = 1,
            reps = 0, durationMinutes = 20)

        assertEquals("3 sets × 12 reps", RecommendationText.summary(strength))
        assertEquals("20 min", RecommendationText.summary(cardio))
    }

    private fun recipe() = Recipe(1, "Bowl", "", 10, 1, null, "", emptyList(), null, emptyList(),
        emptyList())
}
