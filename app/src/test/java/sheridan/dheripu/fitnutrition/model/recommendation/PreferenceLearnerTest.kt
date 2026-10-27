package sheridan.dheripu.fitnutrition.model.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceLearnerTest {
    private fun event(
        action: FeedbackAction,
        itemId: String,
        vararg tags: String,
        createdAt: Long = NOW
    ) = RecommendationFeedback(
        itemId = itemId,
        action = action,
        tags = tags.toList(),
        createdAt = createdAt
    )

    @Test
    fun acceptedTagsGainAffinityAndDismissedTagsLoseIt() {
        val profile = PreferenceLearner.build(
            listOf(
                event(FeedbackAction.ACCEPTED, "1", "Salad"),
                event(FeedbackAction.COMPLETED, "2", "salad"),
                event(FeedbackAction.DISMISSED, "3", "pasta")
            )
        )

        assertTrue(profile.affinityFor(listOf("salad")) > 0.4)
        assertTrue(profile.affinityFor(listOf("pasta")) < 0.0)
    }

    @Test
    fun shownEventsDoNotChangeAffinity() {
        val profile = PreferenceLearner.build(listOf(event(FeedbackAction.SHOWN, "1", "salad")))

        assertEquals(0.0, profile.affinityFor(listOf("salad")), 0.0001)
    }

    @Test
    fun affinityAveragesOnlyKnownTagsAndStaysWithinOne() {
        val events = List(20) { event(FeedbackAction.ACCEPTED, "$it", "chest") }
        val profile = PreferenceLearner.build(events)

        val affinity = profile.affinityFor(listOf("chest", "unknown tag"))

        assertTrue(affinity > 0.8)
        assertTrue(affinity < 1.0)
        assertEquals(0.0, profile.affinityFor(emptyList()), 0.0001)
    }

    @Test
    fun recentlyDismissedItemsAreHidden() {
        val profile = PreferenceLearner.build(
            listOf(
                event(FeedbackAction.DISMISSED, "7"),
                event(FeedbackAction.ACCEPTED, "8")
            ),
            nowMillis = NOW
        )

        assertEquals(setOf("7"), profile.dismissedItemIds)
    }

    @Test
    fun aDismissalStopsHidingTheItemOnceTheWindowPasses() {
        val justInside = PreferenceLearner.DISMISSAL_WINDOW_MILLIS - 1_000
        val justOutside = PreferenceLearner.DISMISSAL_WINDOW_MILLIS + 1_000
        val events = listOf(
            event(FeedbackAction.DISMISSED, "fresh", createdAt = NOW - justInside),
            event(FeedbackAction.DISMISSED, "stale", "pasta", createdAt = NOW - justOutside)
        )

        val profile = PreferenceLearner.build(events, nowMillis = NOW)

        assertEquals(setOf("fresh"), profile.dismissedItemIds)
        // The expired dismissal still shapes ranking, it just stops hiding the item outright.
        assertTrue(profile.affinityFor(listOf("pasta")) < 0.0)
    }

    @Test
    fun aDismissalWithNoTimestampDoesNotHideAnythingForever() {
        val profile = PreferenceLearner.build(
            listOf(event(FeedbackAction.DISMISSED, "9", createdAt = 0L)),
            nowMillis = NOW
        )

        assertEquals(emptySet<String>(), profile.dismissedItemIds)
    }

    private companion object {
        const val NOW = 1_792_944_000_000L
    }
}
