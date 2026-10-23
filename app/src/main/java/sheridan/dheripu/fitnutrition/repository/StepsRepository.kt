package sheridan.dheripu.fitnutrition.repository

import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StepEntry(
    val dateKey: String = "",
    val steps: Int = 0
)

class StepsRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun stepsCollection(userId: String) =
        db.collection("users").document(userId).collection("steps")

    fun saveTodaySteps(
        userId: String,
        steps: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val key = dateKey(System.currentTimeMillis())

        stepsCollection(userId).document(key)
            .set(StepEntry(key, steps))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to save steps") }
    }
    fun getSteps(
        userId: String,
        onSuccess: (List<StepEntry>) -> Unit,
        onError: (String) -> Unit
    ) {
        stepsCollection(userId).get()
            .addOnSuccessListener { snapshot -> onSuccess(snapshot.toObjects(StepEntry::class.java)) }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to load steps") }
    }
}

fun dateKey(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))