package sheridan.dheripu.fitnutrition.model.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceLearnerTest {
    private fun event(action: FeedbackAction, itemId: String, vararg tags: String) =
        RecommendationFeedback(itemId = itemId, action = action, tags = tags.toList())

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
    fun dismissedItemsAreRemembered() {
        val profile = PreferenceLearner.build(
            listOf(
                event(FeedbackAction.DISMISSED, "7"),
                event(FeedbackAction.ACCEPTED, "8")
            )
        )

        assertEquals(setOf("7"), profile.dismissedItemIds)
    }
}
