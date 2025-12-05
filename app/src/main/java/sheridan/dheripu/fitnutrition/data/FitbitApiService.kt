package sheridan.dheripu.fitnutrition.data

import sheridan.dheripu.fitnutrition.model.*
import retrofit2.Response
import retrofit2.http.*

interface FitbitApiService {

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
