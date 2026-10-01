package sheridan.dheripu.fitnutrition.data

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sheridan.dheripu.fitnutrition.AuthManager
import sheridan.dheripu.fitnutrition.model.NutritionLog
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.repository.NutritionRepository
import kotlin.math.roundToInt

data class NutritionUiState(
    val isLoading: Boolean = false,
    val isLogging: Boolean = false,
    val logs: List<NutritionLog> = emptyList(),
    val errorMessage: String? = null,
    val actionMessage: String? = null
)

class NutritionViewModel(
    private val nutritionRepository: NutritionRepository = NutritionRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    private var activeUserId: String? = null
    private var logListener: ListenerRegistration? = null

    fun loadLogs() {
        val userId = AuthManager.currentUser?.uid
        if (userId == null) {
            stopListening()
            _uiState.value = NutritionUiState(errorMessage = "Sign in to view nutrition analytics")
            return
        }

        if (activeUserId == userId && logListener != null) return

        stopListening()
        activeUserId = userId
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        logListener = nutritionRepository.observeNutritionLogs(
            userId = userId,
            onLogsChanged = { logs ->
                if (activeUserId != userId) return@observeNutritionLogs
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    logs = logs,
                    errorMessage = null
                )
            },
            onError = { message ->
                if (activeUserId != userId) return@observeNutritionLogs
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = message)
            }
        )
    }

    fun logRecipe(recipe: Recipe) {
        val userId = AuthManager.currentUser?.uid
        if (userId == null) {
            _uiState.value = _uiState.value.copy(actionMessage = "Sign in to log a meal")
            return
        }

        val nutrients = recipe.nutrition?.nutrients.orEmpty()
        val calories = nutrientAmount(nutrients, "Calories").roundToInt()
        val protein = nutrientAmount(nutrients, "Protein")
        val carbohydrates = nutrientAmount(nutrients, "Carbohydrates")
        val fat = nutrientAmount(nutrients, "Fat")

        if (calories <= 0 && protein <= 0 && carbohydrates <= 0 && fat <= 0) {
            _uiState.value = _uiState.value.copy(
                actionMessage = "This recipe does not include nutrition information to log"
            )
            return
        }

        val log = NutritionLog(
            recipeId = recipe.id,
            mealName = recipe.title,
            calories = calories,
            proteinGrams = protein,
            carbohydratesGrams = carbohydrates,
            fatGrams = fat
        )
        _uiState.value = _uiState.value.copy(isLogging = true, actionMessage = null)

        nutritionRepository.addNutritionLog(
            userId = userId,
            log = log,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    isLogging = false,
                    actionMessage = "${recipe.title} logged"
                )
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(isLogging = false, actionMessage = message)
            }
        )
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun clearLogs() {
        stopListening()
        _uiState.value = NutritionUiState()
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }

    private fun stopListening() {
        logListener?.remove()
        logListener = null
        activeUserId = null
    }

    private fun nutrientAmount(
        nutrients: List<sheridan.dheripu.fitnutrition.model.Nutrient>,
        nutrientName: String
    ): Double = nutrients
        .firstOrNull { it.name.equals(nutrientName, ignoreCase = true) }
        ?.amount
        ?: 0.0
}
