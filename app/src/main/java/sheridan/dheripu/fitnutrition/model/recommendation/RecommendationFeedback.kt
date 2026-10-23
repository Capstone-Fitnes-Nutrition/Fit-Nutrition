package sheridan.dheripu.fitnutrition.model.recommendation

/** Which recommendation surface produced a suggestion. */
enum class RecommendationType { MEAL, WORKOUT }

/** What the user did with a suggestion; every action is logged so ranking can learn from it. */
enum class FeedbackAction { SHOWN, ACCEPTED, DISMISSED, COMPLETED }

/**
 * One feedback event, stored under users/{uid}/recommendationFeedback.
 *
 * Default values keep the class constructible by Firestore's `toObject`.
 * [scoreSnapshot] records the feature scores the user saw, so a learned ranker can later be
 * trained on exactly what was shown.
 */
data class RecommendationFeedback(
    val id: String = "",
    val recommendationId: String = "",
    val type: RecommendationType = RecommendationType.MEAL,
    val itemId: String = "",
    val action: FeedbackAction = FeedbackAction.SHOWN,
    val tags: List<String> = emptyList(),
    val reason: String = "",
    val scoreSnapshot: Map<String, Double> = emptyMap(),
    val createdAt: Long = 0L
)

/** Per-user tag affinities in the range -1..1, learned from feedback events. */
data class UserPreferenceProfile(
    val affinities: Map<String, Double> = emptyMap(),
    val dismissedItemIds: Set<String> = emptySet()
) {
    /** Mean affinity of the tags this profile has seen; unknown tags are ignored. */
    fun affinityFor(tags: Collection<String>): Double {
        val known = tags.mapNotNull { affinities[normalizeTag(it)] }
        return if (known.isEmpty()) 0.0 else known.average()
    }
}

/** Turns raw feedback into a [UserPreferenceProfile]. */
object PreferenceLearner {
    // Reason: adding a prior to the event count shrinks affinities for tags seen only once or
    // twice, so a single accept does not dominate ranking.
    private const val PRIOR_EVENTS = 2.0

    fun build(events: List<RecommendationFeedback>): UserPreferenceProfile {
        val totals = mutableMapOf<String, Pair<Double, Int>>()
        events.forEach { event ->
            val signal = signalOf(event.action) ?: return@forEach
            event.tags.map(::normalizeTag).distinct().forEach { tag ->
                val (sum, count) = totals[tag] ?: (0.0 to 0)
                totals[tag] = (sum + signal) to (count + 1)
            }
        }
        return UserPreferenceProfile(
            affinities = totals.mapValues { (_, value) ->
                value.first / (value.second + PRIOR_EVENTS)
            },
            dismissedItemIds = events.filter { it.action == FeedbackAction.DISMISSED }
                .map { it.itemId }
                .toSet()
        )
    }

    private fun signalOf(action: FeedbackAction): Double? = when (action) {
        FeedbackAction.SHOWN -> null
        FeedbackAction.ACCEPTED, FeedbackAction.COMPLETED -> 1.0
        FeedbackAction.DISMISSED -> -1.0
    }
}

internal fun normalizeTag(tag: String): String = tag.trim().lowercase()
