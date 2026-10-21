package sheridan.dheripu.fitnutrition.repository

import com.google.firebase.firestore.FirebaseFirestore
import sheridan.dheripu.fitnutrition.model.ContextPreparation
import sheridan.dheripu.fitnutrition.model.MealRecommendationContext
import sheridan.dheripu.fitnutrition.model.NutritionLog
import sheridan.dheripu.fitnutrition.model.RecommendationContextFactory
import sheridan.dheripu.fitnutrition.model.RecommendationRequest
import sheridan.dheripu.fitnutrition.model.User
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.model.WorkoutRecommendationContext

/** Fetches a user's Firebase profile and history before building validated sample requests. */
class RecommendationContextRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun userDocument(userId: String) = firestore.collection("users").document(userId)

    fun prepareMeal(
        userId: String,
        onPrepared: (ContextPreparation<RecommendationRequest<MealRecommendationContext>>) -> Unit,
        onError: (String) -> Unit
    ) {
        val document = userDocument(userId)
        document.get().addOnSuccessListener { profileSnapshot ->
            val profile = profileSnapshot.takeIf { it.exists() }?.toObject(User::class.java)
            document.collection("nutritionLogs").get()
                .addOnSuccessListener { historySnapshot ->
                    val history = historySnapshot.documents.mapNotNull { item ->
                        item.toObject(NutritionLog::class.java)?.copy(id = item.id)
                    }.sortedByDescending { it.loggedAt }
                    onPrepared(RecommendationContextFactory.meal(profile, history))
                }
                .addOnFailureListener {
                    onPrepared(
                        RecommendationContextFactory.meal(
                            profile,
                            emptyList(),
                            historyWarning = "Meal history is unavailable; using profile data only."
                        )
                    )
                }
        }.addOnFailureListener { onError(it.message ?: "Unable to load nutrition profile") }
    }

    fun prepareWorkout(
        userId: String,
        onPrepared: (ContextPreparation<RecommendationRequest<WorkoutRecommendationContext>>) -> Unit,
        onError: (String) -> Unit
    ) {
        val document = userDocument(userId)
        document.get().addOnSuccessListener { profileSnapshot ->
            val profile = profileSnapshot.takeIf { it.exists() }?.toObject(User::class.java)
            document.collection("workouts").get()
                .addOnSuccessListener { historySnapshot ->
                    val history = historySnapshot.documents.mapNotNull { item ->
                        item.toObject(WorkoutItem::class.java)
                    }.sortedByDescending { it.completedAt }
                    onPrepared(RecommendationContextFactory.workout(profile, history))
                }
                .addOnFailureListener {
                    onPrepared(
                        RecommendationContextFactory.workout(
                            profile,
                            emptyList(),
                            historyWarning = "Workout history is unavailable; using profile data only."
                        )
                    )
                }
        }.addOnFailureListener { onError(it.message ?: "Unable to load fitness profile") }
    }
}
