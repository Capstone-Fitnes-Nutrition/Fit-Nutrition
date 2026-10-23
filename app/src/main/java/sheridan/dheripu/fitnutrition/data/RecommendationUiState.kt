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

    /** [handledIds] are suggestions the user already logged; [notice] is a one-line status. */
    data class Meals(
        val items: List<ScoredRecommendation<MealPick>>,
        val note: String?,
        val handledIds: Set<String> = emptySet(),
        val notice: String? = null
    ) : RecommendationUiState

    data class Workout(
        val items: List<ScoredRecommendation<PrescribedExercise>>,
        val note: String?,
        val handledIds: Set<String> = emptySet(),
        val notice: String? = null
    ) : RecommendationUiState
}
