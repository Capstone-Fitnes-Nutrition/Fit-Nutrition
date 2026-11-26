package sheridan.dheripu.fitnutrition.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.ActivityLevel
import kotlin.coroutines.resume

/**
 * Reads the demo overrides the companion control panel writes to
 * users/{uid}/mock_health/{yyyy-MM-dd}.
 *
 * This is what makes the mock source demonstrable rather than merely deterministic: the panel
 * sets a day's steps, sleep and resting heart rate in a browser, and the next sync shows them,
 * so the BR4 and BR5 alerts can be triggered in front of an audience instead of described.
 * Nothing is written from the app — the override is read-only here, and a day with no override
 * falls through to the generated data.
 */
class MockHealthOverrideRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    /** The override for [date], or null when the panel has not set one. */
    suspend fun overrideFor(date: String): HealthMetrics? {
        val userId = auth.currentUser?.uid ?: return null
        val reference = database.reference
            .child("users")
            .child(userId)
            .child(NODE)
            .child(date)

        return try {
            suspendCancellableCoroutine { continuation ->
                reference.get()
                    .addOnSuccessListener { snapshot ->
                        val stored = runCatching {
                            snapshot.getValue(HealthMetrics::class.java)
                        }.getOrNull()
                        continuation.resume(stored?.normalised(date))
                    }
                    .addOnFailureListener { error ->
                        Log.w(TAG, "No mock override for $date: ${error.message}")
                        continuation.resume(null)
                    }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Could not read the mock override for $date: ${error.message}")
            null
        }
    }

    /**
     * The panel only has to send the numbers. The date is the key it was stored under, and the
     * activity level is derived here so the rules can never be fed a level that contradicts the
     * steps it sits next to.
     */
    private fun HealthMetrics.normalised(date: String): HealthMetrics = copy(
        date = date,
        activityLevel = ActivityLevel.from(steps, activeMinutes),
        lastSyncTime = lastSyncTime.ifBlank { "Set from the demo panel" }
    )

    private companion object {
        const val TAG = "MockHealthOverride"
        const val NODE = "mock_health"
    }
}
