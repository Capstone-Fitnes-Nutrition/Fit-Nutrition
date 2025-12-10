package sheridan.dheripu.fitnutrition.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.WorkoutItem

class FitnessViewModel : ViewModel() {
    private val _myWorkouts = mutableStateListOf<WorkoutItem>()
    val myWorkouts: List<WorkoutItem> = _myWorkouts

    var exercises by mutableStateOf<List<Exercise>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set
    fun addWorkoutItem(item: WorkoutItem){
        _myWorkouts.add(item)
    }
    fun clearExercises() {
        exercises = emptyList()
        errorMessage = null
    }


    fun fetchExercisesByBodyPart(bodyPart: String) {
        if (bodyPart.isBlank()) {
            exercises = emptyList()
            return
        }

        val call = RetrofitInstance.api.getExercisesByBodyPart(bodyPart.lowercase())

        call.enqueue(object : Callback<List<Exercise>> {
            override fun onResponse(call: Call<List<Exercise>>, response: Response<List<Exercise>>) {
                when {
                    response.isSuccessful -> {
                        exercises = response.body() ?: emptyList()
                        errorMessage = null
                    }
                    response.code() == 404 || response.code() == 422 -> {
                        exercises = emptyList()
                        errorMessage = "No exercises found for \"$bodyPart\"."
                    }
                }
            }

            override fun onFailure(call: Call<List<Exercise>>, t: Throwable) {
                exercises = emptyList()
                errorMessage = "Failed to fetch any exercises. Something might be wrong with wifi"
            }
        })
    }
}
