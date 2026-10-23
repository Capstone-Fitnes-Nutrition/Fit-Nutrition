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

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val source = FakeDataSource()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(userId: String? = "uid") = RecommendationViewModel(
        dataSource = source,
        clock = { NOW },
        userIdProvider = { userId }
    )

    @Test
    fun signedOutUserIsAskedToSignIn() = runTest(dispatcher) {
        val viewModel = viewModel(userId = null)

        viewModel.loadMeals()
        advanceUntilIdle()

        assertEquals(RecommendationUiState.SignedOut, viewModel.state.value)
    }

    @Test
    fun missingProfileFieldsArePassedThrough() = runTest(dispatcher) {
        source.mealContext = ContextPreparation.NeedsProfile(listOf("dietary preference"))
        val viewModel = viewModel()

        viewModel.loadMeals()
        advanceUntilIdle()

        assertEquals(
            RecommendationUiState.NeedsProfile(listOf("dietary preference")),
            viewModel.state.value
        )
    }

    @Test
    fun mealRecommendationsAreRankedAndLoggedAsShown() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.loadMeals()
        advanceUntilIdle()

        val state = viewModel.state.value as RecommendationUiState.Meals
        assertEquals(2, state.items.size)
        assertEquals(
            state.items.map { it.recommendationId },
            source.recorded.filter { it.action == FeedbackAction.SHOWN }.map { it.recommendationId }
        )
        assertEquals(NOW, source.recorded.first().createdAt)
    }

    @Test
    fun recipeFailureShowsError() = runTest(dispatcher) {
        source.recipeError = IllegalStateException("quota")
        val viewModel = viewModel()

        viewModel.loadMeals()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is RecommendationUiState.Error)
    }

    @Test
    fun feedbackReadFailureStillRecommends() = runTest(dispatcher) {
        source.feedbackError = IllegalStateException("offline")
        val viewModel = viewModel()

        viewModel.loadMeals()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is RecommendationUiState.Meals)
    }

    @Test
    fun acceptingAMealLogsThePortionAndRecordsFeedback() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val pick = (viewModel.state.value as RecommendationUiState.Meals).items.first()

        viewModel.accept(pick.recommendationId)
        advanceUntilIdle()

        val logged = source.loggedMeals.single()
        assertEquals(pick.item.calories, logged.calories)
        assertEquals(pick.item.recipe.id, logged.recipeId)
        assertTrue(source.recorded.any {
            it.action == FeedbackAction.ACCEPTED && it.recommendationId == pick.recommendationId
        })
        val state = viewModel.state.value as RecommendationUiState.Meals
        assertTrue(state.handledIds.contains(pick.recommendationId))
    }

    @Test
    fun dismissingRemovesTheItemAndRecordsTheReason() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadMeals()
        advanceUntilIdle()
        val pick = (viewModel.state.value as RecommendationUiState.Meals).items.first()

        viewModel.dismiss(pick.recommendationId, "Don't like it")
        advanceUntilIdle()

        val state = viewModel.state.value as RecommendationUiState.Meals
        assertTrue(state.items.none { it.recommendationId == pick.recommendationId })
        val event = source.recorded.single { it.action == FeedbackAction.DISMISSED }
        assertEquals("Don't like it", event.reason)
        assertEquals(pick.item.recipe.id.toString(), event.itemId)
    }

    @Test
    fun workoutUsesTodaysFitbitActivityLevel() = runTest(dispatcher) {
        source.activityLevel = "SEDENTARY"
        val viewModel = viewModel()

        viewModel.loadWorkout()
        advanceUntilIdle()

        val state = viewModel.state.value as RecommendationUiState.Workout
        assertTrue(state.items.all { it.item.exercise.equipment == "body weight" })
        assertTrue(state.note.orEmpty().contains("Fitbit"))
    }

    @Test
    fun workoutSurvivesOneBodyPartFailing() = runTest(dispatcher) {
        source.failingBodyParts = setOf("chest")
        val viewModel = viewModel()

        viewModel.loadWorkout()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is RecommendationUiState.Workout)
    }

    @Test
    fun workoutFailsWhenNoExercisesCanBeLoaded() = runTest(dispatcher) {
        source.failingBodyParts = setOf("cardio", "chest", "back", "upper legs", "shoulders",
            "upper arms", "waist")
        val viewModel = viewModel()

        viewModel.loadWorkout()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is RecommendationUiState.Error)
    }

    @Test
    fun completingAnExerciseLogsTheWorkout() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadWorkout()
        advanceUntilIdle()
        val pick = (viewModel.state.value as RecommendationUiState.Workout).items.first()

        viewModel.accept(pick.recommendationId)
        advanceUntilIdle()

        assertEquals(pick.item.exercise, source.loggedWorkouts.single().exercise)
        assertTrue(source.recorded.any { it.action == FeedbackAction.COMPLETED })
    }

    private class FakeDataSource : RecommendationDataSource {
        var mealContext: ContextPreparation<RecommendationRequest<MealRecommendationContext>> =
            ContextPreparation.Ready(
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
        var recipeError: Exception? = null
        var feedbackError: Exception? = null
        var activityLevel: String? = null
        var failingBodyParts: Set<String> = emptySet()
        val recorded = mutableListOf<RecommendationFeedback>()
        val loggedMeals = mutableListOf<NutritionLog>()
        val loggedWorkouts = mutableListOf<WorkoutItem>()

        override suspend fun mealContext(userId: String) = mealContext

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

        override suspend fun recipeCandidates(filters: RecipeFilters): List<Recipe> {
            recipeError?.let { throw it }
            return listOf(recipe(1, 600.0), recipe(2, 450.0))
        }

        override suspend fun exerciseCandidates(bodyPart: String): List<Exercise> {
            if (bodyPart in failingBodyParts) throw IllegalStateException("down")
            return listOf(
                Exercise("$bodyPart-1", "$bodyPart body weight move", bodyPart, "", emptyList(),
                    "body weight"),
                Exercise("$bodyPart-2", "$bodyPart dumbbell move", bodyPart, "", emptyList(),
                    "dumbbell")
            )
        }

        override suspend fun latestActivityLevel(): String? = activityLevel

        override suspend fun recentFeedback(userId: String): List<RecommendationFeedback> {
            feedbackError?.let { throw it }
            return emptyList()
        }

        override suspend fun recordFeedback(userId: String, events: List<RecommendationFeedback>) {
            recorded += events
        }

        override suspend fun logMeal(userId: String, log: NutritionLog) {
            loggedMeals += log
        }

        override suspend fun logWorkout(userId: String, item: WorkoutItem) {
            loggedWorkouts += item
        }

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
                listOf(Nutrient("Calories", calories, "kcal"), Nutrient("Protein", 30.0, "g"))
            ),
            diets = emptyList(),
            dishTypes = listOf("main course")
        )
    }

    companion object {
        private const val NOW = 1_792_756_800_000L
    }
}
