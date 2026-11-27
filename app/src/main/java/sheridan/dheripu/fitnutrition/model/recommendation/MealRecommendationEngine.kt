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
        val offered = candidates
            .distinctBy { it.id }
            .filterNot { it.id.toString() in preferences.dismissedItemIds }
        val eligible = offered.filter { recipe -> MealRules.all.all { it.keep(context, recipe) } }
        // Reason: the workout card already names what its filters removed. Saying the same here
        // is what separates "your profile excluded most of this page" from a broken search.
        val advisory = (offered.size - eligible.size)
            .takeIf { it > 0 }
            ?.let { MealRules.dietaryLockMessage(context, it) }
        if (eligible.isEmpty()) {
            return RecommendationResponse(
                recommendations = emptyList(),
                rationale = advisory
                    ?: "No recipes matched your dietary needs and preferences."
            )
        }
        val ranked = eligible.map { score(context, it, targets, preferences, nowMillis) }.ranked()
        // Reason: grouping by dish type with no fallback put every recipe Spoonacular sent
        // without one into a single "" group, where a cap of one per group threw all but one of
        // them away. A recipe with no dish type is now its own group.
        // Reason: a cap of one per dish type meant only the first pick was chosen on score.
        // Spoonacular labels most of a page the same way, so the other two slots fell to the
        // variety fallback and came back near-arbitrary. Two scored picks, one for variety.
        val picks = pickVaried(ranked, MAX_RESULTS, maxPerGroup = 2) {
            it.item.recipe.dishTypes.firstOrNull()?.lowercase()
                ?.takeIf(String::isNotBlank)
                ?: "untyped-${it.itemId}"
        }
        return RecommendationResponse(
            recommendations = picks,
            rationale = "Ranked ${eligible.size} of ${candidates.size} recipes for a " +
                "${targets.calories} kcal meal.",
            advisory = advisory
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
            PROTEIN_FIT to proteinFit(pick.proteinGrams, targets.proteinGrams),
            MACRO_BALANCE to macroBalance(pick),
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

    /**
     * Protein against the per-meal target.
     *
     * Reason: this used to be the ratio capped at 1, which scored a recipe carrying three
     * times the target exactly as well as one that hit it. Protein above target is not a
     * problem the way calories above target are, so there is a tolerance band rather than a
     * symmetric penalty, but past it the credit does fall away.
     */
    private fun proteinFit(protein: Double, target: Double): Double {
        if (target <= 0.0) return 0.0
        val ratio = protein / target
        return when {
            ratio < 1.0 -> ratio
            ratio <= OVER_PROTEIN_TOLERANCE -> 1.0
            else -> (1.0 - (ratio - OVER_PROTEIN_TOLERANCE) / OVER_PROTEIN_TOLERANCE)
                .coerceAtLeast(0.0)
        }
    }

    /**
     * How skewed the portion's macros are, by share of its energy.
     *
     * Reason: calories and protein alone rank a 700 kcal plate of mostly fat level with a
     * balanced one. A recipe missing macro data scores neutral rather than zero, so a gap in
     * Spoonacular's nutrition is not read as a bad meal.
     */
    private fun macroBalance(pick: MealPick): Double {
        val energy = pick.proteinGrams * KCAL_PER_GRAM_PROTEIN +
            pick.carbohydratesGrams * KCAL_PER_GRAM_CARB +
            pick.fatGrams * KCAL_PER_GRAM_FAT
        if (energy <= 0.0) return NEUTRAL_BALANCE
        val fatShare = pick.fatGrams * KCAL_PER_GRAM_FAT / energy
        val excess = (fatShare - FAT_SHARE_TARGET).coerceAtLeast(0.0)
        return (1.0 - excess / (1.0 - FAT_SHARE_TARGET)).coerceIn(0.0, 1.0)
    }

    private fun novelty(
        context: MealRecommendationContext,
        recipe: Recipe,
        nowMillis: Long
    ): Double {
        // Reason: matching on the title alone missed a repeat whenever the logged name differed
        // at all from the recipe title. The recipe id is the reliable match; the title is the
        // fallback for meals the user typed in by hand.
        val lastEaten = context.recentMeals
            .filter {
                (it.recipeId != 0 && it.recipeId == recipe.id) ||
                    it.name.equals(recipe.title, ignoreCase = true)
            }
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
        // Reason: these sit beside the portion and the calories the card already shows, so a
        // reason that repeats either of those numbers buys the reader nothing. Each one says
        // what the figures do not.
        if ((features[LEARNED_AFFINITY] ?: 0.0) >= 0.3) add("Like your past picks")
        if ((features[CALORIE_FIT] ?: 0.0) >= 0.8) {
            add("Near your ${targets.calories} kcal target")
        }
        if ((features[PROTEIN_FIT] ?: 0.0) >= 0.8) {
            add("Protein toward ${targets.dailyProteinGrams} g a day")
        }
        if ((features[MACRO_BALANCE] ?: 0.0) >= 0.9 && pick.fatGrams > 0.0) {
            add("Balanced macros")
        }
        if (hasHistory && features[NOVELTY] == 1.0) add("Not eaten recently")
    }.take(MAX_REASONS)

    companion object {
        const val CALORIE_FIT = "calorieFit"
        const val PROTEIN_FIT = "proteinFit"
        const val MACRO_BALANCE = "macroBalance"
        const val NOVELTY = "novelty"
        const val LEARNED_AFFINITY = "learnedAffinity"

        val DEFAULT_WEIGHTS = ScoringWeights(
            mapOf(
                CALORIE_FIT to 0.30,
                PROTEIN_FIT to 0.22,
                MACRO_BALANCE to 0.13,
                NOVELTY to 0.12,
                LEARNED_AFFINITY to 0.23
            )
        )

        private const val MAX_RESULTS = 3
        private const val MAX_REASONS = 3

        /** Protein up to this multiple of the target still scores full marks. */
        private const val OVER_PROTEIN_TOLERANCE = 1.5

        /** Fat may supply this share of a meal's energy before the score starts falling. */
        private const val FAT_SHARE_TARGET = 0.40
        private const val NEUTRAL_BALANCE = 0.5
        private const val KCAL_PER_GRAM_PROTEIN = 4.0
        private const val KCAL_PER_GRAM_CARB = 4.0
        private const val KCAL_PER_GRAM_FAT = 9.0
    }
}
