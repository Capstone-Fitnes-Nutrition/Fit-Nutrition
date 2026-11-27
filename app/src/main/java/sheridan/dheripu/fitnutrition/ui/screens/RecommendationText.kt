package sheridan.dheripu.fitnutrition.ui.screens

import sheridan.dheripu.fitnutrition.model.recommendation.MealPick
import sheridan.dheripu.fitnutrition.model.recommendation.PrescribedExercise

/**
 * The one figure that decides whether a suggestion is worth reading, and the detail that
 * qualifies it.
 *
 * Reason: these used to be one run-on line of middle-dot separated values, which made the
 * number the user is actually choosing on — the calories, or the sets and reps — no easier to
 * find than the units beside it.
 */
data class SuggestionMetric(val lead: String, val detail: String)

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

    fun metric(pick: MealPick): SuggestionMetric = SuggestionMetric(
        lead = "${pick.calories} kcal",
        detail = "${servings(pick)} · ${pick.proteinGrams.toInt()} g protein"
    )

    fun metric(prescription: PrescribedExercise): SuggestionMetric = SuggestionMetric(
        lead = if (prescription.durationMinutes > 0) {
            "${prescription.durationMinutes} min"
        } else {
            "${prescription.sets} × ${prescription.reps}"
        },
        detail = listOf(prescription.exercise.bodyPart, prescription.exercise.equipment)
            .filter(String::isNotBlank)
            .joinToString(" · ")
            .ifBlank { if (prescription.durationMinutes > 0) "cardio" else "sets × reps" }
    )

    /**
     * The flat one-line form, for places with no room to split the figure out: the dismissed
     * list, and anywhere a suggestion is named rather than offered.
     */
    fun summary(pick: MealPick): String =
        "${servings(pick)} · ${pick.calories} kcal · ${pick.proteinGrams.toInt()} g protein"

    fun summary(prescription: PrescribedExercise): String =
        if (prescription.durationMinutes > 0) {
            "${prescription.durationMinutes} min"
        } else {
            "${prescription.sets} sets × ${prescription.reps} reps"
        }

    private fun servings(pick: MealPick): String {
        val count = if (pick.servings % 1.0 == 0.0) {
            pick.servings.toInt().toString()
        } else {
            pick.servings.toString()
        }
        return "$count ${if (pick.servings == 1.0) "serving" else "servings"}"
    }
}
