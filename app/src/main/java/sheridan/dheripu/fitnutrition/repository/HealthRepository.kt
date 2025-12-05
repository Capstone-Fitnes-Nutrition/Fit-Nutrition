package sheridan.dheripu.fitnutrition.repository

import sheridan.dheripu.fitnutrition.model.HealthMetrics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for health metrics data operations
 */
class HealthRepository {

    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Save health metrics to Firebase
     */
    fun saveHealthMetrics(metrics: HealthMetrics): Flow<Result<Unit>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }

        val date = metrics.date.ifEmpty {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        val reference = database.reference
            .child("users")
            .child(userId)
            .child("health_metrics")
            .child(date)

        reference.setValue(metrics)
            .addOnSuccessListener {
                trySend(Result.success(Unit))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Result.failure(e))
                close()
            }

        awaitClose()
    }

    /**
     * Observe today's health metrics
     */
    fun observeHealthMetrics(): Flow<Result<HealthMetrics>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val reference = database.reference
            .child("users")
            .child(userId)
            .child("health_metrics")
            .child(today)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val metrics = snapshot.getValue(HealthMetrics::class.java)
                    if (metrics != null) {
                        trySend(Result.success(metrics))
                    } else {
                        trySend(Result.success(HealthMetrics(date = today)))
                    }
                } catch (e: Exception) {
                    trySend(Result.failure(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(Exception(error.message)))
            }
        }

        reference.addValueEventListener(listener)

        awaitClose {
            reference.removeEventListener(listener)
        }
    }

    /**
     * Get weekly health metrics
     */
    fun getWeeklyHealthMetrics(): Flow<Result<List<HealthMetrics>>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }

        val reference = database.reference
            .child("users")
            .child(userId)
            .child("health_metrics")
            .orderByChild("timestamp")
            .limitToLast(7)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val metrics = mutableListOf<HealthMetrics>()
                    for (child in snapshot.children) {
                        val metric = child.getValue(HealthMetrics::class.java)
                        if (metric != null) {
                            metrics.add(metric)
                        }
                    }
                    trySend(Result.success(metrics.sortedByDescending { it.timestamp }))
                } catch (e: Exception) {
                    trySend(Result.failure(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(Exception(error.message)))
            }
        }

        reference.addValueEventListener(listener)

        awaitClose {
            reference.removeEventListener(listener)
        }
    }
}
