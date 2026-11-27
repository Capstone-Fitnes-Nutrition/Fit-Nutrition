package sheridan.dheripu.fitnutrition.model.recommendation

import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.WorkoutHistoryItem
import sheridan.dheripu.fitnutrition.model.WorkoutRecommendationContext

/** An exercise with the sets, reps or minutes the engine suggests. */
data class PrescribedExercise(
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val durationMinutes: Int,
    val previous: WorkoutHistoryItem? = null
)

/** Workout rules: equipment, level gating (BR8, BR9), goal fit and progression. */
internal object WorkoutRules {
    private val beginnerEquipment = setOf(
        "", "body weight", "band", "resistance band", "stability ball", "assisted", "roller",
        "medicine ball"
    )
    private val advancedEquipment = setOf("barbell", "olympic barbell", "trap bar")
    private const val ADVANCED_PREREQUISITE_WORKOUTS = 5
    private const val MAX_EQUIPMENT_EXAMPLES = 3
    private const val INTERMEDIATE_PREREQUISITE_WORKOUTS = 3

    val hasEquipment = WorkoutFilter { context, exercise ->
        // Reason: a blank entry in the user's equipment list used to defeat the whole filter,
        // because needed.contains("") is always true, so every barbell exercise passed for a
        // user who owns nothing. Blank and one-character entries are dropped before matching.
        val owned = context.availableEquipment
            .map(::singular)
            .filter { it.length > 1 }
        val needed = exercise.equipment.lowercase()
        needed.isBlank() || needed == "body weight" ||
            owned.any { it.contains("gym") || it == "all" } ||
            owned.any { needed == it || needed.contains(it) }
    }

    /** BR9 beginners see beginner exercises; BR8 harder ones unlock after completed workouts. */
    val withinLevel = WorkoutFilter { context, exercise ->
        difficultyOf(exercise) <= maxDifficulty(context)
    }

    val all = listOf(hasEquipment, withinLevel)

    /** 1 = beginner, 2 = intermediate, 3 = advanced, judged by the equipment an exercise uses. */
    fun difficultyOf(exercise: Exercise): Int {
        val equipment = exercise.equipment.lowercase()
        return when (equipment) {
            in beginnerEquipment -> 1
            in advancedEquipment -> 3
            else -> 2
        }
    }

    /**
     * Explains that [blockedCount] exercises were dropped because they need equipment the user
     * has not listed, naming a few of them. Without this the equipment filter is invisible and
     * looks like it is being ignored.
     */
    fun equipmentLockMessage(blockedCount: Int, neededEquipment: List<String>): String {
        val examples = neededEquipment.distinct().take(MAX_EQUIPMENT_EXAMPLES)
        val plural = if (blockedCount == 1) "exercise needs" else "exercises need"
        val named = if (examples.isEmpty()) "" else " (${examples.joinToString(", ")})"
        return "$blockedCount $plural equipment$named that is not on your list."
    }

    /**
     * Explains that the level gate, not the equipment list, removed [lockedCount] exercises,
     * and names what would unlock them. BR8 and BR9 are easy to mistake for a bug otherwise:
     * a sedentary user who owns a full gym still sees only body-weight work.
     */
    fun levelLockMessage(context: WorkoutRecommendationContext, lockedCount: Int): String {
        val completed = context.recentWorkouts.size
        val unlock = when {
            activityScore(context.activityLevel) >= 3 ->
                "log ${ADVANCED_PREREQUISITE_WORKOUTS - completed} more workouts"
            completed >= INTERMEDIATE_PREREQUISITE_WORKOUTS ->
                "raise your activity level"
            else ->
                "raise your activity level above " +
                    context.activityLevel.orEmpty().ifBlank { "sedentary" }.lowercase() +
                    ", or log ${INTERMEDIATE_PREREQUISITE_WORKOUTS - completed} more workouts"
        }
        val plural = if (lockedCount == 1) "exercise is" else "exercises are"
        return "$lockedCount $plural you have equipment for sit above your current level; " +
            "$unlock to unlock them."
    }

    fun maxDifficulty(context: WorkoutRecommendationContext): Int {
        val activity = activityScore(context.activityLevel)
        val completed = context.recentWorkouts.size
        return when {
            completed >= ADVANCED_PREREQUISITE_WORKOUTS && activity >= 3 -> 3
            activity >= 2 || completed >= INTERMEDIATE_PREREQUISITE_WORKOUTS -> 2
            else -> 1
        }
    }

    /** Maps both the profile wording and the synced activity levels onto 1..4. */
    fun activityScore(level: String?): Int {
        val normalized = level.orEmpty().lowercase()
        return when {
            normalized.contains("sedentary") -> 1
            normalized.contains("light") || normalized == "low" -> 2
            normalized.contains("moderate") -> 3
            normalized.contains("very") || normalized == "high" -> 4
            else -> 2
        }
    }

    fun goalMatch(goal: String?, exercise: Exercise): Double {
        val normalized = goal.orEmpty().lowercase()
        val cardio = exercise.isCardio()
        val difficulty = difficultyOf(exercise)
        return when {
            normalized.contains("loss") -> if (cardio) 1.0 else if (difficulty == 1) 0.8 else 0.5
            normalized.contains("gain") -> if (cardio) 0.2 else if (difficulty >= 2) 1.0 else 0.6
            else -> if (cardio) 0.8 else 0.7
        }
    }

    fun lastSessionOf(
        context: WorkoutRecommendationContext,
        exercise: Exercise
    ): WorkoutHistoryItem? =
        context.recentWorkouts
            .filter { it.exerciseName.equals(exercise.name, ignoreCase = true) }
            .maxByOrNull { it.completedAtEpochMillis }

    fun lastTrained(context: WorkoutRecommendationContext, bodyPart: String): Long? =
        context.recentWorkouts
            .filter { it.bodyPart.equals(bodyPart, ignoreCase = true) }
            .maxOfOrNull { it.completedAtEpochMillis }

    /** BR8: repeat exercises progress by one rep over the last session, within a safe cap. */
    fun prescribe(context: WorkoutRecommendationContext, exercise: Exercise): PrescribedExercise {
        val goal = context.fitnessGoal.orEmpty().lowercase()
        val previous = lastSessionOf(context, exercise)
        if (exercise.isCardio()) {
            val minutes = if (goal.contains("loss")) 20 else 10
            return PrescribedExercise(exercise, sets = 1, reps = 0, durationMinutes = minutes,
                previous = previous)
        }
        val (baseSets, baseReps) = when {
            goal.contains("gain") -> 4 to 8
            goal.contains("loss") -> 3 to 15
            else -> 3 to 12
        }
        val reps = previous?.repetitions?.takeIf { it > 0 }
            ?.let { minOf(it + 1, baseReps + MAX_EXTRA_REPS) } ?: baseReps
        val sets = maxOf(baseSets, previous?.sets ?: 0)
        return PrescribedExercise(exercise, sets, reps, durationMinutes = 0, previous = previous)
    }

    private const val MAX_EXTRA_REPS = 4
}

private typealias WorkoutFilter = CandidateFilter<WorkoutRecommendationContext, Exercise>

internal fun Exercise.isCardio(): Boolean = bodyPart.equals("cardio", ignoreCase = true)
