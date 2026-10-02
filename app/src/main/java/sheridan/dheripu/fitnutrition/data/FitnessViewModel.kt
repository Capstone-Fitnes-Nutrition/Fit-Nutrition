package sheridan.dheripu.fitnutrition.data

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import sheridan.dheripu.fitnutrition.AuthManager
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.repository.FitnessRepository
import sheridan.dheripu.fitnutrition.repository.WorkoutRepository

class FitnessViewModel : ViewModel() {
    private val repository = FitnessRepository()
    private val workoutRepository = WorkoutRepository()
    val myWorkouts = mutableStateOf<List<WorkoutItem>>(emptyList())

    var exercises = mutableStateOf<List<Exercise>>(emptyList())

    var errorMessage:String? = null

    init {
        loadWorkouts()
    }
    private fun loadWorkouts() {
        val uid = AuthManager.currentUser?.uid ?:return
        workoutRepository.getWorkouts(
            userId = uid,
            onSuccess = {myWorkouts.value = it},
            onError = { message ->
                errorMessage = if (message.contains("PERMISSION_DENIED", ignoreCase = true)) {
                    "Saved workout history isn't accessible for this account. " +
                        "Check Firestore rules for users/{uid}/workouts."
                } else {
                    message
                }
            }
        )
    }

    fun fetchExercisesByBodyPart(bodyPart: String) {
        repository.getExercisesByBodyPart (
            bodyPart = bodyPart,
            onSuccess = { fetchedExercises ->
                exercises.value = fetchedExercises
                errorMessage = null
            },
            onError = { error ->
                exercises.value = emptyList()
                errorMessage = error
            }
        )
    }
    fun addWorkoutItem(item: WorkoutItem){
        myWorkouts.value = myWorkouts.value + item
        val uid = AuthManager.currentUser?.uid ?: return
        workoutRepository.addWorkout(
            userId = uid,
            item = item,
            onSuccess = {},
            onError = { errorMessage = it}
        )
    }
    fun clearExercises() {
        exercises.value = emptyList()
        errorMessage = null
    }


}
