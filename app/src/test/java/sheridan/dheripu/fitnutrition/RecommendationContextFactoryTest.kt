package sheridan.dheripu.fitnutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.google.gson.JsonParser
import com.google.gson.Gson
import sheridan.dheripu.fitnutrition.model.ContextPreparation
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.NutritionLog
import sheridan.dheripu.fitnutrition.model.RecommendationContextFactory
import sheridan.dheripu.fitnutrition.model.RecommendationResponse
import sheridan.dheripu.fitnutrition.model.MealSuggestion
import sheridan.dheripu.fitnutrition.model.WorkoutSuggestion
import sheridan.dheripu.fitnutrition.model.WorkoutExerciseSuggestion
import sheridan.dheripu.fitnutrition.model.User
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.ui.screens.RecommendationDemoFormatter

class RecommendationContextFactoryTest {
    @Test
    fun mealRequestIncludesValidatedPreferencesAndMostRecentHistory() {
        val profile = User(
            fitnessGoal = "Muscle Gain",
            dailyCalorieTarget = "2400",
            dailyProteinTarget = "150",
            dietaryPreference = "Vegetarian",
            dietaryRestrictions = listOf("Peanuts")
        )
        val history = (1..12).map {
            NutritionLog(mealName = "Meal $it", calories = it * 100, loggedAt = it.toLong())
        }

        val result = RecommendationContextFactory.meal(profile, history)

        assertTrue(result is ContextPreparation.Ready)
        val request = (result as ContextPreparation.Ready).value
        assertEquals("meal", request.recommendationType)
        assertEquals("Vegetarian", request.context.dietaryPreference)
        assertEquals(listOf("Peanuts"), request.context.dietaryRestrictions)
        assertEquals(2400, request.context.dailyCalorieTarget)
        assertEquals(150, request.context.dailyProteinTarget)
        assertEquals(10, request.context.recentMeals.size)
        assertEquals("Meal 12", request.context.recentMeals.first().name)
    }

    @Test
    fun mealRequestExplainsMissingProfileAndAllowsMissingHistory() {
        val missingProfile = RecommendationContextFactory.meal(null, emptyList())
        assertEquals(
            listOf("nutrition profile"),
            (missingProfile as ContextPreparation.NeedsProfile).missingFields
        )

        val profile = User(dietaryPreference = "No preference")
        val ready = RecommendationContextFactory.meal(profile, emptyList())
        assertTrue(ready is ContextPreparation.Ready)
        assertTrue((ready as ContextPreparation.Ready).value.context.recentMeals.isEmpty())
        assertTrue(ready.value.context.historyNote?.contains("No meal history") == true)
    }

    @Test
    fun mealRequestRequiresDietaryPreference() {
        val result = RecommendationContextFactory.meal(User(), emptyList())

        assertEquals(
            listOf("dietary preference"),
            (result as ContextPreparation.NeedsProfile).missingFields
        )
    }

    @Test
    fun mealRequestRejectsMalformedNonemptyNutritionTargets() {
        val result = RecommendationContextFactory.meal(
            User(
                dietaryPreference = "No preference",
                dailyCalorieTarget = "many",
                dailyProteinTarget = "0"
            ),
            emptyList()
        )

        assertEquals(
            listOf("valid daily calorie target", "valid daily protein target"),
            (result as ContextPreparation.NeedsProfile).missingFields
        )
    }

    @Test
    fun mealDemoUsesReadableFormattingAndPromptsForMissingProfileFields() {
        val incomplete = RecommendationContextFactory.meal(User(), emptyList())
        val prompt = RecommendationDemoFormatter.meal(incomplete)

        assertTrue(prompt.contains("Choose a dietary preference"))
        assertTrue(prompt.contains("Open Profile → Edit Profile"))

        val ready = RecommendationContextFactory.meal(
            User(dietaryPreference = "Vegetarian"),
            emptyList()
        )
        val display = RecommendationDemoFormatter.meal(ready)
        assertTrue(display.contains("Sample meal"))
        assertTrue(display.contains("Lentil and brown rice bowl"))
        assertTrue(display.contains("No meal history"))
        assertTrue(!display.contains("schemaVersion"))
        assertTrue(!display.contains("{\""))
    }

