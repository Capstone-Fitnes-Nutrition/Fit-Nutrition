package sheridan.dheripu.fitnutrition.model

/** Shared, provider-independent context and request/response contracts for recommendation work. */
data class MealRecommendationContext(
    val fitnessGoal: String?,
    val dailyCalorieTarget: Int?,
    val dailyProteinTarget: Int?,
    val dietaryPreference: String?,
    val dietaryRestrictions: List<String>,
    val recentMeals: List<MealHistoryItem>,
    val historyNote: String? = null
)

data class MealHistoryItem(
    val name: String,
    val calories: Int,
    val proteinGrams: Double,
    val loggedAtEpochMillis: Long
)

data class WorkoutRecommendationContext(
    val fitnessGoal: String?,
    val activityLevel: String?,
    val availableEquipment: List<String>,
    val recentWorkouts: List<WorkoutHistoryItem>,
    val historyNote: String? = null
)

data class WorkoutHistoryItem(
    val exerciseName: String,
    val sets: Int,
    val repetitions: Int,
    val durationMinutes: Int,
    val completedAtEpochMillis: Long,
    val bodyPart: String = ""
)

data class RecommendationRequest<T>(
    val schemaVersion: Int = 1,
    val recommendationType: String,
    val context: T,
    val instruction: String
)

data class RecommendationResponse<T>(
    val schemaVersion: Int = 1,
    val recommendations: List<T>,
    val rationale: String
)

data class MealSuggestion(
    val name: String,
    val description: String,
    val ingredients: List<String>,
    val estimatedCalories: Int?,
    val estimatedProteinGrams: Double?
)

data class WorkoutSuggestion(
    val name: String,
    val exercises: List<WorkoutExerciseSuggestion>,
    val rationale: String
)

data class WorkoutExerciseSuggestion(
    val name: String,
    val sets: Int?,
    val repetitions: Int?,
    val durationMinutes: Int?
)

sealed interface ContextPreparation<out T> {
    data class Ready<T>(val value: T) : ContextPreparation<T>
    data class NeedsProfile(val missingFields: List<String>) : ContextPreparation<Nothing>
}

/** Builds validated sample-ready contexts; live AI integration is deliberately out of scope. */
object RecommendationContextFactory {
    fun meal(
        profile: User?,
        history: List<NutritionLog>,
        historyWarning: String? = null
    ): ContextPreparation<RecommendationRequest<MealRecommendationContext>> {
        if (profile == null) return ContextPreparation.NeedsProfile(listOf("nutrition profile"))

        val missing = buildList {
            if (profile.dietaryPreference.isBlank()) add("dietary preference")
            if (profile.dailyCalorieTarget.isNotBlank() &&
                profile.dailyCalorieTarget.toIntOrNull()?.let { it > 0 } != true
            ) add("valid daily calorie target")
            if (profile.dailyProteinTarget.isNotBlank() &&
                profile.dailyProteinTarget.toIntOrNull()?.let { it > 0 } != true
            ) add("valid daily protein target")
        }
        if (missing.isNotEmpty()) return ContextPreparation.NeedsProfile(missing)

        val context = MealRecommendationContext(
            fitnessGoal = profile.fitnessGoal.takeIf(String::isNotBlank),
            dailyCalorieTarget = profile.dailyCalorieTarget.toIntOrNull(),
            dailyProteinTarget = profile.dailyProteinTarget.toIntOrNull(),
            dietaryPreference = profile.dietaryPreference,
            dietaryRestrictions = profile.dietaryRestrictions,
            recentMeals = history.sortedByDescending { it.loggedAt }
                .take(MAX_HISTORY_ITEMS).map {
                MealHistoryItem(it.mealName, it.calories, it.proteinGrams, it.loggedAt)
            },
            historyNote = historyWarning ?: history.takeIf { it.isEmpty() }
                ?.let { "No meal history yet; use profile preferences and targets only." }
        )
        return ContextPreparation.Ready(
            RecommendationRequest(
                recommendationType = "meal",
                context = context,
                instruction = "Suggest balanced meals that respect the user's dietary needs."
            )
        )
    }

    fun workout(
        profile: User?,
        history: List<WorkoutItem>,
        historyWarning: String? = null
    ): ContextPreparation<RecommendationRequest<WorkoutRecommendationContext>> {
        if (profile == null) return ContextPreparation.NeedsProfile(listOf("fitness profile"))

        val missing = buildList {
            if (profile.fitnessGoal.isBlank()) add("fitness goal")
            if (profile.activityLevel.isBlank()) add("activity level")
            if (profile.availableEquipment.isEmpty()) add("available equipment")
        }
        if (missing.isNotEmpty()) return ContextPreparation.NeedsProfile(missing)

        val context = WorkoutRecommendationContext(
            fitnessGoal = profile.fitnessGoal,
            activityLevel = profile.activityLevel,
            availableEquipment = profile.availableEquipment,
            recentWorkouts = history.sortedByDescending { it.completedAt }
                .take(MAX_HISTORY_ITEMS).map {
                WorkoutHistoryItem(
                    exerciseName = it.exercise.name,
                    sets = it.sets,
                    repetitions = it.reps,
                    durationMinutes = it.time,
                    completedAtEpochMillis = it.completedAt,
                    bodyPart = it.exercise.bodyPart
                )
            },
            historyNote = historyWarning ?: history.takeIf { it.isEmpty() }
                ?.let { "No workout history yet; use goals, activity level, and equipment only." }
        )
        return ContextPreparation.Ready(
            RecommendationRequest(
                recommendationType = "workout",
                context = context,
                instruction = "Suggest a safe plan matched to the user's goals, ability, equipment, and history."
            )
        )
    }

    private const val MAX_HISTORY_ITEMS = 10
}
