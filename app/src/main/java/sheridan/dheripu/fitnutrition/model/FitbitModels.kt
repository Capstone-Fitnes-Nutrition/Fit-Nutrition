package sheridan.dheripu.fitnutrition.model

import com.google.gson.annotations.SerializedName

// ============= Core Health Metrics =============
data class HealthMetrics(
    val heartRate: Int = 0,
    val steps: Int = 0,
    val calories: Int = 0,
    val distance: Double = 0.0,
    val activeMinutes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val lastSyncTime: String = "",
    val activityLevel: String = "LOW",
    val date: String = ""
)


// ============= Fitbit Authentication =============
data class FitbitAuthResponse(
    @SerializedName("access_token")
    val accessToken: String = "",
    @SerializedName("refresh_token")
    val refreshToken: String = "",
    @SerializedName("expires_in")
    val expiresIn: Long = 0,
    @SerializedName("user_id")
    val userId: String = "",
    @SerializedName("token_type")
    val tokenType: String = "Bearer"
)

data class FitbitUser(
    val userId: String = "",
    val displayName: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val tokenExpiresAt: Long = 0,
    val isAuthenticated: Boolean = false
)

// ============= Activity Response =============
data class FitbitActivityResponse(
    val activities: List<Activity> = emptyList(),
    val summary: Summary? = null
) {
    data class Activity(
        val activityId: Int = 0,
        val activityName: String = "",
        val calories: Int = 0,
        val duration: Int = 0,
        val startTime: String = ""
    )

    data class Summary(
        val steps: Int = 0,
        val distances: List<Distance> = emptyList(),
        @SerializedName("caloriesOut")
        val caloriesOut: Int = 0,
        @SerializedName("veryActiveMinutes")
        val veryActiveMinutes: Int = 0,
        @SerializedName("fairlyActiveMinutes")
        val fairlyActiveMinutes: Int = 0,
        @SerializedName("lightlyActiveMinutes")
        val lightlyActiveMinutes: Int = 0
    )

    data class Distance(
        val activity: String = "",
        val distance: Double = 0.0
    )
}

// ============= Heart Rate Response =============
data class FitbitHeartRateResponse(
    @SerializedName("activities-heart")
    val activitiesHeart: List<HeartRateData>? = null
) {
    data class HeartRateData(
        val dateTime: String = "",
        val value: Value? = null
    ) {
        data class Value(
            val restingHeartRate: Int = 0,
            val heartRateZones: List<Zone> = emptyList()
        )

        data class Zone(
            val name: String = "",
            val min: Int = 0,
            val max: Int = 0,
            val minutes: Int = 0,
            val caloriesOut: Double = 0.0
        )
    }
}

// ============= Sleep Response =============
data class FitbitSleepResponse(
    val sleep: List<SleepData> = emptyList(),
    val summary: SleepSummary? = null
) {
    data class SleepData(
        val dateOfSleep: String = "",
        val duration: Long = 0,
        val efficiency: Int = 0,
        val endTime: String = "",
        val startTime: String = ""
    )

    data class SleepSummary(
        val totalMinutesAsleep: Long = 0,
        val totalSleepRecords: Int = 0,
        val totalTimeInBed: Long = 0
    )
}

// ============= User Profile Response =============
data class FitbitProfileResponse(
    val user: UserProfile? = null
) {
    data class UserProfile(
        @SerializedName("encodedId")
        val userId: String = "",
        val fullName: String = "",
        val displayName: String = "",
        val avatar: String = "",
        val age: Int = 0,
        val weight: Double = 0.0,
        val height: Double = 0.0,
        val gender: String = "",
        val aboutMe: String = ""
    )
}

// ============= Error Response =============
data class FitbitErrorResponse(
    val errors: List<ErrorDetail> = emptyList()
) {
    data class ErrorDetail(
        val errorType: String = "",
        val message: String = ""
    )
}