    @Test
    fun workoutRequestValidatesRequiredFieldsAndUsesRecentHistory() {
        val incomplete = RecommendationContextFactory.workout(User(), emptyList())
        assertEquals(
            listOf("fitness goal", "activity level", "available equipment"),
            (incomplete as ContextPreparation.NeedsProfile).missingFields
        )

        val profile = User(
            fitnessGoal = "General Fitness",
            activityLevel = "Moderately Active",
            availableEquipment = listOf("Dumbbells")
        )
        val history = (1..12).map {
            WorkoutItem(
                exercise = Exercise(name = "Exercise $it"),
                sets = 3,
                reps = 8,
                time = 20,
                completedAt = it.toLong()
            )
        }

        val ready = RecommendationContextFactory.workout(profile, history)

        assertTrue(ready is ContextPreparation.Ready)
        val request = (ready as ContextPreparation.Ready).value
        assertEquals("workout", request.recommendationType)
        assertEquals("Moderately Active", request.context.activityLevel)
        assertEquals(listOf("Dumbbells"), request.context.availableEquipment)
        assertEquals(10, request.context.recentWorkouts.size)
        assertEquals("Exercise 12", request.context.recentWorkouts.first().exerciseName)
    }

    @Test
    fun workoutRequestAllowsMissingHistoryWithExplicitNote() {
        val profile = User(
            fitnessGoal = "General Fitness",
            activityLevel = "Lightly Active",
            availableEquipment = listOf("None")
        )

        val ready = RecommendationContextFactory.workout(profile, emptyList())

        assertTrue(ready is ContextPreparation.Ready)
        val context = (ready as ContextPreparation.Ready).value.context
        assertTrue(context.recentWorkouts.isEmpty())
        assertTrue(context.historyNote?.contains("No workout history") == true)
    }

    @Test
    fun workoutDemoPromptsForMissingFieldsAndExplainsUnavailableHistory() {
        val incomplete = RecommendationContextFactory.workout(User(), emptyList())
        val prompt = RecommendationDemoFormatter.workout(incomplete)

        assertTrue(prompt.contains("Choose a fitness goal"))
        assertTrue(prompt.contains("Choose an activity level"))
        assertTrue(prompt.contains("List available workout equipment"))

        val ready = RecommendationContextFactory.workout(
            User(
                fitnessGoal = "General Fitness",
                activityLevel = "Moderately Active",
                availableEquipment = listOf("Dumbbells")
            ),
            emptyList(),
            historyWarning = "Workout history is unavailable; using profile data only."
        )
        val display = RecommendationDemoFormatter.workout(ready)
        assertTrue(display.contains("Workout history is unavailable"))
        assertTrue(display.contains("Goblet squat"))
        assertTrue(!display.contains("schemaVersion"))
    }

    @Test
    fun sampleMealAndWorkoutResponsesSerializeWithTheAgreedShape() {
        val meal = RecommendationResponse(
            recommendations = listOf(
                MealSuggestion(
                    name = "Lentil bowl",
                    description = "Lentils, grains, and vegetables",
                    ingredients = listOf("Lentils", "Brown rice"),
                    estimatedCalories = 520,
                    estimatedProteinGrams = 28.0
                )
            ),
            rationale = "Vegetarian and protein-forward"
        )
        val workout = RecommendationResponse(
            recommendations = listOf(
                WorkoutSuggestion(
                    name = "Full body basics",
                    exercises = listOf(
                        WorkoutExerciseSuggestion("Goblet squat", 3, 8, null)
                    ),
                    rationale = "Matches the available dumbbell equipment"
                )
            ),
            rationale = "Matched to the user's goals and activity level"
        )

        val mealJson = JsonParser.parseString(Gson().toJson(meal)).asJsonObject
        val workoutJson = JsonParser.parseString(Gson().toJson(workout)).asJsonObject

        assertEquals(1, mealJson.get("schemaVersion").asInt)
        assertTrue(mealJson.getAsJsonArray("recommendations")[0]
            .asJsonObject.has("estimatedProteinGrams"))
        assertTrue(mealJson.has("rationale"))
        assertEquals(1, workoutJson.get("schemaVersion").asInt)
        assertTrue(workoutJson.getAsJsonArray("recommendations")[0]
            .asJsonObject.getAsJsonArray("exercises")[0].asJsonObject.has("sets"))
        assertTrue(workoutJson.has("rationale"))
    }
}
