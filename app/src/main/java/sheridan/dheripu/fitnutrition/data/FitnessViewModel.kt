package sheridan.dheripu.fitnutrition.data

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.repository.FitnessRepository

class FitnessViewModel : ViewModel() {
    private val repository = FitnessRepository()
    val myWorkouts = mutableStateOf<List<WorkoutItem>>(emptyList())

    var exercises = mutableStateOf<List<Exercise>>(emptyList())

    var errorMessage:String? = null

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
    }
    fun clearExercises() {
        exercises.value = emptyList()
        errorMessage = null
    }


}