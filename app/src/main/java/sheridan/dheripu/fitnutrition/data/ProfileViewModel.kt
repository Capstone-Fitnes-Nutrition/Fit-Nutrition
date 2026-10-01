package sheridan.dheripu.fitnutrition.data

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sheridan.dheripu.fitnutrition.AuthManager
import sheridan.dheripu.fitnutrition.model.User
import sheridan.dheripu.fitnutrition.repository.ProfileRepository

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: User? = null,
    val isProfileMissing: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveMessage: String? = null
)

/** Owns profile loading and saving for the currently authenticated Firebase user. */
class ProfileViewModel(
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var activeUserId: String? = null
    private var profileListener: ListenerRegistration? = null

    fun loadProfile() {
        val firebaseUser = AuthManager.currentUser
        if (firebaseUser == null) {
            stopListening()
            _uiState.value = ProfileUiState(
                isLoading = false,
                errorMessage = "Sign in to view your profile"
            )
            return
        }

        if (activeUserId == firebaseUser.uid && profileListener != null) return

        stopListening()
        activeUserId = firebaseUser.uid
        _uiState.value = ProfileUiState(isLoading = true)

        profileListener = profileRepository.observeProfile(
            userId = firebaseUser.uid,
            onProfileChanged = { profile ->
                if (activeUserId != firebaseUser.uid) return@observeProfile

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    profile = profile,
                    isProfileMissing = profile == null,
                    errorMessage = null
                )
            },
            onError = { message ->
                if (activeUserId != firebaseUser.uid) return@observeProfile

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = message
                )
            }
        )
    }

    fun saveProfile(
        name: String,
        weight: String,
        height: String,
        fitnessGoal: String,
        activityLevel: String,
        dailyCalorieTarget: String,
        dailyProteinTarget: String,
        dietaryPreference: String
    ) {
        val firebaseUser = AuthManager.currentUser
        if (firebaseUser == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sign in to save a profile")
            return
        }

        validateProfile(
            name = name,
            weight = weight,
            height = height,
            dailyCalorieTarget = dailyCalorieTarget,
            dailyProteinTarget = dailyProteinTarget
        )?.let { validationError ->
            _uiState.value = _uiState.value.copy(
                errorMessage = validationError,
                saveMessage = null
            )
            return
        }

        val profile = User(
            id = firebaseUser.uid,
            email = firebaseUser.email.orEmpty(),
            name = name.trim(),
            weight = weight.trim(),
            height = height.trim(),
            fitnessGoal = fitnessGoal,
        )

        _uiState.value = _uiState.value.copy(
            isSaving = true,
            errorMessage = null,
            saveMessage = null
        )

        profileRepository.saveProfile(
            profile = profile,
            isNewProfile = _uiState.value.isProfileMissing,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    profile = profile,
                    isProfileMissing = false,
                    saveMessage = "Profile saved"
                )
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = message
                )
            }
        )
    }

    fun clearSaveMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
    }

    fun clearProfile() {
        stopListening()
        _uiState.value = ProfileUiState(isLoading = false)
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }

    private fun stopListening() {
        profileListener?.remove()
        profileListener = null
        activeUserId = null
    }

    private fun validateProfile(
        name: String,
        weight: String,
        height: String,
        dailyCalorieTarget: String,
        dailyProteinTarget: String
    ): String? {
        if (name.isBlank()) return "Name is required"
        if (!isPositiveDecimalOrBlank(weight)) return "Weight must be a positive number"
        if (!isPositiveDecimalOrBlank(height)) return "Height must be a positive number"
        if (!isPositiveWholeNumberOrBlank(dailyCalorieTarget)) {
            return "Daily calorie target must be a positive whole number"
        }
        if (!isPositiveWholeNumberOrBlank(dailyProteinTarget)) {
            return "Daily protein target must be a positive whole number"
        }
        return null
    }

    private fun isPositiveDecimalOrBlank(value: String): Boolean =
        value.isBlank() || (value.toDoubleOrNull()?.let { it > 0 } == true)

    private fun isPositiveWholeNumberOrBlank(value: String): Boolean =
        value.isBlank() || (value.toIntOrNull()?.let { it > 0 } == true)
}
