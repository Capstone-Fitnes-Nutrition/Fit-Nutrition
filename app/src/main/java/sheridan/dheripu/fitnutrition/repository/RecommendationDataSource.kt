package sheridan.dheripu.fitnutrition.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import sheridan.dheripu.fitnutrition.model.ContextPreparation
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.MealRecommendationContext
import sheridan.dheripu.fitnutrition.model.NutritionLog
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.model.RecipeFilters
import sheridan.dheripu.fitnutrition.model.RecommendationRequest
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.model.WorkoutRecommendationContext
import sheridan.dheripu.fitnutrition.model.recommendation.RecommendationFeedback
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

typealias MealPreparation = ContextPreparation<RecommendationRequest<MealRecommendationContext>>
typealias WorkoutPreparation = ContextPreparation<RecommendationRequest<WorkoutRecommendationContext>>

/**
 * Everything the recommendation flow reads or writes. Failures are thrown as exceptions so
 * the ViewModel decides which ones are fatal and which degrade gracefully.
 */
interface RecommendationDataSource {
    suspend fun mealContext(userId: String): MealPreparation
    suspend fun workoutContext(userId: String): WorkoutPreparation
    suspend fun recipeCandidates(filters: RecipeFilters): List<Recipe>
    suspend fun exerciseCandidates(bodyPart: String): List<Exercise>

    /** Today's Fitbit activity level, or null when no Fitbit data was synced today. */
    suspend fun latestActivityLevel(): String?
    suspend fun recentFeedback(userId: String): List<RecommendationFeedback>
    suspend fun recordFeedback(userId: String, events: List<RecommendationFeedback>)
    suspend fun logMeal(userId: String, log: NutritionLog)
    suspend fun logWorkout(userId: String, item: WorkoutItem)
}

class RecommendationDataException(message: String) : Exception(message)

/** Adapts the callback-based repositories to [RecommendationDataSource]. */
class FirebaseRecommendationDataSource(
    private val contextRepository: RecommendationContextRepository = RecommendationContextRepository(),
    private val recipeRepository: RecipeRepository = RecipeRepository(),
    private val fitnessRepository: FitnessRepository = FitnessRepository(),
    private val healthRepository: HealthRepository = HealthRepository(),
    private val feedbackRepository: RecommendationFeedbackRepository = RecommendationFeedbackRepository(),
    private val nutritionRepository: NutritionRepository = NutritionRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository()
) : RecommendationDataSource {

    override suspend fun mealContext(userId: String): MealPreparation = callback { done, fail ->
        contextRepository.prepareMeal(userId, done, fail)
    }

    override suspend fun workoutContext(userId: String): WorkoutPreparation = callback { done, fail ->
        contextRepository.prepareWorkout(userId, done, fail)
    }

    override suspend fun recipeCandidates(filters: RecipeFilters): List<Recipe> =
        recipeRepository.searchRecipes(filters)

    override suspend fun exerciseCandidates(bodyPart: String): List<Exercise> =
        callback { done, fail -> fitnessRepository.getExercisesByBodyPart(bodyPart, done, fail) }

    override suspend fun latestActivityLevel(): String? {
        val metrics = withTimeoutOrNull(HEALTH_TIMEOUT_MILLIS) {
            healthRepository.observeHealthMetrics().first()
        }?.getOrNull() ?: return null
        return metrics.activityLevel.takeIf { metrics.steps > 0 && it != "UNKNOWN" }
    }

    override suspend fun recentFeedback(userId: String): List<RecommendationFeedback> =
        callback { done, fail -> feedbackRepository.loadRecent(userId, FEEDBACK_LIMIT, done, fail) }

    override suspend fun recordFeedback(userId: String, events: List<RecommendationFeedback>) =
        callback { done, fail -> feedbackRepository.record(userId, events, { done(Unit) }, fail) }

    override suspend fun logMeal(userId: String, log: NutritionLog) = callback { done, fail ->
        nutritionRepository.addNutritionLog(userId, log, { done(Unit) }, fail)
    }

    override suspend fun logWorkout(userId: String, item: WorkoutItem) = callback { done, fail ->
        workoutRepository.addWorkout(userId, item, { done(Unit) }, fail)
    }

    private suspend fun <T> callback(
        start: (onSuccess: (T) -> Unit, onError: (String) -> Unit) -> Unit
    ): T = suspendCancellableCoroutine { continuation ->
        start(
            { value -> if (continuation.isActive) continuation.resume(value) },
            { message ->
                if (continuation.isActive) {
                    continuation.resumeWithException(RecommendationDataException(message))
                }
            }
        )
    }

    companion object {
        private const val FEEDBACK_LIMIT = 100L
        private const val HEALTH_TIMEOUT_MILLIS = 3_000L
    }
}
