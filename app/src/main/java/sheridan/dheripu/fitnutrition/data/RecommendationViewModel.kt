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
import sheridan.dheripu.fitnutrition.model.recommendation.MealRules
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
import java.time.ZoneId

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

    /**
     * Items dismissed in this session, with when it happened.
     *
     * Reason: the feedback log is the durable record, but writing it is a round trip that may
     * still be in flight — or may have failed — when the user hits refresh. Suppressing locally
     * as well is what makes a dismissal stick straight away instead of handing back the
     * suggestion the user just turned down.
     */
    private val sessionDismissals = mutableMapOf<String, Long>()

    /** Which page of recipes to ask for next, so refreshing widens the pool. */
    private var mealPage = 0

    fun loadMeals() = load { userId, previous ->
        when (val preparation = dataSource.mealContext(userId)) {
            is ContextPreparation.NeedsProfile ->
                RecommendationUiState.NeedsProfile(preparation.missingFields)
            is ContextPreparation.Ready -> {
                val context = preparation.value.context
                val candidates = dataSource.recipeCandidates(filtersFor(context))
                mealPage = (mealPage + 1) % RECIPE_PAGES
                val response = mealEngine.recommend(
                    context, candidates, preferencesFor(userId), clock()
                )
                recordShown(userId, RecommendationType.MEAL, response.recommendations)
                RecommendationUiState.Meals(
                    items = response.recommendations,
                    note = noteOf(context.historyNote, null, response),
                    dismissed = (previous as? RecommendationUiState.Meals)?.dismissed.orEmpty(),
                    reserves = response.reserves
                )
            }
        }
    }

    fun loadWorkout() = load { userId, previous ->
        when (val preparation = dataSource.workoutContext(userId)) {
            is ContextPreparation.NeedsProfile ->
                RecommendationUiState.NeedsProfile(preparation.missingFields)
            is ContextPreparation.Ready -> {
                val profileContext = preparation.value.context
                val syncedLevel = attempt { dataSource.latestActivityLevel() }
                val context = profileContext.copy(
                    activityLevel = syncedLevel ?: profileContext.activityLevel
                )
                val candidates = exercisesFor(WorkoutTargetPlanner.bodyPartsFor(context))
                val response = workoutEngine.recommend(
                    context, candidates, preferencesFor(userId), clock()
                )
                recordShown(userId, RecommendationType.WORKOUT, response.recommendations)
                val syncedNote = syncedLevel?.let { "Using today's synced activity level ($it)." }
                RecommendationUiState.Workout(
                    items = response.recommendations,
                    note = noteOf(context.historyNote, syncedNote, response),
                    dismissed = (previous as? RecommendationUiState.Workout)?.dismissed
                        .orEmpty(),
                    reserves = response.reserves
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

    /**
     * Hides the suggestion, remembers it so it can be restored, and records the negative
     * feedback with an optional reason.
     */
    fun dismiss(recommendationId: String, reason: String = "") {
        val userId = userIdProvider() ?: return
        // Reason: logging that a replacement was shown is a suspend call, so the swap is
        // decided here and recorded with the dismissal in the one coroutine below.
        var shown: ScoredRecommendation<*>? = null
        val (dismissed, type) = when (val current = _state.value) {
            is RecommendationUiState.Meals -> {
                val item = current.items.firstOrNull { it.recommendationId == recommendationId }
                    ?: return
                // Reason: a dismissal used to leave a gap, and enough of them emptied the card
                // with nothing to refill it. The engine already ranked more than it showed, so
                // the next best one takes the freed slot without another request.
                val replacement = current.reserves.firstOrNull()
                _state.value = current.copy(
                    items = current.items - item + listOfNotNull(replacement),
                    reserves = current.reserves.drop(1),
                    dismissed = listOf(item) + current.dismissed,
                    undoableId = recommendationId,
                    notice = refilledNotice(replacement?.item?.recipe?.title, current.reserves)
                )
                shown = replacement
                item to RecommendationType.MEAL
            }
            is RecommendationUiState.Workout -> {
                val item = current.items.firstOrNull { it.recommendationId == recommendationId }
                    ?: return
                val replacement = current.reserves.firstOrNull()
                _state.value = current.copy(
                    items = current.items - item + listOfNotNull(replacement),
                    reserves = current.reserves.drop(1),
                    dismissed = listOf(item) + current.dismissed,
                    undoableId = recommendationId,
                    notice = refilledNotice(
                        replacement?.item?.exercise?.name?.replaceFirstChar(Char::uppercase),
                        current.reserves
                    )
                )
                shown = replacement
                item to RecommendationType.WORKOUT
            }
            else -> return
        }
        sessionDismissals[dismissed.itemId] = clock()
        val replacement = shown
        viewModelScope.launch {
            attempt {
                dataSource.recordFeedback(
                    userId, listOf(event(dismissed, type, FeedbackAction.DISMISSED, reason))
                )
            }
            if (replacement != null) attempt { recordShown(userId, type, listOf(replacement)) }
        }
    }

    /**
     * What the card says after a dismissal: what took the freed slot, or that the engine has
     * run out of ranked candidates and a refresh is what brings more.
     */
    private fun refilledNotice(replacementName: String?, reservesBefore: List<*>): String? =
        when {
            replacementName != null -> "Swapped in $replacementName."
            reservesBefore.isEmpty() -> "That was the last ranked suggestion. Refresh for more."
            else -> null
        }

    /**
     * Puts a dismissed suggestion back in the list and clears its suppression, so the next
     * refresh can offer it again. Logged as [FeedbackAction.RESTORED] so the undo outlives this
     * session and cancels the dismissal's negative signal.
     */
    fun restore(recommendationId: String) {
        val userId = userIdProvider() ?: return
        val (restored, type) = when (val current = _state.value) {
            is RecommendationUiState.Meals -> {
                val item = current.dismissed
                    .firstOrNull { it.recommendationId == recommendationId } ?: return
                _state.value = current.copy(
                    items = (current.items + item).sortedByDescending { it.score.total },
                    dismissed = current.dismissed - item,
                    undoableId = current.undoableId.takeIf { it != recommendationId }
                )
                item to RecommendationType.MEAL
            }
            is RecommendationUiState.Workout -> {
                val item = current.dismissed
                    .firstOrNull { it.recommendationId == recommendationId } ?: return
                _state.value = current.copy(
                    items = (current.items + item).sortedByDescending { it.score.total },
                    dismissed = current.dismissed - item,
                    undoableId = current.undoableId.takeIf { it != recommendationId }
                )
                item to RecommendationType.WORKOUT
            }
            else -> return
        }
        sessionDismissals.remove(restored.itemId)
        viewModelScope.launch {
            attempt {
                dataSource.recordFeedback(
                    userId, listOf(event(restored, type, FeedbackAction.RESTORED))
                )
            }
        }
    }

    /** Drops the undo offer without restoring anything. */
    fun clearUndo() {
        _state.update { state ->
            when (state) {
                is RecommendationUiState.Meals -> state.copy(undoableId = null)
                is RecommendationUiState.Workout -> state.copy(undoableId = null)
                else -> state
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

    /**
     * Runs a load, handing the block the state it replaces. Reason: the state is set to Loading
     * before the block runs, so a block that needs what was on screen — the dismissed list it
     * must carry over — cannot read it back off the flow.
     */
    private fun load(block: suspend (String, RecommendationUiState) -> RecommendationUiState) {
        val userId = userIdProvider()
        if (userId == null) {
            _state.value = RecommendationUiState.SignedOut
            return
        }
        val previous = _state.value
        _state.value = RecommendationUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                block(userId, previous)
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

    /**
     * The learned profile, widened by the dismissals made in this session. Expired session
     * entries are dropped first, so the local set suppresses for the same window as the log.
     */
    private suspend fun preferencesFor(userId: String): UserPreferenceProfile {
        val now = clock()
        sessionDismissals.entries
            .removeAll { now - it.value >= PreferenceLearner.DISMISSAL_WINDOW_MILLIS }
        val learned = PreferenceLearner.build(
            attempt { dataSource.recentFeedback(userId) }.orEmpty(), now
        )
        return learned.copy(
            dismissedItemIds = learned.dismissedItemIds + sessionDismissals.keys
        )
    }

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

    /**
     * What to ask Spoonacular for.
     *
     * Reason: only the diet was being sent, so the restrictions and the calorie target were
     * enforced locally over whatever single page came back. A page of dairy recipes for a
     * dairy-free user left nothing to rank and the card said no recipes matched. Excluding
     * server side keeps the page usable; the local filters stay as the backstop.
     *
     * The offset advances per load so a refresh sees candidates it has not already ranked,
     * rather than re-ranking one page and returning the same three suggestions.
     */
    private fun filtersFor(context: MealRecommendationContext): RecipeFilters {
        val preference = context.dietaryPreference.orEmpty().trim()
        val diet = when {
            preference.isBlank() ||
                preference.equals(MealRules.NO_PREFERENCE, ignoreCase = true) -> null
            else -> preference.lowercase().replace("-", " ")
        }
        val mealCalories = MealRules.targets(context, clock(), ZoneId.systemDefault()).calories
        return RecipeFilters(
            maxCalories = mealCalories * CALORIE_HEADROOM,
            diet = diet,
            intolerances = MealRules.spoonacularIntolerances(context.dietaryRestrictions),
            offset = mealPage * RECIPE_PAGE_SIZE
        )
    }

    private fun noteOf(
        historyNote: String?,
        sourceNote: String?,
        response: RecommendationResponse<*>
    ): String? = listOfNotNull(
        historyNote,
        sourceNote,
        response.advisory,
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

        /** A meal target is a floor, not a ceiling; allow headroom so the pool is not starved. */
        const val CALORIE_HEADROOM = 2
        const val RECIPE_PAGE_SIZE = 20
        const val RECIPE_PAGES = 5
    }
}
