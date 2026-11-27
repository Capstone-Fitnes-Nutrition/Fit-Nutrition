package sheridan.dheripu.fitnutrition.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import sheridan.dheripu.fitnutrition.model.recommendation.RecommendationFeedback

/** Stores recommendation feedback events under users/{uid}/recommendationFeedback. */
class RecommendationFeedbackRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun feedback(userId: String) = firestore
        .collection(USERS_COLLECTION)
        .document(userId)
        .collection(FEEDBACK_COLLECTION)

    fun record(
        userId: String,
        events: List<RecommendationFeedback>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val batch = firestore.batch()
        events.forEach { event ->
            val document = feedback(userId).document()
            batch.set(document, event.copy(id = document.id))
        }
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Unable to save feedback") }
    }

    fun loadRecent(
        userId: String,
        limit: Long,
        onSuccess: (List<RecommendationFeedback>) -> Unit,
        onError: (String) -> Unit
    ) {
        feedback(userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull {
                    it.toObject(RecommendationFeedback::class.java)?.copy(id = it.id)
                })
            }
            .addOnFailureListener { onError(it.message ?: "Unable to load feedback") }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FEEDBACK_COLLECTION = "recommendationFeedback"
    }
}
