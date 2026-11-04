package sheridan.dheripu.fitnutrition.model.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.WorkoutHistoryItem
import sheridan.dheripu.fitnutrition.model.WorkoutRecommendationContext

class WorkoutRecommendationEngineTest {
    private val engine = WorkoutRecommendationEngine()

    @Test
    fun keepsOnlyExercisesForAvailableEquipment() {
        val result = engine.recommend(
            context(equipment = listOf("Dumbbells"), activityLevel = "Very Active"),
            listOf(
                exercise("1", "dumbbell press", equipment = "dumbbell"),
                exercise("2", "push up", equipment = "body weight"),
                exercise("3", "barbell bench press", equipment = "barbell")
            ),
            UserPreferenceProfile(),
            NOW
        )

        assertEquals(setOf("1", "2"), result.recommendations.map { it.item.exercise.id }.toSet())
    }

    @Test
    fun beginnersOnlySeeBeginnerFriendlyExercises() {
        val result = engine.recommend(
            context(activityLevel = "Sedentary"),
            listOf(
                exercise("1", "dumbbell press", equipment = "dumbbell"),
                exercise("2", "push up", equipment = "body weight")
            ),
            UserPreferenceProfile(),
            NOW
        )

        assertEquals(listOf("2"), result.recommendations.map { it.item.exercise.id })
    }

    @Test
    fun advancedExercisesUnlockOnlyAfterPrerequisiteWorkouts() {
        val candidates = listOf(exercise("1", "barbell squat", "upper legs", "barbell"))
        val noHistory = context(activityLevel = "Very Active", equipment = listOf("Full gym"))
        val experienced = noHistory.copy(
            recentWorkouts = List(5) { history("push up", "chest", daysAgo = 10 + it) }
        )

        val locked = engine.recommend(noHistory, candidates, UserPreferenceProfile(), NOW)
        val unlocked = engine.recommend(experienced, candidates, UserPreferenceProfile(), NOW)

        assertTrue(locked.recommendations.isEmpty())
        assertEquals(1, unlocked.recommendations.size)
    }

    @Test
    fun recentlyTrainedBodyPartRanksLower() {
        val result = engine.recommend(
            context(recent = listOf(history("push up", "chest", daysAgo = 1))),
            listOf(
                exercise("1", "diamond push up", "chest"),
                exercise("2", "superman", "back")
            ),
            UserPreferenceProfile(),
            NOW
        )

        assertEquals("2", result.recommendations.first().item.exercise.id)
        assertTrue(result.recommendations.first().reasons.any { it.contains("rested") })
    }

    @Test
    fun progressionAddsOneRepToTheLastSession() {
        val last = history("push up", "chest", daysAgo = 4, sets = 3, reps = 10)

        val result = engine.recommend(
            context(recent = listOf(last)),
            listOf(exercise("1", "Push Up", "chest")),
            UserPreferenceProfile(),
            NOW
        )

        val prescription = result.recommendations.single().item
        assertEquals(3, prescription.sets)
        assertEquals(11, prescription.reps)
        assertTrue(result.recommendations.single().reasons.any { it.contains("3x10") })
    }

    @Test
    fun cardioIsPrescribedByDuration() {
        val result = engine.recommend(
            context(goal = "Weight Loss"),
            listOf(exercise("1", "jumping jack", "cardio")),
            UserPreferenceProfile(),
            NOW
        )

        val prescription = result.recommendations.single().item
        assertEquals(20, prescription.durationMinutes)
        assertEquals(0, prescription.reps)
    }

    @Test
    fun spreadsPicksAcrossBodyPartsBeforeRepeating() {
        val chest = (1..6).map { exercise("c$it", "chest move $it", "chest") }
        val back = (1..3).map { exercise("b$it", "back move $it", "back") }

        val result = engine.recommend(context(), chest + back, UserPreferenceProfile(), NOW)

        assertEquals(5, result.recommendations.size)
        val firstFour = result.recommendations.take(4).map { it.item.exercise.bodyPart }
        assertEquals(2, firstFour.count { it == "chest" })
        assertEquals(2, firstFour.count { it == "back" })
    }

