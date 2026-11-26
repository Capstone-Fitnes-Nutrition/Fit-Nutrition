package sheridan.dheripu.fitnutrition.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily health metrics, one document per day at users/{uid}/healthMetrics/{yyyy-MM-dd}.
 *
 * This used to write to the Realtime Database. The project has no Realtime Database instance —
 * google-services.json carries no firebase_url and the candidate hosts answer 404 — so every
 * write went nowhere and every read came back empty. Firestore is what the rest of the app
 * already uses, and the existing rules cover this subcollection.
 */
class HealthRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun metricsCollection(userId: String) =
        db.collection("users").document(userId).collection(COLLECTION)

    /** Stores one day, keyed by its date so a re-sync overwrites rather than duplicates. */
    fun saveHealthMetrics(metrics: HealthMetrics): Flow<Result<Unit>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }

        val date = metrics.date.ifEmpty { today() }

        metricsCollection(userId).document(date)
            .set(metrics.copy(date = date))
            .addOnSuccessListener {
                trySend(Result.success(Unit))
                close()
            }
            .addOnFailureListener { error ->
                trySend(Result.failure(error))
                close()
            }

        awaitClose()
    }

    /**
     * Today's metrics, live. A day that has not been synced yet emits an empty day rather than
     * an error, because "nothing recorded yet" is not a failure.
     */
    fun observeHealthMetrics(): Flow<Result<HealthMetrics>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }

        val today = today()
        val registration = metricsCollection(userId).document(today)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.failure(error))
                    else -> {
                        val metrics = runCatching {
                            snapshot?.toObject(HealthMetrics::class.java)
                        }.getOrNull()
                        trySend(Result.success(metrics ?: HealthMetrics(date = today)))
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    /**
     * The seven most recent days, newest first. Ordered by the date key rather than the
     * timestamp, because the key is what makes a day unique and an ISO date sorts correctly.
     */
    fun getWeeklyHealthMetrics(): Flow<Result<List<HealthMetrics>>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }

        val registration = metricsCollection(userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(WEEK_DAYS)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> {
                        Log.w(TAG, "Weekly health metrics unavailable: ${error.message}")
                        trySend(Result.failure(error))
                    }
                    else -> {
                        val days = snapshot?.documents.orEmpty().mapNotNull { document ->
                            runCatching { document.toObject(HealthMetrics::class.java) }.getOrNull()
                        }
                        trySend(Result.success(days))
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    private fun today(): String = dateFormatter.format(Date())

    private val dateFormatter get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private companion object {
        const val TAG = "HealthRepository"
        const val COLLECTION = "healthMetrics"
        const val WEEK_DAYS = 7L
    }
}
