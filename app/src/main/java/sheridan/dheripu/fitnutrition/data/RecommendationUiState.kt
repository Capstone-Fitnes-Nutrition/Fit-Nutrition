package sheridan.dheripu.fitnutrition.data

import sheridan.dheripu.fitnutrition.model.recommendation.MealPick
import sheridan.dheripu.fitnutrition.model.recommendation.PrescribedExercise
import sheridan.dheripu.fitnutrition.model.recommendation.ScoredRecommendation

/** What a recommendation card shows. */
sealed interface RecommendationUiState {
    data object Idle : RecommendationUiState
    data object Loading : RecommendationUiState
    data object SignedOut : RecommendationUiState
    data class NeedsProfile(val missingFields: List<String>) : RecommendationUiState
    data class Error(val message: String) : RecommendationUiState

    /** The states that hold suggestions, so the card can treat meals and workouts alike. */
    sealed interface Suggestions : RecommendationUiState {
        val note: String?
        val handledIds: Set<String>
        val notice: String?

        /** Dismissed suggestions, newest first, still restorable. */
        val dismissedIds: List<String>

        /** The newest dismissal while the undo offer is still worth showing. */
        val undoableId: String?

        /** True while a dismissal can still be filled from what the engine already ranked. */
        val hasReserves: Boolean
    }

    /**
     * [handledIds] are suggestions the user already logged; [notice] is a one-line status.
     * [dismissed] keeps what the user turned down so it can be restored, which is why a
     * dismissal is removed from [items] rather than from the state altogether.
     */
    data class Meals(
        val items: List<ScoredRecommendation<MealPick>>,
        override val note: String?,
        override val handledIds: Set<String> = emptySet(),
        override val notice: String? = null,
        val dismissed: List<ScoredRecommendation<MealPick>> = emptyList(),
        override val undoableId: String? = null,
        val reserves: List<ScoredRecommendation<MealPick>> = emptyList()
    ) : Suggestions {
        override val dismissedIds: List<String> get() = dismissed.map { it.recommendationId }
        override val hasReserves: Boolean get() = reserves.isNotEmpty()
    }

    data class Workout(
        val items: List<ScoredRecommendation<PrescribedExercise>>,
        override val note: String?,
        override val handledIds: Set<String> = emptySet(),
        override val notice: String? = null,
        val dismissed: List<ScoredRecommendation<PrescribedExercise>> = emptyList(),
        override val undoableId: String? = null,
        val reserves: List<ScoredRecommendation<PrescribedExercise>> = emptyList()
    ) : Suggestions {
        override val dismissedIds: List<String> get() = dismissed.map { it.recommendationId }
        override val hasReserves: Boolean get() = reserves.isNotEmpty()
    }
}
