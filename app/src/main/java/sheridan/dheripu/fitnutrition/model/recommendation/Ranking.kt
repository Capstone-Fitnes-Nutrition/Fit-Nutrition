package sheridan.dheripu.fitnutrition.model.recommendation

import sheridan.dheripu.fitnutrition.model.RecommendationResponse

/** Feature scores for one candidate plus their weighted total. */
data class ScoreBreakdown(val total: Double, val features: Map<String, Double>)

/**
 * Feature weights. Defaults are hand-tuned; once enough feedback exists these can be replaced
 * by weights learned from accepted versus dismissed suggestions.
 */
data class ScoringWeights(val weights: Map<String, Double>) {
    fun combine(features: Map<String, Double>): ScoreBreakdown {
        val totalWeight = weights.values.sum()
        val total = if (totalWeight == 0.0) 0.0 else
            weights.entries.sumOf { (name, weight) -> weight * (features[name] ?: 0.0) } /
                totalWeight
        return ScoreBreakdown(total, features)
    }
}

/** A hard rule: a candidate that fails it is never shown. */
fun interface CandidateFilter<C, T> {
    fun keep(context: C, candidate: T): Boolean
}

/** A ranked suggestion with the reasons and tags used for display and feedback. */
data class ScoredRecommendation<R>(
    val recommendationId: String,
    val itemId: String,
    val item: R,
    val score: ScoreBreakdown,
    val reasons: List<String>,
    val tags: List<String>
)

/** Ranks [candidates] of type T for context C and returns suggestions of type R. */
interface RecommendationEngine<C, T, R> {
    fun recommend(
        context: C,
        candidates: List<T>,
        preferences: UserPreferenceProfile,
        nowMillis: Long
    ): RecommendationResponse<ScoredRecommendation<R>>
}

/**
 * Picks [count] items from [ranked] (best first), allowing at most [maxPerGroup] per group on
 * the first pass and filling any remaining slots from the leftovers in rank order.
 */
internal fun <R> pickVaried(
    ranked: List<ScoredRecommendation<R>>,
    count: Int,
    maxPerGroup: Int,
    groupOf: (ScoredRecommendation<R>) -> String
): List<ScoredRecommendation<R>> {
    val picked = mutableListOf<ScoredRecommendation<R>>()
    val perGroup = mutableMapOf<String, Int>()
    for (candidate in ranked) {
        if (picked.size == count) break
        val group = groupOf(candidate)
        if ((perGroup[group] ?: 0) < maxPerGroup) {
            picked += candidate
            perGroup[group] = (perGroup[group] ?: 0) + 1
        }
    }
    ranked.filterNot { it in picked }.take(count - picked.size).let { picked += it }
    return picked
}

/** Orders by total score, breaking ties by item id so results are deterministic. */
internal fun <R> List<ScoredRecommendation<R>>.ranked(): List<ScoredRecommendation<R>> =
    sortedWith(compareByDescending<ScoredRecommendation<R>> { it.score.total }.thenBy { it.itemId })

/** Lowercases and drops a plural "s" so "Peanuts" matches "peanut butter". */
internal fun singular(word: String): String {
    val lower = word.trim().lowercase()
    return if (lower.length > 3 && lower.endsWith("s") && !lower.endsWith("ss")) {
        lower.dropLast(1)
    } else {
        lower
    }
}

internal const val DAY_MILLIS = 86_400_000L
