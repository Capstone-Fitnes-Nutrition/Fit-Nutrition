package sheridan.dheripu.fitnutrition.model

import com.google.gson.annotations.SerializedName

// ============= Core Health Metrics =============
data class HealthMetrics(
    val heartRate: Int = 0,
    val steps: Int = 0,
    val caloriesBurned: Double = 0.0,
    val distance: Double = 0.0,
    val activeMinutes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val lastSyncTime: String = "",
    val activityLevel: String = "LOW",
    val date: String = ""
) {
    constructor() : this(0, 0, 0.0, 0.0, 0, System.currentTimeMillis(), "", "LOW", "")
}

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
) {
    constructor() : this("", "", 0, "", "Bearer")
}

data class FitbitUser(
    val userId: String = "",
    val displayName: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val tokenExpiresAt: Long = 0,
    val isAuthenticated: Boolean = false
) {
    constructor() : this("", "", "", "", 0, false)
}

// ============= Activity Response =============
data class FitbitActivityResponse(
    val activities: List<Activity> = emptyList(),
    val summary: Summary = Summary()
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
        val distance: Double = 0.0,
        @SerializedName("caloriesOut")
        val caloriesBurned: Int = 0,
        @SerializedName("veryActiveMinutes")
        val activeMinutes: Int = 0
    ) {
        constructor() : this(0, 0.0, 0, 0)
    }
}

// ============= Heart Rate Response =============
data class FitbitHeartRateResponse(
    @SerializedName("activities-heart")
    val activitiesHeart: List<HeartRateData> = emptyList()
) {
    data class HeartRateData(
        val dateTime: String = "",
        val value: Value = Value()
    ) {
        data class Value(
            val restingHeartRate: Int = 0,
            val heartRateZones: List<Zone> = emptyList()
        ) {
            constructor() : this(0, emptyList())

            data class Zone(
                val name: String = "",
                val min: Int = 0,
                val max: Int = 0,
                val minutes: Int = 0,
                val caloriesOut: Double = 0.0
            )
        }
    }
}

// ============= Sleep Response =============
data class FitbitSleepResponse(
    val sleep: List<SleepData> = emptyList(),
    val summary: SleepSummary = SleepSummary()
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
    ) {
        constructor() : this(0, 0, 0)
    }
}

// ============= User Profile Response =============
data class FitbitProfileResponse(
    val user: UserProfile = UserProfile()
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
    ) {
        constructor() : this("", "", "", "", 0, 0.0, 0.0, "", "")
    }
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