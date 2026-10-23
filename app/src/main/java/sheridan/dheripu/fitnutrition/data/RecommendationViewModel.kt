package sheridan.dheripu.fitnutrition.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sheridan.dheripu.fitnutrition.AuthManager
import sheridan.dheripu.fitnutrition.model.ContextPreparation
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.MealRecommendationContext
import sheridan.dheripu.fitnutrition.model.NutritionLog
import sheridan.dheripu.fitnutrition.model.RecipeFilters
import sheridan.dheripu.fitnutrition.model.RecommendationResponse
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.model.recommendation.FeedbackAction
import sheridan.dheripu.fitnutrition.model.recommendation.MealPick
import sheridan.dheripu.fitnutrition.model.recommendation.MealRecommendationEngine
import sheridan.dheripu.fitnutrition.model.recommendation.PreferenceLearner
import sheridan.dheripu.fitnutrition.model.recommendation.PrescribedExercise
import sheridan.dheripu.fitnutrition.model.recommendation.RecommendationFeedback
import sheridan.dheripu.fitnutrition.model.recommendation.RecommendationType
import sheridan.dheripu.fitnutrition.model.recommendation.ScoredRecommendation
import sheridan.dheripu.fitnutrition.model.recommendation.UserPreferenceProfile
import sheridan.dheripu.fitnutrition.model.recommendation.WorkoutRecommendationEngine
import sheridan.dheripu.fitnutrition.model.recommendation.WorkoutTargetPlanner
import sheridan.dheripu.fitnutrition.repository.FirebaseRecommendationDataSource
import sheridan.dheripu.fitnutrition.repository.RecommendationDataException
import sheridan.dheripu.fitnutrition.repository.RecommendationDataSource

/**
 * Runs the recommendation pipeline: context, candidates, learned preferences, engine ranking,
 * then logs what was shown and what the user did with it.
 */
