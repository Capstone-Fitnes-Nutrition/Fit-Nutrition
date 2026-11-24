package sheridan.dheripu.fitnutrition.model.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sheridan.dheripu.fitnutrition.model.MealRecommendationContext

class MealRulesTest {

    private fun context(
        preference: String? = "No preference",
        restrictions: List<String> = emptyList()
    ) = MealRecommendationContext(
        fitnessGoal = "Weight Loss",
        dailyCalorieTarget = 1800,
        dailyProteinTarget = 120,
        dietaryPreference = preference,
        dietaryRestrictions = restrictions,
        recentMeals = emptyList()
    )

    @Test
    fun `restrictions map onto the vocabulary Spoonacular accepts`() {
        val mapped = MealRules.spoonacularIntolerances(
            listOf("Dairy", "peanuts", "Tree Nuts", "shellfish")
        )

        assertEquals(listOf("Dairy", "Peanut", "Tree Nut", "Shellfish"), mapped)
    }

    @Test
    fun `everyday wording for a restriction still maps`() {
        assertEquals(listOf("Dairy"), MealRules.spoonacularIntolerances(listOf("milk")))
        assertEquals(listOf("Seafood"), MealRules.spoonacularIntolerances(listOf("fish")))
        assertEquals(listOf("Soy"), MealRules.spoonacularIntolerances(listOf("soya")))
    }

    @Test
    fun `a restriction Spoonacular has no term for is left to the local filter`() {
        assertEquals(emptyList<String>(), MealRules.spoonacularIntolerances(listOf("coriander")))
    }

    @Test
    fun `blank and duplicate restrictions are dropped`() {
        val mapped = MealRules.spoonacularIntolerances(listOf("Dairy", "", "  ", "dairy"))

        assertEquals(listOf("Dairy"), mapped)
    }

    @Test
    fun `the advisory names the preference and the restrictions that excluded recipes`() {
        val message = MealRules.dietaryLockMessage(
            context(preference = "Vegan", restrictions = listOf("Peanut")),
            4
        )

        assertTrue(message, message.contains("4 recipes"))
        assertTrue(message, message.contains("Vegan"))
        assertTrue(message, message.contains("Peanut"))
    }

    @Test
    fun `with no dietary rules the advisory blames the missing calorie data`() {
        val message = MealRules.dietaryLockMessage(context(), 1)

        assertEquals("1 recipe had no calorie data and was skipped.", message)
    }
}
