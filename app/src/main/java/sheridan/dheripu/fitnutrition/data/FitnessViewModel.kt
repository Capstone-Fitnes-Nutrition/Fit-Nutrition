package sheridan.dheripu.fitnutrition.data

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import sheridan.dheripu.fitnutrition.model.WorkoutItem

class FitnessViewModel : ViewModel() {
    private val _myWorkouts = mutableStateListOf<WorkoutItem>()
    val myWorkouts: List<WorkoutItem> = _myWorkouts

    fun addWorkoutItem(item: WorkoutItem){
        _myWorkouts.add(item)
    }
}