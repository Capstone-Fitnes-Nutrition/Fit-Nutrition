package sheridan.dheripu.fitnutrition.model.recommendation

import sheridan.dheripu.fitnutrition.model.MealRecommendationContext
import sheridan.dheripu.fitnutrition.model.Recipe
import java.time.Instant
import java.time.ZoneId

/** Hard meal rules and per-meal targets (business rules BR1, BR2 and BR3). */
internal object MealRules {
    private val restrictionTerms = mapOf(
        "dairy" to listOf("milk", "cheese", "butter", "cream", "yogurt"),
        "gluten" to listOf("wheat", "flour", "bread", "pasta", "barley", "rye", "noodle"),
        "nut" to listOf("almond", "walnut", "cashew", "pecan", "pistachio", "hazelnut"),
        "tree nut" to listOf("almond", "walnut", "cashew", "pecan", "pistachio", "hazelnut"),
        "shellfish" to listOf("shrimp", "prawn", "crab", "lobster")
    )

    /** BR2: a recipe must not contain anything the user is allergic to or avoids. */
    val respectsRestrictions = CandidateFilter<MealRecommendationContext, Recipe> { context, recipe ->
        val text = (listOf(recipe.title) + recipe.extendedIngredients.map { it.name })
            .joinToString(" ")
            .lowercase()
        context.dietaryRestrictions.filter(String::isNotBlank).none { restriction ->
            val key = singular(restriction)
            (restrictionTerms[key] ?: listOf(key)).any { term -> text.contains(term) }
        }
    }

    /** BR2: a recipe must fit the user's dietary preference. Unknown preferences pass. */
    val matchesPreference = CandidateFilter<MealRecommendationContext, Recipe> { context, recipe ->
        val diets = recipe.diets.map { it.lowercase() }
        fun hasAny(vararg labels: String) = diets.any { diet -> labels.any { diet.contains(it) } }
        when (context.dietaryPreference.orEmpty().lowercase().replace("-", " ").trim()) {
            "vegan" -> hasAny("vegan")
            "vegetarian" -> hasAny("vegetarian", "vegan")
            "pescatarian" -> hasAny("pescatarian", "vegetarian", "vegan")
            "gluten free" -> hasAny("gluten free")
            else -> true
        }
    }

    /** Recipes without calorie data cannot be portioned, so they are skipped. */
    val hasNutrition = CandidateFilter<MealRecommendationContext, Recipe> { _, recipe ->
        (recipe.caloriesPerServing() ?: 0.0) > 0.0
    }

    val all = listOf(hasNutrition, matchesPreference, respectsRestrictions)

    /**
     * BR1: the calorie and protein target for the next meal. Uses the profile targets, or a
     * goal-based default, and never aims above what is left of today's calories.
     */
    fun targets(context: MealRecommendationContext, nowMillis: Long, zone: ZoneId): MealTargets {
        val goal = context.fitnessGoal.orEmpty().lowercase()
        val dailyCalories = context.dailyCalorieTarget ?: when {
            goal.contains("loss") -> 1800
            goal.contains("gain") -> 2600
            else -> 2200
        }
        val dailyProtein = context.dailyProteinTarget ?: when {
            goal.contains("gain") -> 150
            goal.contains("loss") -> 120
            else -> 90
        }
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val eatenToday = context.recentMeals
            .filter { Instant.ofEpochMilli(it.loggedAtEpochMillis).atZone(zone).toLocalDate() == today }
            .sumOf { it.calories }
        val perMeal = dailyCalories / MEALS_PER_DAY
        val remaining = dailyCalories - eatenToday
        return MealTargets(
            calories = if (remaining >= perMeal) perMeal else maxOf(remaining, MIN_MEAL_CALORIES),
            proteinGrams = dailyProtein.toDouble() / MEALS_PER_DAY,
            dailyProteinGrams = dailyProtein
        )
    }

    /** BR3: servings that bring the recipe closest to the target, in half-serving steps. */
    fun servingsFor(caloriesPerServing: Double, targetCalories: Int): Double {
        val halves = Math.round(targetCalories / caloriesPerServing * 2)
        return (halves / 2.0).coerceIn(MIN_SERVINGS, MAX_SERVINGS)
    }

    private const val MEALS_PER_DAY = 3
    private const val MIN_MEAL_CALORIES = 200
    private const val MIN_SERVINGS = 0.5
    private const val MAX_SERVINGS = 2.0
}

internal data class MealTargets(
    val calories: Int,
    val proteinGrams: Double,
    val dailyProteinGrams: Int
)

internal fun Recipe.caloriesPerServing(): Double? = nutrientAmount("calories")

internal fun Recipe.nutrientAmount(name: String): Double? =
    nutrition?.nutrients?.firstOrNull { it.name.equals(name, ignoreCase = true) }?.amount
