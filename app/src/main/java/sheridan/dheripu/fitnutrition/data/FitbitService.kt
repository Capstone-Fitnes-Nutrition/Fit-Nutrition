package sheridan.dheripu.fitnutrition.data

import sheridan.dheripu.fitnutrition.model.*
import retrofit2.Response
import retrofit2.http.*

interface FitbitApiService {

    // ============= Activity Endpoints =============
    @GET("1/user/-/activities/date/{date}.json")
    suspend fun getDailyActivitySummary(
        @Path("date") date: String,
        @Header("Authorization") authHeader: String
    ): Response<FitbitActivityResponse>

    @GET("1/user/-/activities/date/{startDate}/{endDate}.json")
    suspend fun getActivityRange(
        @Path("startDate") startDate: String,
        @Path("endDate") endDate: String,
        @Header("Authorization") authHeader: String
    ): Response<FitbitActivityResponse>

    // ============= Heart Rate Endpoints =============
    @GET("1/user/-/activities/heart/date/{date}/1d.json")
    suspend fun getHeartRateData(
        @Path("date") date: String,
        @Header("Authorization") authHeader: String
    ): Response<FitbitHeartRateResponse>

    @GET("1/user/-/activities/heart/date/{startDate}/{endDate}.json")
    suspend fun getHeartRateRange(
        @Path("startDate") startDate: String,
        @Path("endDate") endDate: String,
        @Header("Authorization") authHeader: String
    ): Response<FitbitHeartRateResponse>

    // ============= Sleep Endpoints =============
    @GET("1.2/user/-/sleep/date/{date}.json")
    suspend fun getSleepData(
        @Path("date") date: String,
        @Header("Authorization") authHeader: String
    ): Response<FitbitSleepResponse>

    @GET("1.2/user/-/sleep/date/{startDate}/{endDate}.json")
    suspend fun getSleepRange(
        @Path("startDate") startDate: String,
        @Path("endDate") endDate: String,
        @Header("Authorization") authHeader: String
    ): Response<FitbitSleepResponse>

    // ============= User Profile Endpoints =============
    @GET("1/user/-/profile.json")
    suspend fun getUserProfile(
        @Header("Authorization") authHeader: String
    ): Response<FitbitProfileResponse>

    // ============= Steps Endpoints =============
    @GET("1/user/-/activities/steps/date/{date}/1d.json")
    suspend fun getDailySteps(
        @Path("date") date: String,
        @Header("Authorization") authHeader: String
    ): Response<Map<String, Any>>

    // ============= Token Endpoints =============
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun getAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String,
        @Field("redirect_uri") redirectUri: String
    ): Response<FitbitAuthResponse>

    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun refreshAccessToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String
    ): Response<FitbitAuthResponse>
}