class RecommendationViewModel(
    private val dataSource: RecommendationDataSource = FirebaseRecommendationDataSource(),
    private val mealEngine: MealRecommendationEngine = MealRecommendationEngine(),
    private val workoutEngine: WorkoutRecommendationEngine = WorkoutRecommendationEngine(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val userIdProvider: () -> String? = { AuthManager.currentUser?.uid }
) : ViewModel() {

    private val _state = MutableStateFlow<RecommendationUiState>(RecommendationUiState.Idle)
    val state: StateFlow<RecommendationUiState> = _state.asStateFlow()

    fun loadMeals() = load { userId ->
        when (val preparation = dataSource.mealContext(userId)) {
            is ContextPreparation.NeedsProfile ->
                RecommendationUiState.NeedsProfile(preparation.missingFields)
            is ContextPreparation.Ready -> {
                val context = preparation.value.context
                val candidates = dataSource.recipeCandidates(filtersFor(context))
                val response = mealEngine.recommend(
                    context, candidates, preferencesFor(userId), clock()
                )
                recordShown(userId, RecommendationType.MEAL, response.recommendations)
                RecommendationUiState.Meals(
                    response.recommendations,
                    noteOf(context.historyNote, null, response)
                )
            }
        }
    }

    fun loadWorkout() = load { userId ->
        when (val preparation = dataSource.workoutContext(userId)) {
            is ContextPreparation.NeedsProfile ->
                RecommendationUiState.NeedsProfile(preparation.missingFields)
            is ContextPreparation.Ready -> {
                val profileContext = preparation.value.context
                val fitbitLevel = attempt { dataSource.latestActivityLevel() }
                val context = profileContext.copy(
                    activityLevel = fitbitLevel ?: profileContext.activityLevel
                )
                val candidates = exercisesFor(WorkoutTargetPlanner.bodyPartsFor(context))
                val response = workoutEngine.recommend(
                    context, candidates, preferencesFor(userId), clock()
                )
                recordShown(userId, RecommendationType.WORKOUT, response.recommendations)
                val fitbitNote = fitbitLevel?.let { "Using today's Fitbit activity level ($it)." }
                RecommendationUiState.Workout(
                    response.recommendations,
                    noteOf(context.historyNote, fitbitNote, response)
                )
            }
        }
    }

    /** Logs the meal or exercise and records the positive feedback. */
    fun accept(recommendationId: String) {
        val userId = userIdProvider() ?: return
        when (val current = _state.value) {
            is RecommendationUiState.Meals -> current.items
                .firstOrNull { it.recommendationId == recommendationId }
                ?.let { acceptMeal(userId, it) }
            is RecommendationUiState.Workout -> current.items
                .firstOrNull { it.recommendationId == recommendationId }
                ?.let { completeExercise(userId, it) }
            else -> Unit
        }
    }

    /** Hides the suggestion and records the negative feedback with an optional reason. */
    fun dismiss(recommendationId: String, reason: String = "") {
        val userId = userIdProvider() ?: return
        val (dismissed, type) = when (val current = _state.value) {
            is RecommendationUiState.Meals -> {
                _state.value = current.copy(items = current.items.filterNot {
                    it.recommendationId == recommendationId
                })
                current.items.firstOrNull { it.recommendationId == recommendationId } to
                    RecommendationType.MEAL
            }
            is RecommendationUiState.Workout -> {
                _state.value = current.copy(items = current.items.filterNot {
                    it.recommendationId == recommendationId
                })
                current.items.firstOrNull { it.recommendationId == recommendationId } to
                    RecommendationType.WORKOUT
            }
            else -> return
        }
        dismissed ?: return
        viewModelScope.launch {
            attempt {
                dataSource.recordFeedback(
                    userId, listOf(event(dismissed, type, FeedbackAction.DISMISSED, reason))
                )
            }
        }
    }

    private fun acceptMeal(userId: String, pick: ScoredRecommendation<MealPick>) {
        viewModelScope.launch {
            val meal = pick.item
            val logged = attempt {
                dataSource.logMeal(
                    userId,
                    NutritionLog(
                        recipeId = meal.recipe.id,
                        mealName = meal.recipe.title,
                        calories = meal.calories,
                        proteinGrams = meal.proteinGrams,
                        carbohydratesGrams = meal.carbohydratesGrams,
                        fatGrams = meal.fatGrams,
                        loggedAt = clock()
                    )
                )
            } != null
            if (logged) {
                attempt {
                    dataSource.recordFeedback(
                        userId,
                        listOf(event(pick, RecommendationType.MEAL, FeedbackAction.ACCEPTED))
                    )
                }
            }
            _state.update { state ->
                if (state !is RecommendationUiState.Meals) return@update state
                if (logged) {
                    state.copy(handledIds = state.handledIds + pick.recommendationId,
                        notice = "Logged ${meal.recipe.title}.")
                } else {
                    state.copy(notice = "Couldn't log ${meal.recipe.title}. Try again.")
                }
            }
        }
    }

    private fun completeExercise(userId: String, pick: ScoredRecommendation<PrescribedExercise>) {
        viewModelScope.launch {
            val prescription = pick.item
            val logged = attempt {
                dataSource.logWorkout(
                    userId,
                    WorkoutItem(
                        exercise = prescription.exercise,
                        sets = prescription.sets,
                        reps = prescription.reps,
                        time = prescription.durationMinutes,
                        completedAt = clock()
                    )
                )
            } != null
            if (logged) {
                attempt {
                    dataSource.recordFeedback(
                        userId,
                        listOf(event(pick, RecommendationType.WORKOUT, FeedbackAction.COMPLETED))
                    )
                }
            }
            _state.update { state ->
                if (state !is RecommendationUiState.Workout) return@update state
                val name = prescription.exercise.name
                if (logged) {
                    state.copy(handledIds = state.handledIds + pick.recommendationId,
                        notice = "Logged $name.")
                } else {
                    state.copy(notice = "Couldn't log $name. Try again.")
                }
            }
        }
    }

    private fun load(block: suspend (String) -> RecommendationUiState) {
        val userId = userIdProvider()
        if (userId == null) {
            _state.value = RecommendationUiState.SignedOut
            return
        }
        _state.value = RecommendationUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                block(userId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                RecommendationUiState.Error(error.message ?: "Unable to load recommendations")
            }
        }
    }

    private suspend fun exercisesFor(bodyParts: List<String>): List<Exercise> {
        var lastError: Exception? = null
        val loaded = bodyParts.mapNotNull { bodyPart ->
            try {
                dataSource.exerciseCandidates(bodyPart)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastError = error
                null
            }
        }
        if (loaded.isEmpty()) {
            throw RecommendationDataException(
                lastError?.message ?: "Exercise library unavailable. Try again."
            )
        }
        return loaded.flatten()
    }

    private suspend fun preferencesFor(userId: String): UserPreferenceProfile =
        PreferenceLearner.build(attempt { dataSource.recentFeedback(userId) }.orEmpty())

    private suspend fun recordShown(
        userId: String,
        type: RecommendationType,
        shown: List<ScoredRecommendation<*>>
    ) {
        if (shown.isEmpty()) return
        attempt {
            dataSource.recordFeedback(userId, shown.map { event(it, type, FeedbackAction.SHOWN) })
        }
    }

    private fun event(
        recommendation: ScoredRecommendation<*>,
        type: RecommendationType,
        action: FeedbackAction,
        reason: String = ""
    ) = RecommendationFeedback(
        recommendationId = recommendation.recommendationId,
        type = type,
        itemId = recommendation.itemId,
        action = action,
        tags = recommendation.tags,
        reason = reason,
        scoreSnapshot = recommendation.score.features + (TOTAL_SCORE to recommendation.score.total),
        createdAt = clock()
    )

    private fun filtersFor(context: MealRecommendationContext): RecipeFilters {
        val preference = context.dietaryPreference.orEmpty().trim()
        val diet = when {
            preference.isBlank() || preference.equals("No preference", ignoreCase = true) -> null
            else -> preference.lowercase().replace("-", " ")
        }
        return RecipeFilters(diet = diet)
    }

    private fun noteOf(
        historyNote: String?,
        sourceNote: String?,
        response: RecommendationResponse<*>
    ): String? = listOfNotNull(
        historyNote,
        sourceNote,
        response.rationale.takeIf { response.recommendations.isEmpty() }
    ).joinToString(" ").ifBlank { null }

    /** Runs a non-essential step; failure returns null instead of failing the whole flow. */
    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val TOTAL_SCORE = "total"
    }
}
