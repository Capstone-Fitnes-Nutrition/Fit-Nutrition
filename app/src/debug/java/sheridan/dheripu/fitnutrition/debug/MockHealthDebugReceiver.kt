package sheridan.dheripu.fitnutrition.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

/**
 * Lets the companion demo panel set a day's health metrics over adb, with no sign-in.
 *
 * Debug builds only — this file lives in `src/debug`, so it is not compiled into a release
 * build and the receiver is not in the release manifest.
 *
 * The app writes the file rather than `adb push`ing it because a pushed file is owned by the
 * shell user and the app cannot read it back. A broadcast sidesteps that: the app does the
 * write, so the app owns the file. It lands in the external files directory, which the panel
 * can still read to show what a day is already set to.
 *
 *     adb shell am broadcast -n sheridan.dheripu.fitnutrition/.debug.MockHealthDebugReceiver \
 *       -a sheridan.dheripu.fitnutrition.SET_MOCK_DAY \
 *       --es date 2026-11-26 --ei steps 14250 --ei heartRate 108
 */
class MockHealthDebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val date = intent.getStringExtra("date")?.takeIf { it.matches(ISO_DATE) } ?: run {
            Log.w(TAG, "Ignoring a mock day with no usable date")
            return
        }

        val directory = context.getExternalFilesDir(DIRECTORY) ?: run {
            Log.w(TAG, "No external files directory on this device")
            return
        }
        directory.mkdirs()
        val file = directory.resolve("$date.json")

        if (intent.getBooleanExtra("clear", false)) {
            val removed = file.delete()
            Log.d(TAG, "Cleared $date: $removed")
            return
        }

        val day = JSONObject().apply {
            for (field in NUMERIC_FIELDS) {
                if (intent.hasExtra(field)) put(field, intent.getIntExtra(field, 0))
            }
            if (intent.hasExtra("distance")) put("distance", intent.getFloatExtra("distance", 0f))
        }

        try {
            file.writeText(day.toString())
            Log.d(TAG, "Set $date")
        } catch (error: Exception) {
            Log.w(TAG, "Could not write $date: ${error.message}")
        }
    }

    private companion object {
        const val TAG = "MockHealthDebug"
        const val ACTION = "sheridan.dheripu.fitnutrition.SET_MOCK_DAY"
        const val DIRECTORY = "mock_health"
        val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
        val NUMERIC_FIELDS = listOf(
            "steps", "activeMinutes", "heartRate", "sleepMinutes", "calories"
        )
    }
}
