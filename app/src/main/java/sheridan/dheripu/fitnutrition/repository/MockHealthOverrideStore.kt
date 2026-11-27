package sheridan.dheripu.fitnutrition.repository

import android.content.Context
import android.util.Log
import org.json.JSONObject
import sheridan.dheripu.fitnutrition.BuildConfig
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.ActivityLevel
import java.io.File

/**
 * Reads the demo overrides the companion control panel drops into the app's own external files
 * directory, one JSON file per day: `files/mock_health/{yyyy-MM-dd}.json`.
 *
 * A file rather than a database on purpose. The panel used to write to Firestore, which meant
 * signing in as the account being demoed, and whoever is running the demo does not necessarily
 * know that password — the account signed into the emulator is often one nobody remembers
 * creating. `adb push` needs no credentials and does not care which account is signed in, so
 * the panel now drives whatever install is in front of you.
 *
 * Debug builds only. A release build ignores the directory even if something put files there.
 */
class MockHealthOverrideStore(context: Context) {

    private val directory: File? =
        if (BuildConfig.DEBUG) context.getExternalFilesDir(DIRECTORY) else null

    /** The override for [date], or null when the panel has not set one. */
    fun overrideFor(date: String): HealthMetrics? {
        val file = directory?.resolve("$date.json")?.takeIf { it.isFile } ?: return null
        return try {
            file.readText().toMetrics(date)
        } catch (error: Exception) {
            Log.w(TAG, "Could not read the mock override for $date: ${error.message}")
            null
        }
    }

    /**
     * The panel only has to send the numbers. The date comes from the file name, and the
     * activity level is derived here so the rules can never be fed a level that contradicts
     * the steps it sits next to.
     */
    private fun String.toMetrics(date: String): HealthMetrics {
        val json = JSONObject(this)
        val steps = json.optInt("steps")
        val activeMinutes = json.optInt("activeMinutes")
        return HealthMetrics(
            date = date,
            steps = steps,
            distance = json.optDouble("distance", 0.0),
            calories = json.optInt("calories"),
            heartRate = json.optInt("heartRate"),
            activeMinutes = activeMinutes,
            sleepMinutes = json.optInt("sleepMinutes"),
            lastSyncTime = "Set from the demo panel",
            activityLevel = ActivityLevel.from(steps, activeMinutes),
            timestamp = System.currentTimeMillis()
        )
    }

    private companion object {
        const val TAG = "MockHealthOverride"
        const val DIRECTORY = "mock_health"
    }
}
