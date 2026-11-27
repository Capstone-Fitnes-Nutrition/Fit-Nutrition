package sheridan.dheripu.fitnutrition.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.ActivityLevel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Reads daily metrics from Health Connect, Android's on-device health store.
 *
 * This is where a Fitbit's data arrives now: the watch syncs to the Google Health app, which
 * writes steps, sleep, heart rate and workouts into Health Connect, and we read them from
 * there. Any other app that writes to Health Connect feeds this just the same, which is the
 * point of reading the platform store rather than one vendor's API.
 *
 * Everything is aggregated per calendar day in the device's own time zone, so a day here means
 * the same thing it means in [HealthMetrics.date] and in the Realtime Database keys.
 */
class HealthConnectDataSource(
    private val context: Context,
    private val zone: ZoneId = ZoneId.systemDefault()
) : HealthDataSource {

    /**
     * Created lazily, and only when the platform is actually there. [HealthConnectClient.getOrCreate]
     * throws on a device with no provider, which is every emulator without the Health Connect
     * app installed.
     */
    private val client: HealthConnectClient? by lazy {
        if (!isConfigured) null else runCatching { HealthConnectClient.getOrCreate(context) }
            .onFailure { Log.w(TAG, "Health Connect unavailable: ${it.message}") }
            .getOrNull()
    }

    override val isConfigured: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    /**
     * Steps alone is enough to be useful, so a user who granted only that is connected. The
     * other reads degrade to zero on their own.
     */
    override suspend fun isConnected(): Boolean {
        val granted = grantedPermissions() ?: return false
        return HealthPermission.getReadPermission(StepsRecord::class) in granted
    }

    override suspend fun fetchDay(date: String): Result<HealthMetrics> {
        val day = try {
            LocalDate.parse(date, dateFormatter)
        } catch (_: DateTimeParseException) {
            return Result.failure(IllegalArgumentException("Not an ISO date: $date"))
        }
        val client = client
            ?: return Result.failure(Exception(NOT_AVAILABLE))
        if (!isConnected()) return Result.failure(Exception(NOT_PERMITTED))

        val range = TimeRangeFilter.between(day.atStartOfDay(), day.plusDays(1).atStartOfDay())
        val totals = try {
            client.aggregate(AggregateRequest(metrics = AGGREGATES, timeRangeFilter = range))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Health Connect read failed for $date", error)
            return Result.failure(Exception("Could not read Health Connect data.", error))
        }

        return Result.success(metricsFrom(day, totals))
    }

    /**
     * A day Health Connect has nothing for is left out rather than stored as zeroes, so the
     * alert rules do not read a gap as a day of no sleep.
     */
    override suspend fun fetchRecentDays(days: Int): List<HealthMetrics> {
        val today = LocalDate.now(zone)
        return (0 until days).mapNotNull { offset ->
            fetchDay(dateFormatter.format(today.minusDays(offset.toLong()))).getOrNull()
                ?.takeIf { it.steps > 0 || it.sleepMinutes > 0 || it.heartRate > 0 }
        }
    }

    /** Nothing is cached here; the user revokes access in the Health Connect settings. */
    override fun disconnect() = Unit

    private suspend fun grantedPermissions(): Set<String>? = try {
        client?.permissionController?.getGrantedPermissions()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w(TAG, "Could not read Health Connect permissions: ${error.message}")
        null
    }

    private fun metricsFrom(day: LocalDate, totals: AggregationResult): HealthMetrics {
        val steps = (totals[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
        val activeMinutes =
            (totals[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes() ?: 0L).toInt()
        return HealthMetrics(
            date = dateFormatter.format(day),
            steps = steps,
            distance = totals[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0,
            calories = (totals[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0)
                .toInt(),
            heartRate = (totals[RestingHeartRateRecord.BPM_AVG] ?: 0L).toInt(),
            activeMinutes = activeMinutes,
            sleepMinutes =
                (totals[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes() ?: 0L).toInt(),
            lastSyncTime = syncFormatter.format(LocalDate.now(zone)),
            activityLevel = ActivityLevel.from(steps, activeMinutes),
            timestamp = System.currentTimeMillis()
        )
    }

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val syncFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    companion object {
        private const val TAG = "HealthConnectSource"

        const val NOT_AVAILABLE =
            "Health Connect is not available on this device. Install it from Play, or switch " +
                "on sample data."
        const val NOT_PERMITTED = "Grant Health Connect access to read your daily metrics."

        private val AGGREGATES = setOf(
            StepsRecord.COUNT_TOTAL,
            DistanceRecord.DISTANCE_TOTAL,
            TotalCaloriesBurnedRecord.ENERGY_TOTAL,
            RestingHeartRateRecord.BPM_AVG,
            SleepSessionRecord.SLEEP_DURATION_TOTAL,
            ExerciseSessionRecord.EXERCISE_DURATION_TOTAL
        )
    }
}
