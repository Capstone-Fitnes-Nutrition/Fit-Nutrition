package sheridan.dheripu.fitnutrition.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sheridan.dheripu.fitnutrition.model.ContextPreparation
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.MealRecommendationContext
import sheridan.dheripu.fitnutrition.model.Nutrient
import sheridan.dheripu.fitnutrition.model.Nutrition
import sheridan.dheripu.fitnutrition.model.NutritionLog
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.model.RecipeFilters
import sheridan.dheripu.fitnutrition.model.RecommendationRequest
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.model.WorkoutRecommendationContext
import sheridan.dheripu.fitnutrition.model.recommendation.FeedbackAction
import sheridan.dheripu.fitnutrition.model.recommendation.RecommendationFeedback
import sheridan.dheripu.fitnutrition.repository.RecommendationDataSource

/**
 * A dismissal has to stick even when the feedback write is slow or fails, and the user has to
 * be able to take one back. These are the cases that made dismissals look broken: the suggestion
 * came straight back on the next refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationDismissalTest {

    private val dispatcher = StandardTestDispatcher()
    private val source = FakeSource()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = RecommendationViewModel(
        dataSource = source,
        clock = { now },
        userIdProvider = { "uid" }
    )

    private var now = START

    private fun meals(viewModel: RecommendationViewModel) =
        viewModel.state.value as RecommendationUiState.Meals

    @Test
    fun `a dismissed meal does not come back on the next refresh`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val dismissedId = meals(viewModel).items.first().item.recipe.id.toString()

        viewModel.dismiss(meals(viewModel).items.first().recommendationId)
        advanceUntilIdle()
        viewModel.loadMeals()
        advanceUntilIdle()

        assertTrue(
            meals(viewModel).items.none { it.item.recipe.id.toString() == dismissedId }
        )
    }

    @Test
    fun `a dismissal sticks even when the feedback write fails`() = runTest(dispatcher) {
        // Reason: suppression used to depend entirely on reading the dismissal back out of the
        // feedback log, so a failed write silently handed the suggestion straight back.
        source.feedbackError = IllegalStateException("offline")
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val dismissedId = meals(viewModel).items.first().item.recipe.id.toString()

        viewModel.dismiss(meals(viewModel).items.first().recommendationId)
        advanceUntilIdle()
        viewModel.loadMeals()
        advanceUntilIdle()

        assertTrue(
            meals(viewModel).items.none { it.item.recipe.id.toString() == dismissedId }
        )
    }

    @Test
    fun `dismissing offers an undo and keeps the item for restoring`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val target = meals(viewModel).items.first()

        viewModel.dismiss(target.recommendationId)
        advanceUntilIdle()

        val state = meals(viewModel)
        assertEquals(target.recommendationId, state.undoableId)
        assertEquals(listOf(target), state.dismissed)
        assertTrue(state.items.none { it.recommendationId == target.recommendationId })
    }

    @Test
    fun `restoring puts the suggestion back and logs the undo`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val target = meals(viewModel).items.first()
        viewModel.dismiss(target.recommendationId)
        advanceUntilIdle()

        viewModel.restore(target.recommendationId)
        advanceUntilIdle()

        val state = meals(viewModel)
        assertTrue(state.items.any { it.recommendationId == target.recommendationId })
        assertTrue(state.dismissed.isEmpty())
        assertNull(state.undoableId)
        assertTrue(
            source.recorded.any {
                it.action == FeedbackAction.RESTORED && it.itemId == target.itemId
            }
        )
    }

    @Test
    fun `a restored suggestion is offered again on the next refresh`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val target = meals(viewModel).items.first()
        val itemId = target.itemId

        viewModel.dismiss(target.recommendationId)
        advanceUntilIdle()
        viewModel.restore(target.recommendationId)
        advanceUntilIdle()
        viewModel.loadMeals()
        advanceUntilIdle()

        assertTrue(meals(viewModel).items.any { it.itemId == itemId })
    }

    @Test
    fun `the dismissed list survives a refresh so it stays restorable`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val target = meals(viewModel).items.first()

        viewModel.dismiss(target.recommendationId)
        advanceUntilIdle()
        viewModel.loadMeals()
        advanceUntilIdle()

        assertEquals(listOf(target.itemId), meals(viewModel).dismissed.map { it.itemId })
    }

    @Test
    fun `a dismissal stops hiding the suggestion once the window passes`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val target = meals(viewModel).items.first()
        viewModel.dismiss(target.recommendationId)
        advanceUntilIdle()

        now = START + WINDOW + 1_000
        viewModel.loadMeals()
        advanceUntilIdle()

        assertTrue(meals(viewModel).items.any { it.itemId == target.itemId })
    }

    @Test
    fun `clearing the undo leaves the suggestion dismissed`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val target = meals(viewModel).items.first()
        viewModel.dismiss(target.recommendationId)
        advanceUntilIdle()

        viewModel.clearUndo()

        val state = meals(viewModel)
        assertNull(state.undoableId)
        assertEquals(listOf(target.itemId), state.dismissed.map { it.itemId })
        assertFalse(state.items.any { it.recommendationId == target.recommendationId })
    }

    @Test
    fun `a dismissed exercise does not come back on the next refresh`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadWorkout()
        advanceUntilIdle()
        val state = viewModel.state.value as RecommendationUiState.Workout
        val target = state.items.first()

        viewModel.dismiss(target.recommendationId)
        advanceUntilIdle()
        viewModel.loadWorkout()
        advanceUntilIdle()

        val refreshed = viewModel.state.value as RecommendationUiState.Workout
        assertTrue(refreshed.items.none { it.itemId == target.itemId })
        assertEquals(listOf(target.itemId), refreshed.dismissed.map { it.itemId })
    }

    private class FakeSource : RecommendationDataSource {
        var feedbackError: Exception? = null
        val recorded = mutableListOf<RecommendationFeedback>()

        override suspend fun mealContext(userId: String) = ContextPreparation.Ready(
            RecommendationRequest(
                recommendationType = "meal",
                context = MealRecommendationContext(
                    fitnessGoal = "Weight Loss",
                    dailyCalorieTarget = 1800,
                    dailyProteinTarget = 120,
                    dietaryPreference = "No preference",
                    dietaryRestrictions = emptyList(),
                    recentMeals = emptyList()
                ),
                instruction = ""
            )
        )

        override suspend fun workoutContext(userId: String) = ContextPreparation.Ready(
            RecommendationRequest(
                recommendationType = "workout",
                context = WorkoutRecommendationContext(
                    fitnessGoal = "General Fitness",
                    activityLevel = "Very Active",
                    availableEquipment = listOf("Dumbbells"),
                    recentWorkouts = emptyList()
                ),
                instruction = ""
            )
        )

        // Reason: two candidates against the engine's top-three cut, so every eligible recipe is
        // shown. A longer list would let a restored recipe be ranked out rather than suppressed,
        // and the test could not tell the two apart.
        override suspend fun recipeCandidates(filters: RecipeFilters): List<Recipe> =
            listOf(recipe(1, 600.0), recipe(2, 450.0))

        override suspend fun exerciseCandidates(bodyPart: String): List<Exercise> = listOf(
            Exercise("$bodyPart-1", "$bodyPart body weight move", bodyPart, "", emptyList(),
                "body weight"),
            Exercise("$bodyPart-2", "$bodyPart dumbbell move", bodyPart, "", emptyList(),
                "dumbbell")
        )

        override suspend fun latestActivityLevel(): String? = null

        /** Mirrors Firestore: the events written are the events read back. */
        override suspend fun recentFeedback(userId: String): List<RecommendationFeedback> =
            recorded.toList()

        override suspend fun recordFeedback(
            userId: String,
            events: List<RecommendationFeedback>
        ) {
            feedbackError?.let { throw it }
            recorded += events
        }

        override suspend fun logMeal(userId: String, log: NutritionLog) = Unit

        override suspend fun logWorkout(userId: String, item: WorkoutItem) = Unit

        private fun recipe(id: Int, calories: Double) = Recipe(
            id = id,
            title = "Recipe $id",
            image = "",
            readyInMinutes = 20,
            servings = 1,
            sourceUrl = null,
            summary = "",
            extendedIngredients = emptyList(),
            nutrition = Nutrition(
                listOf(
                    Nutrient("Calories", calories, "kcal"),
                    Nutrient("Protein", 30.0, "g")
                )
            ),
            diets = emptyList(),
            dishTypes = listOf("dish $id")
        )
    }

    private companion object {
        const val START = 1_792_944_000_000L
        val WINDOW =
            sheridan.dheripu.fitnutrition.model.recommendation.PreferenceLearner
                .DISMISSAL_WINDOW_MILLIS
    }
}
