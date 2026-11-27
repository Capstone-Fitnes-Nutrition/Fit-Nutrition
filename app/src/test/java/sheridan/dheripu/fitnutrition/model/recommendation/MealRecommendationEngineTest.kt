package sheridan.dheripu.fitnutrition.model.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sheridan.dheripu.fitnutrition.model.Ingredient
import sheridan.dheripu.fitnutrition.model.MealHistoryItem
import sheridan.dheripu.fitnutrition.model.MealRecommendationContext
import sheridan.dheripu.fitnutrition.model.Nutrient
import sheridan.dheripu.fitnutrition.model.Nutrition
import sheridan.dheripu.fitnutrition.model.Recipe
import java.time.ZoneOffset

class MealRecommendationEngineTest {
    private val engine = MealRecommendationEngine(zone = ZoneOffset.UTC)

    @Test
    fun prefersTheRecipeOnTheProteinTargetOverOneCarryingFarMore() {
        // Reason: protein fit used to be the ratio capped at 1, so both of these scored a flat
        // 1.0 and the order fell to the id tiebreak. Only the calorie fit differs otherwise,
        // and these two are equally far from the target.
        val candidates = listOf(
            recipe(1, "Protein Overload", calories = 700.0, protein = 160.0),
            recipe(2, "On Target", calories = 700.0, protein = 42.0)
        )

        val result = engine.recommend(context(), candidates, UserPreferenceProfile(), NOW)

        assertEquals(2, result.recommendations.first().item.recipe.id)
    }

    @Test
    fun ranksAFatHeavyPlateBelowABalancedOneOfTheSameCalories() {
        val candidates = listOf(
            // 85% of its energy from fat, and the same calories and protein as the other.
            recipe(1, "Buttered Everything", calories = 700.0, protein = 30.0,
                fat = 66.0, carbohydrates = 5.0),
            recipe(2, "Balanced Plate", calories = 700.0, protein = 30.0,
                fat = 20.0, carbohydrates = 80.0)
        )

        val result = engine.recommend(context(), candidates, UserPreferenceProfile(), NOW)

        assertEquals(2, result.recommendations.first().item.recipe.id)
        assertTrue(result.recommendations.first().reasons.any { it.contains("Balanced macros") })
    }

    @Test
    fun scoresARecipeWithNoMacroDataNeutrallyRatherThanAsFatHeavy() {
        val withMacros = recipe(1, "Balanced Plate", calories = 700.0, protein = 42.0)
        val noMacros = withMacros.copy(
            id = 2,
            title = "Unknown Macros",
            nutrition = Nutrition(
                listOf(Nutrient("Calories", 700.0, "kcal"), Nutrient("Protein", 42.0, "g"))
            )
        )

        val result = engine.recommend(
            context(), listOf(withMacros, noMacros), UserPreferenceProfile(), NOW
        )

        // Both are offered; the one with known balanced macros simply ranks first.
        assertEquals(listOf(1, 2), result.recommendations.map { it.item.recipe.id })
    }

    @Test
    fun excludesRecipesThatBreakRestrictionsOrPreference() {
        val context = context(preference = "Vegetarian", restrictions = listOf("Peanuts"))
        val candidates = listOf(
            recipe(1, "Peanut Noodles", diets = listOf("vegetarian"),
                ingredients = listOf("noodles", "peanut butter")),
            recipe(2, "Grilled Chicken Salad", ingredients = listOf("chicken breast")),
            recipe(3, "Veggie Bowl", diets = listOf("lacto ovo vegetarian"))
        )

        val result = engine.recommend(context, candidates, UserPreferenceProfile(), NOW)

        assertEquals(listOf(3), result.recommendations.map { it.item.recipe.id })
    }

    @Test
    fun ranksRecipeClosestToMealTargetFirst() {
        val candidates = listOf(
            recipe(1, "Huge Lasagna", calories = 2000.0, protein = 40.0),
            recipe(2, "Balanced Plate", calories = 600.0, protein = 40.0)
        )

        val result = engine.recommend(context(), candidates, UserPreferenceProfile(), NOW)

        assertEquals(2, result.recommendations.first().item.recipe.id)
    }

    @Test
    fun suggestsLargerPortionWhenOneServingIsTooSmall() {
        val result = engine.recommend(
            context(),
            listOf(recipe(1, "Light Soup", calories = 300.0, protein = 20.0)),
            UserPreferenceProfile(),
            NOW
        )

        val pick = result.recommendations.single()
        assertEquals(2.0, pick.item.servings, 0.0001)
        assertEquals(600, pick.item.calories)
    }

    @Test
    fun aimsAtRemainingCaloriesForToday() {
        val eatenToday = MealHistoryItem("Breakfast", 1500, 30.0, NOW - HOUR)

        val result = engine.recommend(
            context(recentMeals = listOf(eatenToday)),
            listOf(recipe(1, "Balanced Plate", calories = 600.0)),
            UserPreferenceProfile(),
            NOW
        )

        assertEquals(0.5, result.recommendations.single().item.servings, 0.0001)
    }

    @Test
    fun ranksRecentlyEatenMealsLower() {
        val eatenYesterday = MealHistoryItem("Balanced Plate", 600, 40.0, NOW - DAY)

        val result = engine.recommend(
            context(recentMeals = listOf(eatenYesterday)),
            listOf(recipe(1, "Balanced Plate"), recipe(2, "Other Plate")),
            UserPreferenceProfile(),
            NOW
        )

        assertEquals(2, result.recommendations.first().item.recipe.id)
    }