    @Test
    fun dismissedExercisesAreNotSuggestedAgain() {
        val result = engine.recommend(
            context(),
            listOf(exercise("1", "push up", "chest"), exercise("2", "superman", "back")),
            UserPreferenceProfile(dismissedItemIds = setOf("1")),
            NOW
        )

        assertFalse(result.recommendations.any { it.item.exercise.id == "1" })
        assertEquals("workout-2-$NOW", result.recommendations.single().recommendationId)
    }

    @Test
    fun equipmentYouOwnButCannotUseYetIsExplained() {
        val machines = (1..3).map {
            exercise("m$it", "smith press $it", "chest", equipment = "smith machine")
        }
        val sedentary = context(activityLevel = "Sedentary", equipment = listOf("Smith machine"))

        val result = engine.recommend(
            sedentary,
            machines + exercise("p1", "push up", "chest"),
            UserPreferenceProfile(),
            NOW
        )

        val advisory = requireNotNull(result.advisory)
        assertTrue(advisory.contains("3 exercises"))
        assertTrue(advisory.contains("equipment for"))
        assertTrue(advisory.contains("activity level"))
        assertEquals("push up", result.recommendations.single().item.exercise.name)
    }

    @Test
    fun theStrongestBodyPartDoesNotTakeEverySpareSlot() {
        // Weight loss scores cardio highest, so without a spread the whole list is cardio.
        val cardio = (1..6).map { exercise("c$it", "cardio move $it", "cardio") }
        val equipmentWork = (1..3).map {
            exercise("d$it", "dumbbell move $it", "chest", equipment = "dumbbell")
        }

        val result = engine.recommend(
            context(goal = "Weight Loss", equipment = listOf("Dumbbell")),
            cardio + equipmentWork,
            UserPreferenceProfile(),
            NOW
        )

        val bodyParts = result.recommendations.map { it.item.exercise.bodyPart }
        assertEquals(5, result.recommendations.size)
        assertTrue("owned equipment should appear: $bodyParts", bodyParts.contains("chest"))
        assertTrue(
            "cardio should not fill every slot: $bodyParts",
            bodyParts.count { it == "cardio" } < 5
        )
    }

    @Test
    fun exercisesNeedingEquipmentYouDoNotOwnAreNamed() {
        val result = engine.recommend(
            context(equipment = listOf("Body weight")),
            listOf(
                exercise("1", "push up", "chest"),
                exercise("2", "bench press", "chest", equipment = "barbell"),
                exercise("3", "cable fly", "chest", equipment = "cable")
            ),
            UserPreferenceProfile(),
            NOW
        )

        val advisory = requireNotNull(result.advisory)
        assertTrue(advisory.contains("2 exercises need equipment"))
        assertTrue(advisory.contains("barbell"))
        assertTrue(advisory.contains("cable"))
    }

    @Test
    fun noAdvisoryWhenTheLevelGateRemovedNothing() {
        val result = engine.recommend(
            context(equipment = listOf("Body weight")),
            listOf(exercise("1", "push up", "chest")),
            UserPreferenceProfile(),
            NOW
        )

        assertEquals(null, result.advisory)
    }

    private fun context(
        goal: String = "General Fitness",
        activityLevel: String = "Moderately Active",
        equipment: List<String> = listOf("Body weight"),
        recent: List<WorkoutHistoryItem> = emptyList()
    ) = WorkoutRecommendationContext(
        fitnessGoal = goal,
        activityLevel = activityLevel,
        availableEquipment = equipment,
        recentWorkouts = recent
    )

    private fun exercise(
        id: String,
        name: String,
        bodyPart: String = "chest",
        equipment: String = "body weight"
    ) = Exercise(id = id, name = name, bodyPart = bodyPart, target = "$bodyPart muscle",
        equipment = equipment)

    private fun history(
        name: String,
        bodyPart: String,
        daysAgo: Int,
        sets: Int = 3,
        reps: Int = 12
    ) = WorkoutHistoryItem(
        exerciseName = name,
        sets = sets,
        repetitions = reps,
        durationMinutes = 0,
        completedAtEpochMillis = NOW - daysAgo * DAY,
        bodyPart = bodyPart
    )

    companion object {
        private const val NOW = 1_792_756_800_000L
        private const val DAY = 86_400_000L
    }
}
