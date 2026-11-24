package sheridan.dheripu.fitnutrition.data

import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.MockHealthData
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * Stands in for a Fitbit account so the Health tab, the BR4/BR5 alert rules and the
 * health-driven parts of the recommendation engine can be exercised with no account and no
 * watch. Always connected, needs no authorization, and serves the deterministic days from
 * [MockHealthData].
 */
class MockFitbitDataSource(
    private val zone: ZoneId = ZoneId.systemDefault()
) : FitbitDataSource {

    override val isConfigured: Boolean = true

    override fun isConnected(): Boolean = true

    override fun authorizationUrl(): String = ""

    override suspend fun completeAuthorization(code: String, state: String?): Result<Unit> =
        Result.success(Unit)

    override suspend fun fetchDay(date: String): Result<HealthMetrics> {
        val parsed = try {
            LocalDate.parse(date)
        } catch (_: DateTimeParseException) {
            return Result.failure(IllegalArgumentException("Not an ISO date: $date"))
        }
        return Result.success(MockHealthData.day(parsed))
    }

    override suspend fun fetchRecentDays(days: Int): List<HealthMetrics> =
        MockHealthData.recentDays(LocalDate.now(zone), days)

    override fun disconnect() = Unit
}