    @Test
    fun learnedAffinityLiftsLikedDishTypes() {
        val preferences = UserPreferenceProfile(affinities = mapOf("salad" to 0.8))

        val result = engine.recommend(
            context(),
            listOf(recipe(1, "Pasta", dishTypes = listOf("pasta")),
                recipe(2, "Salad", dishTypes = listOf("salad"))),
            preferences,
            NOW
        )

        val top = result.recommendations.first()
        assertEquals(2, top.item.recipe.id)
        assertTrue(top.reasons.any { it.contains("past picks") })
    }

    @Test
    fun dropsRecentlyDismissedRecipes() {
        val preferences = UserPreferenceProfile(dismissedItemIds = setOf("1"))

        val result = engine.recommend(
            context(), listOf(recipe(1, "A"), recipe(2, "B")), preferences, NOW
        )

        assertEquals(listOf(2), result.recommendations.map { it.item.recipe.id })
    }

    @Test
    fun explainsWhenNothingQualifies() {
        val noNutrition = recipe(1, "Mystery").copy(nutrition = null)

        val result = engine.recommend(context(), listOf(noNutrition), UserPreferenceProfile(), NOW)

        assertTrue(result.recommendations.isEmpty())
        // The rationale now names what was dropped and why, rather than only that nothing matched.
        assertEquals("1 recipe had no calorie data and was skipped.", result.rationale)
    }

    @Test
    fun returnsAtMostThreeWithStableIdsAndTags() {
        val candidates = (1..5).map { recipe(it, "Recipe $it") }

        val result = engine.recommend(context(), candidates, UserPreferenceProfile(), NOW)

        assertEquals(3, result.recommendations.size)
        val first = result.recommendations.first()
        assertEquals("meal-${first.item.recipe.id}-$NOW", first.recommendationId)
        assertTrue(first.tags.contains("main course"))
    }

    private fun context(
        preference: String = "No preference",
        restrictions: List<String> = emptyList(),
        recentMeals: List<MealHistoryItem> = emptyList()
    ) = MealRecommendationContext(
        fitnessGoal = "Weight Loss",
        dailyCalorieTarget = 1800,
        dailyProteinTarget = 120,
        dietaryPreference = preference,
        dietaryRestrictions = restrictions,
        recentMeals = recentMeals
    )

    @Test
    fun `a repeat is spotted by recipe id even when the logged name differs`() {
        val eatenYesterday = MealHistoryItem(
            name = "leftover pasta",
            calories = 600,
            proteinGrams = 40.0,
            loggedAtEpochMillis = NOW - DAY,
            recipeId = 1
        )
        val candidates = listOf(
            recipe(1, "Creamy Tomato Pasta"),
            recipe(2, "Creamy Tomato Pasta Variant")
        )

        val result = engine.recommend(
            context(recentMeals = listOf(eatenYesterday)), candidates,
            UserPreferenceProfile(), NOW
        )

        // Recipe 1 was eaten yesterday under another name, so the one not eaten ranks first.
        assertEquals(2, result.recommendations.first().item.recipe.id)
    }

    @Test
    fun `recipes with no dish type are not collapsed into a single slot`() {
        // Reason: grouping on the first dish type put every untyped recipe in one "" group, and
        // the cap of one per group threw all but one of them away.
        val candidates = (1..3).map { recipe(it, "Recipe $it", dishTypes = emptyList()) }

        val result = engine.recommend(context(), candidates, UserPreferenceProfile(), NOW)

        assertEquals(3, result.recommendations.size)
    }

    @Test
    fun `the advisory says how many recipes the dietary rules removed`() {
        val context = context(preference = "Vegan", restrictions = listOf("Peanuts"))
        val candidates = listOf(
            recipe(1, "Peanut Stew", diets = listOf("vegan"), ingredients = listOf("peanut")),
            recipe(2, "Chicken Plate", ingredients = listOf("chicken")),
            recipe(3, "Tofu Bowl", diets = listOf("vegan"))
        )

        val result = engine.recommend(context, candidates, UserPreferenceProfile(), NOW)

        assertEquals(listOf(3), result.recommendations.map { it.item.recipe.id })
        val advisory = result.advisory
        assertTrue("advisory was null", advisory != null)
        assertTrue(advisory.orEmpty(), advisory.orEmpty().contains("2 recipes"))
    }

    @Test
    fun `a duplicate recipe in the candidate page is only offered once`() {
        val candidates = listOf(recipe(1, "Soup"), recipe(1, "Soup"), recipe(2, "Stew"))

        val result = engine.recommend(context(), candidates, UserPreferenceProfile(), NOW)

        assertEquals(listOf(1, 2), result.recommendations.map { it.item.recipe.id }.sorted())
    }

    private fun recipe(
        id: Int,
        title: String,
        calories: Double = 600.0,
        protein: Double = 40.0,
        fat: Double = 20.0,
        carbohydrates: Double = 60.0,
        diets: List<String> = emptyList(),
        ingredients: List<String> = emptyList(),
        dishTypes: List<String> = listOf("main course")
    ) = Recipe(
        id = id,
        title = title,
        image = "",
        readyInMinutes = 20,
        servings = 1,
        sourceUrl = null,
        summary = "",
        extendedIngredients = ingredients.mapIndexed { index, name ->
            Ingredient(index, name, 1.0, "g", null)
        },
        nutrition = Nutrition(
            listOf(
                Nutrient("Calories", calories, "kcal"),
                Nutrient("Protein", protein, "g"),
                Nutrient("Fat", fat, "g"),
                Nutrient("Carbohydrates", carbohydrates, "g")
            )
        ),
        diets = diets,
        dishTypes = dishTypes
    )

    companion object {
        // 2026-10-23T12:00:00Z
        private const val NOW = 1_792_756_800_000L
        private const val HOUR = 3_600_000L
        private const val DAY = 24 * HOUR
    }
}
