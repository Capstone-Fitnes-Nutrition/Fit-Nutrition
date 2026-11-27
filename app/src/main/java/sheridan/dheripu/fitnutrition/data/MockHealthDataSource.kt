package sheridan.dheripu.fitnutrition.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.MockHealthData
import sheridan.dheripu.fitnutrition.repository.MockHealthOverrideStore
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * Stands in for Health Connect so the Health tab, the BR4/BR5 alert rules and the
 * health-driven parts of the recommendation engine can be exercised with no watch. An
 * emulator on Android 14 or later does have Health Connect, but nothing writes steps or sleep
 * to it, so a real read there comes back empty.
 *
 * Always connected and needs no permission. A day the companion control panel has set is
 * served from [overrides]; everything else falls through to the deterministic days in
 * [MockHealthData], so tests stay reproducible while a demo stays steerable.
 */
class MockHealthDataSource(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val overrides: MockHealthOverrideStore? = null
) : HealthDataSource {

    override val isConfigured: Boolean = true

    override val requiredPermissions: Set<String> = emptySet()

    override suspend fun isConnected(): Boolean = true

    override suspend fun fetchDay(date: String): Result<HealthMetrics> {
        val parsed = try {
            LocalDate.parse(date)
        } catch (_: DateTimeParseException) {
            return Result.failure(IllegalArgumentException("Not an ISO date: $date"))
        }
        val override = readOverride(date)
        return Result.success(override ?: MockHealthData.day(parsed))
    }

    override suspend fun fetchRecentDays(days: Int): List<HealthMetrics> {
        val today = LocalDate.now(zone)
        return (0 until days).map { offset ->
            val date = today.minusDays(offset.toLong())
            readOverride(date.toString()) ?: MockHealthData.day(date)
        }
    }

    /** Reason: the override is a file read, so it stays off the caller's thread. */
    private suspend fun readOverride(date: String) = overrides?.let { store ->
        withContext(Dispatchers.IO) { store.overrideFor(date) }
    }

    override fun disconnect() = Unit
}
