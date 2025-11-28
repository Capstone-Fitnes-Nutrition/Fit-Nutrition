package sheridan.dheripu.fitnutrition.model

data class WorkoutItem(
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val time: Int,
)
