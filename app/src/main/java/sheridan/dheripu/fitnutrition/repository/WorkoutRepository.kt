package sheridan.dheripu.fitnutrition.repository

import com.google.firebase.firestore.FirebaseFirestore
import sheridan.dheripu.fitnutrition.model.WorkoutItem

class WorkoutRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun workoutsCollection(userId: String) = db.collection("users").document(userId).collection("workouts")

    fun addWorkout(
        userId: String,
        item: WorkoutItem,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        workoutsCollection(userId).add(item)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to add workout") }

    }

    fun getWorkouts(
        userId: String,
        onSuccess: (List<WorkoutItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        workoutsCollection(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val workouts = mutableListOf<WorkoutItem>()

                for (document in snapshot.documents){
                    val workout = document.toObject(WorkoutItem::class.java)
                    if (workout !=null){
                        workouts.add(workout)
                    }
                }
                onSuccess(workouts)
            }
            .addOnFailureListener { e -> onError (e.message ?: "Failed to load")}

    }


}