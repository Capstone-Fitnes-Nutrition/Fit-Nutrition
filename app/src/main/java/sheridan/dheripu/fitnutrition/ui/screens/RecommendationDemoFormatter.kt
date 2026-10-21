package sheridan.dheripu.fitnutrition.ui.screens

import sheridan.dheripu.fitnutrition.model.ContextPreparation
import sheridan.dheripu.fitnutrition.model.MealRecommendationContext
import sheridan.dheripu.fitnutrition.model.MealSuggestion
import sheridan.dheripu.fitnutrition.model.RecommendationRequest
import sheridan.dheripu.fitnutrition.model.RecommendationResponse
import sheridan.dheripu.fitnutrition.model.WorkoutExerciseSuggestion
import sheridan.dheripu.fitnutrition.model.WorkoutRecommendationContext
import sheridan.dheripu.fitnutrition.model.WorkoutSuggestion

/** Formats sample request/response payloads for manual context-flow verification without AI. */
object RecommendationDemoFormatter {
    fun meal(
        preparation: ContextPreparation<RecommendationRequest<MealRecommendationContext>>
    ): String = when (preparation) {
        is ContextPreparation.NeedsProfile ->
            "Complete these profile fields to personalize meal ideas:\n" +
                preparation.missingFields.joinToString("\n") { "• ${mealFieldLabel(it)}" } +
                "\n\nOpen Profile → Edit Profile. Dietary restrictions are optional; add any " +
                "allergies or foods you avoid."
        is ContextPreparation.Ready -> {
            val context = preparation.value.context
            val sampleResponse = RecommendationResponse(
                recommendations = listOf(
                    MealSuggestion(
                        name = "Lentil and brown rice bowl",
                        description = "A balanced vegetarian meal with vegetables.",
                        ingredients = listOf("Lentils", "Brown rice", "Mixed vegetables"),
                        estimatedCalories = 520,
                        estimatedProteinGrams = 28.0
                    )
                ),
                rationale = "Sample response using the prepared nutrition context."
            )
            val suggestion = sampleResponse.recommendations.first()
            buildString {
                appendLine("Request context · sample only; no AI call")
                appendLine("Diet: ${context.dietaryPreference ?: "Not set"}")
                appendLine(
                    "Restrictions: " +
                        context.dietaryRestrictions.ifEmpty { listOf("None added") }.joinToString()
                )
                appendLine("Daily calorie target: ${context.dailyCalorieTarget?.let { "$it kcal" } ?: "Not set"}")
                appendLine("Daily protein target: ${context.dailyProteinTarget?.let { "$it g" } ?: "Not set"}")
                appendLine("Recent meals included: ${context.recentMeals.size}")
                context.historyNote?.let { appendLine("History: $it") }
                appendLine()
                appendLine("Sample meal")
                appendLine(suggestion.name)
                appendLine(suggestion.description)
                appendLine(
                    "Estimated nutrition: ${suggestion.estimatedCalories ?: "?"} kcal · " +
                        "${suggestion.estimatedProteinGrams?.toInt() ?: "?"} g protein"
                )
                appendLine("Ingredients: ${suggestion.ingredients.joinToString()}")
                append("Why: ${sampleResponse.rationale}")
            }
        }
    }

    fun workout(
        preparation: ContextPreparation<RecommendationRequest<WorkoutRecommendationContext>>
    ): String = when (preparation) {
        is ContextPreparation.NeedsProfile ->
            "Complete these profile fields to personalize workouts:\n" +
                preparation.missingFields.joinToString("\n") { "• ${workoutFieldLabel(it)}" } +
                "\n\nOpen Profile → Edit Profile. If you do not have equipment, enter " +
                "‘None’ in the equipment field."
        is ContextPreparation.Ready -> {
            val context = preparation.value.context
            val sampleResponse = RecommendationResponse(
                recommendations = listOf(
                    WorkoutSuggestion(
                        name = "Full body basics",
                        exercises = listOf(
                            WorkoutExerciseSuggestion("Goblet squat", 3, 8, null),
                            WorkoutExerciseSuggestion("Dumbbell row", 3, 10, null)
                        ),
                        rationale = "Uses the equipment listed in the fitness profile."
                    )
                ),
                rationale = "Sample response using the prepared workout context."
            )
            val suggestion = sampleResponse.recommendations.first()
            buildString {
                appendLine("Request context · sample only; no AI call")
                appendLine("Goal: ${context.fitnessGoal ?: "Not set"}")
                appendLine("Activity level: ${context.activityLevel ?: "Not set"}")
                appendLine(
                    "Available equipment: " +
                        context.availableEquipment.ifEmpty { listOf("None added") }.joinToString()
                )
                appendLine("Recent workouts included: ${context.recentWorkouts.size}")
                context.historyNote?.let { appendLine("History: $it") }
                appendLine()
                appendLine("Sample workout")
                appendLine(suggestion.name)
                suggestion.exercises.forEach { exercise ->
                    appendLine("• ${exercise.name}: ${describeExercise(exercise)}")
                }
                append("Why: ${sampleResponse.rationale}")
            }
        }
    }

    private fun describeExercise(exercise: WorkoutExerciseSuggestion): String {
        val parts = buildList {
            exercise.sets?.let { add("$it sets") }
            exercise.repetitions?.let { add("$it reps") }
            exercise.durationMinutes?.let { add("$it min") }
        }
        return parts.ifEmpty { listOf("as tolerated") }.joinToString(" × ")
    }

    private fun mealFieldLabel(field: String): String = when (field) {
        "nutrition profile" -> "Create or complete your nutrition profile"
        "dietary preference" -> "Choose a dietary preference (or No preference)"
        "valid daily calorie target" -> "Enter a positive daily calorie target, or leave it blank"
        "valid daily protein target" -> "Enter a positive daily protein target, or leave it blank"
        else -> field
    }

    private fun workoutFieldLabel(field: String): String = when (field) {
        "fitness profile" -> "Create or complete your fitness profile"
        "fitness goal" -> "Choose a fitness goal"
        "activity level" -> "Choose an activity level"
        "available equipment" -> "List available workout equipment"
        else -> field
    }
}
