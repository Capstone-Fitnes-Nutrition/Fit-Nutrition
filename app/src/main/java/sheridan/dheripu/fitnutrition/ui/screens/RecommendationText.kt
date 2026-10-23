package sheridan.dheripu.fitnutrition.ui.screens

import sheridan.dheripu.fitnutrition.model.recommendation.MealPick
import sheridan.dheripu.fitnutrition.model.recommendation.PrescribedExercise

/** Display text for recommendation cards. */
object RecommendationText {
    fun missingField(field: String): String = when (field) {
        "nutrition profile" -> "Create or complete your nutrition profile"
        "dietary preference" -> "Choose a dietary preference (or No preference)"
        "valid daily calorie target" -> "Enter a positive daily calorie target, or leave it blank"
        "valid daily protein target" -> "Enter a positive daily protein target, or leave it blank"
        "fitness profile" -> "Create or complete your fitness profile"
        "fitness goal" -> "Choose a fitness goal"
        "activity level" -> "Choose an activity level"
        "available equipment" -> "List available workout equipment"
        else -> field
    }

    fun summary(pick: MealPick): String {
        val servings = if (pick.servings % 1.0 == 0.0) pick.servings.toInt().toString()
        else pick.servings.toString()
        val label = if (pick.servings == 1.0) "serving" else "servings"
        return "$servings $label · ${pick.calories} kcal · ${pick.proteinGrams.toInt()} g protein"
    }

    fun summary(prescription: PrescribedExercise): String =
        if (prescription.durationMinutes > 0) {
            "${prescription.durationMinutes} min"
        } else {
            "${prescription.sets} sets × ${prescription.reps} reps"
        }
}
