package sheridan.dheripu.fitnutrition.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import sheridan.dheripu.fitnutrition.model.NutritionLog

/** Stores meal logs below the same user document that owns the user's profile and workouts. */
class NutritionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun nutritionLogs(userId: String) = firestore
        .collection(USERS_COLLECTION)
        .document(userId)
        .collection(NUTRITION_LOGS_COLLECTION)

    fun addNutritionLog(
        userId: String,
        log: NutritionLog,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        nutritionLogs(userId)
            .add(log)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.message ?: "Unable to log this meal")
            }
    }

    fun observeNutritionLogs(
        userId: String,
        onLogsChanged: (List<NutritionLog>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration = nutritionLogs(userId)
        .orderBy("loggedAt")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Unable to load nutrition logs")
                return@addSnapshotListener
            }

            val logs = snapshot?.documents
                ?.mapNotNull { document ->
                    document.toObject(NutritionLog::class.java)?.copy(id = document.id)
                }
                .orEmpty()
                .sortedByDescending { it.loggedAt }

            onLogsChanged(logs)
        }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val NUTRITION_LOGS_COLLECTION = "nutritionLogs"
    }
}
