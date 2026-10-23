package sheridan.dheripu.fitnutrition.model.recommendation

import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.RecommendationResponse
import sheridan.dheripu.fitnutrition.model.WorkoutRecommendationContext

/**
 * Content-based workout ranking: equipment and level filters, then weighted features (goal
 * match, intensity fit, muscle recovery, novelty, learned affinity), then up to five exercises
 * spread across body parts, each with a progressive prescription.
 */
class WorkoutRecommendationEngine(
    private val weights: ScoringWeights = DEFAULT_WEIGHTS
) : RecommendationEngine<WorkoutRecommendationContext, Exercise, PrescribedExercise> {

    override fun recommend(
        context: WorkoutRecommendationContext,
        candidates: List<Exercise>,
        preferences: UserPreferenceProfile,
        nowMillis: Long
    ): RecommendationResponse<ScoredRecommendation<PrescribedExercise>> {
        val eligible = candidates
            .distinctBy { it.id }
            .filterNot { it.id in preferences.dismissedItemIds }
            .filter { exercise -> WorkoutRules.all.all { it.keep(context, exercise) } }
        if (eligible.isEmpty()) {
            return RecommendationResponse(
                recommendations = emptyList(),
                rationale = "No exercises matched your equipment and current level."
            )
        }
        val ranked = eligible.map { score(context, it, preferences, nowMillis) }.ranked()
        val picks = pickVaried(ranked, MAX_RESULTS, maxPerGroup = 2) {
            it.item.exercise.bodyPart.lowercase()
        }
        return RecommendationResponse(
            recommendations = picks,
            rationale = "Ranked ${eligible.size} of ${candidates.size} exercises for your level."
        )
    }

    private fun score(
        context: WorkoutRecommendationContext,
        exercise: Exercise,
        preferences: UserPreferenceProfile,
        nowMillis: Long
    ): ScoredRecommendation<PrescribedExercise> {
        val tags = listOf(exercise.bodyPart, exercise.target, exercise.equipment)
            .filter(String::isNotBlank)
            .map(::normalizeTag)
        val maxDifficulty = WorkoutRules.maxDifficulty(context)
        val features = mapOf(
            GOAL_MATCH to WorkoutRules.goalMatch(context.fitnessGoal, exercise),
            INTENSITY_FIT to (1.0 - 0.3 * (maxDifficulty - WorkoutRules.difficultyOf(exercise)))
                .coerceAtLeast(0.4),
            RECOVERY to recovery(context, exercise, nowMillis),
            NOVELTY to novelty(context, exercise, nowMillis),
            LEARNED_AFFINITY to preferences.affinityFor(tags)
        )
        val prescription = WorkoutRules.prescribe(context, exercise)
        return ScoredRecommendation(
            recommendationId = "workout-${exercise.id}-$nowMillis",
            itemId = exercise.id,
            item = prescription,
            score = weights.combine(features),
            reasons = reasons(context, features, prescription),
            tags = tags
        )
    }

    /** BR4 recovery: muscle groups trained in the last two days rank lower. */
    private fun recovery(
        context: WorkoutRecommendationContext,
        exercise: Exercise,
        nowMillis: Long
    ): Double {
        val last = WorkoutRules.lastTrained(context, exercise.bodyPart) ?: return 1.0
        val age = nowMillis - last
        return when {
            age < 2 * DAY_MILLIS -> 0.0
            age < 3 * DAY_MILLIS -> 0.5
            else -> 1.0
        }
    }

    private fun novelty(
        context: WorkoutRecommendationContext,
        exercise: Exercise,
        nowMillis: Long
    ): Double {
        val last = WorkoutRules.lastSessionOf(context, exercise) ?: return 1.0
        return if (nowMillis - last.completedAtEpochMillis < 7 * DAY_MILLIS) 0.4 else 1.0
    }

    private fun reasons(
        context: WorkoutRecommendationContext,
        features: Map<String, Double>,
        prescription: PrescribedExercise
    ): List<String> = buildList {
        val bodyPart = prescription.exercise.bodyPart
        if (context.recentWorkouts.isNotEmpty() && features[RECOVERY] == 1.0) {
            add("Your $bodyPart is rested")
        }
        prescription.previous?.takeIf { !prescription.exercise.isCardio() }?.let {
            add("Progresses from your last ${it.sets}x${it.repetitions}")
        }
        if ((features[GOAL_MATCH] ?: 0.0) >= 0.8) add("Fits your ${context.fitnessGoal} goal")
        if ((features[LEARNED_AFFINITY] ?: 0.0) >= 0.3) add("Similar to exercises you've done")
        if ((features[INTENSITY_FIT] ?: 0.0) >= 0.9) add("Matched to your current level")
    }.take(MAX_REASONS)

    companion object {
        const val GOAL_MATCH = "goalMatch"
        const val INTENSITY_FIT = "intensityFit"
        const val RECOVERY = "recovery"
        const val NOVELTY = "novelty"
        const val LEARNED_AFFINITY = "learnedAffinity"

        val DEFAULT_WEIGHTS = ScoringWeights(
            mapOf(
                GOAL_MATCH to 0.25,
                INTENSITY_FIT to 0.15,
                RECOVERY to 0.25,
                NOVELTY to 0.10,
                LEARNED_AFFINITY to 0.25
            )
        )

        private const val MAX_RESULTS = 5
        private const val MAX_REASONS = 3
    }
}

/** Chooses which body parts to fetch exercises for, favouring rested muscle groups. */
object WorkoutTargetPlanner {
    private val muscleGroups =
        listOf("chest", "back", "upper legs", "shoulders", "upper arms", "waist")
    private const val TARGET_COUNT = 3

    fun bodyPartsFor(context: WorkoutRecommendationContext): List<String> {
        val rested = muscleGroups.sortedBy { WorkoutRules.lastTrained(context, it) ?: 0L }
        val wantsCardio = !context.fitnessGoal.orEmpty().lowercase().contains("gain")
        return if (wantsCardio) {
            listOf("cardio") + rested.take(TARGET_COUNT - 1)
        } else {
            rested.take(TARGET_COUNT)
        }
    }
}
