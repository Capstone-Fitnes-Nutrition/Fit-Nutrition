package sheridan.dheripu.fitnutrition.model.recommendation

import sheridan.dheripu.fitnutrition.model.MealRecommendationContext
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.model.RecommendationResponse
import java.time.ZoneId
import kotlin.math.abs

/** A recipe with the portion the engine suggests and the nutrition of that portion. */
data class MealPick(
    val recipe: Recipe,
    val servings: Double,
    val calories: Int,
    val proteinGrams: Double,
    val carbohydratesGrams: Double,
    val fatGrams: Double
)

/**
 * Content-based meal ranking: hard dietary filters, then weighted features (calorie fit,
 * protein fit, novelty, learned affinity), then the top three with varied dish types.
 */
class MealRecommendationEngine(
    private val weights: ScoringWeights = DEFAULT_WEIGHTS,
    private val zone: ZoneId = ZoneId.systemDefault()
) : RecommendationEngine<MealRecommendationContext, Recipe, MealPick> {

    override fun recommend(
        context: MealRecommendationContext,
        candidates: List<Recipe>,
        preferences: UserPreferenceProfile,
        nowMillis: Long
    ): RecommendationResponse<ScoredRecommendation<MealPick>> {
        val targets = MealRules.targets(context, nowMillis, zone)
        val eligible = candidates
            .filterNot { it.id.toString() in preferences.dismissedItemIds }
            .filter { recipe -> MealRules.all.all { it.keep(context, recipe) } }
        if (eligible.isEmpty()) {
            return RecommendationResponse(
                recommendations = emptyList(),
                rationale = "No recipes matched your dietary needs and preferences."
            )
        }
        val ranked = eligible.map { score(context, it, targets, preferences, nowMillis) }.ranked()
        val picks = pickVaried(ranked, MAX_RESULTS, maxPerGroup = 1) {
            it.item.recipe.dishTypes.firstOrNull().orEmpty()
        }
        return RecommendationResponse(
            recommendations = picks,
            rationale = "Ranked ${eligible.size} of ${candidates.size} recipes for a " +
                "${targets.calories} kcal meal."
        )
    }

    private fun score(
        context: MealRecommendationContext,
        recipe: Recipe,
        targets: MealTargets,
        preferences: UserPreferenceProfile,
        nowMillis: Long
    ): ScoredRecommendation<MealPick> {
        val pick = portion(recipe, targets)
        val tags = (recipe.diets + recipe.dishTypes + recipe.extendedIngredients.map { it.name })
            .map(::normalizeTag)
            .distinct()
        val features = mapOf(
            CALORIE_FIT to calorieFit(pick.calories, targets.calories),
            PROTEIN_FIT to (pick.proteinGrams / targets.proteinGrams).coerceAtMost(1.0),
            NOVELTY to novelty(context, recipe, nowMillis),
            LEARNED_AFFINITY to preferences.affinityFor(tags)
        )
        return ScoredRecommendation(
            recommendationId = "meal-${recipe.id}-$nowMillis",
            itemId = recipe.id.toString(),
            item = pick,
            score = weights.combine(features),
            reasons = reasons(features, pick, targets, context.recentMeals.isNotEmpty()),
            tags = tags
        )
    }

    private fun portion(recipe: Recipe, targets: MealTargets): MealPick {
        val perServing = recipe.caloriesPerServing() ?: 0.0
        val servings = MealRules.servingsFor(perServing, targets.calories)
        return MealPick(
            recipe = recipe,
            servings = servings,
            calories = (perServing * servings).toInt(),
            proteinGrams = (recipe.nutrientAmount("protein") ?: 0.0) * servings,
            carbohydratesGrams = (recipe.nutrientAmount("carbohydrates") ?: 0.0) * servings,
            fatGrams = (recipe.nutrientAmount("fat") ?: 0.0) * servings
        )
    }

    private fun calorieFit(calories: Int, target: Int): Double =
        (1.0 - abs(calories - target).toDouble() / target).coerceAtLeast(0.0)

    private fun novelty(
        context: MealRecommendationContext,
        recipe: Recipe,
        nowMillis: Long
    ): Double {
        val lastEaten = context.recentMeals
            .filter { it.name.equals(recipe.title, ignoreCase = true) }
            .maxOfOrNull { it.loggedAtEpochMillis } ?: return 1.0
        val age = nowMillis - lastEaten
        return when {
            age < 2 * DAY_MILLIS -> 0.0
            age < 7 * DAY_MILLIS -> 0.5
            else -> 1.0
        }
    }

    private fun reasons(
        features: Map<String, Double>,
        pick: MealPick,
        targets: MealTargets,
        hasHistory: Boolean
    ): List<String> = buildList {
        if ((features[LEARNED_AFFINITY] ?: 0.0) >= 0.3) add("Similar to meals you've liked")
        if (pick.servings != 1.0) {
            add("Have ${formatServings(pick.servings)} servings to fit your meal target")
        }
        if ((features[CALORIE_FIT] ?: 0.0) >= 0.8) {
            add("About ${pick.calories} kcal, close to your ${targets.calories} kcal meal target")
        }
        if ((features[PROTEIN_FIT] ?: 0.0) >= 0.8) {
            add(
                "${pick.proteinGrams.toInt()} g protein toward your " +
                    "${targets.dailyProteinGrams} g daily target"
            )
        }
        if (hasHistory && features[NOVELTY] == 1.0) add("Something you haven't eaten recently")
    }.take(MAX_REASONS)

    private fun formatServings(servings: Double): String =
        if (servings % 1.0 == 0.0) servings.toInt().toString() else servings.toString()

    companion object {
        const val CALORIE_FIT = "calorieFit"
        const val PROTEIN_FIT = "proteinFit"
        const val NOVELTY = "novelty"
        const val LEARNED_AFFINITY = "learnedAffinity"

        val DEFAULT_WEIGHTS = ScoringWeights(
            mapOf(
                CALORIE_FIT to 0.35,
                PROTEIN_FIT to 0.25,
                NOVELTY to 0.15,
                LEARNED_AFFINITY to 0.25
            )
        )

        private const val MAX_RESULTS = 3
        private const val MAX_REASONS = 3
    }
}
