package sheridan.dheripu.fitnutrition.model

data class WorkoutItem(
    val exercise: Exercise = Exercise(),
    val sets: Int = 0,
    val reps: Int = 0,
    val time: Int = 0,
    val completedAt: Long = System.currentTimeMillis()
)
