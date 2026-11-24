package sheridan.dheripu.fitnutrition.model.recommendation

/** Which recommendation surface produced a suggestion. */
enum class RecommendationType { MEAL, WORKOUT }

/**
 * What the user did with a suggestion; every action is logged so ranking can learn from it.
 *
 * [RESTORED] undoes a [DISMISSED]: the item stops being hidden and the dismissal's negative
 * signal is cancelled, so an accidental tap does not quietly bias ranking for good.
 */
enum class FeedbackAction { SHOWN, ACCEPTED, DISMISSED, COMPLETED, RESTORED }

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

/**
 * Per-user tag affinities in the range -1..1, learned from feedback events.
 *
 * [dismissedItemIds] holds only the *recent* dismissals, which the engines drop outright. A
 * dismissal keeps affecting ranking through [affinities] long after it stops hiding the item.
 */
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

    /**
     * How long a dismissed suggestion stays hidden. Long enough that refreshing does not hand
     * back what the user just rejected, short enough that a good suggestion is not lost for
     * good over one dismissal.
     */
    const val DISMISSAL_WINDOW_MILLIS = 10 * 60 * 1000L

    fun build(
        events: List<RecommendationFeedback>,
        nowMillis: Long = System.currentTimeMillis()
    ): UserPreferenceProfile {
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
            dismissedItemIds = suppressedItemIds(events, nowMillis)
        )
    }

    /**
     * Items to hide: those whose most recent dismissal is still inside the window and has not
     * been restored since.
     *
     * Reason: judging this per item on the newest dismissal against the newest restore is what
     * makes an undo work across a reload. Looking only for a DISMISSED event meant a restored
     * item stayed hidden for the rest of the window however many times the user asked for it
     * back.
     *
     * A restore wins a tie. Two events can carry the same millisecond, and in that case the
     * user's "bring it back" is the one to honour: the cost of honouring it wrongly is one
     * unwanted suggestion, against a suggestion the user explicitly asked for staying hidden.
     */
    private fun suppressedItemIds(
        events: List<RecommendationFeedback>,
        nowMillis: Long
    ): Set<String> = events
        .filter { it.action == FeedbackAction.DISMISSED || it.action == FeedbackAction.RESTORED }
        .groupBy { it.itemId }
        .filterValues { perItem ->
            val dismissed = perItem.filter { it.action == FeedbackAction.DISMISSED }
                .maxByOrNull { it.createdAt }
                ?: return@filterValues false
            if (!dismissed.isRecent(nowMillis)) return@filterValues false
            val restored = perItem.filter { it.action == FeedbackAction.RESTORED }
                .maxOfOrNull { it.createdAt }
            restored == null || restored < dismissed.createdAt
        }
        .keys

    /**
     * True while the dismissal is still inside the suppression window. An event with no
     * timestamp counts as expired, so a missing `createdAt` cannot hide an item forever.
     */
    private fun RecommendationFeedback.isRecent(nowMillis: Long): Boolean =
        createdAt > 0L && nowMillis - createdAt in 0 until DISMISSAL_WINDOW_MILLIS

    // Reason: RESTORED carries +1 so it offsets the -1 the dismissal it undoes contributed.
    // The pair then averages to roughly neutral rather than leaving a penalty behind.
    private fun signalOf(action: FeedbackAction): Double? = when (action) {
        FeedbackAction.SHOWN -> null
        FeedbackAction.ACCEPTED, FeedbackAction.COMPLETED, FeedbackAction.RESTORED -> 1.0
        FeedbackAction.DISMISSED -> -1.0
    }
}

internal fun normalizeTag(tag: String): String = tag.trim().lowercase()
