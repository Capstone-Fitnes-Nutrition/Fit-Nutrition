package sheridan.dheripu.fitnutrition.data

import sheridan.dheripu.fitnutrition.model.*
import retrofit2.Response
import retrofit2.http.*

interface FitbitApiService {

    // ============= Activity APIs =============

    @GET("1/user/-/activities/date/{date}.json")
    suspend fun getDailyActivitySummary(
        @Path("date") date: String,
        @Header("Authorization") authHeader: String
    ): Response<FitbitActivityResponse>

    @GET("1/user/-/activities/heart/date/{date}/1d.json")
    suspend fun getHeartRateData(
        @Path("date") date: String,
        @Header("Authorization") authHeader: String
    ): Response<FitbitHeartRateResponse>

    @GET("1.2/user/-/sleep/date/{date}.json")
    suspend fun getSleepData(
        @Path("date") date: String,
        @Header("Authorization") authHeader: String
    ): Response<FitbitSleepResponse>

    @GET("1/user/-/profile.json")
    suspend fun getUserProfile(
        @Header("Authorization") authHeader: String
    ): Response<FitbitProfileResponse>

    // ============= OAuth Token APIs =============

    /**
     * Exchange authorization code for access token
     * REQUIRES: Basic Authorization header with Base64(client_id:client_secret)
     */
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun getAccessToken(
        @Header("Authorization") basicAuth: String,
        @Field("client_id") clientId: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String,
        @Field("redirect_uri") redirectUri: String
    ): Response<FitbitAuthResponse>

    /**
     * Refresh an expired access token
     * REQUIRES: Basic Authorization header with Base64(client_id:client_secret)
     */
    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun refreshAccessToken(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String
    ): Response<FitbitAuthResponse>
}
