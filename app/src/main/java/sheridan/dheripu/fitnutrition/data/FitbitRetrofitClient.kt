package sheridan.dheripu.fitnutrition.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sheridan.dheripu.fitnutrition.BuildConfig
import java.util.concurrent.TimeUnit

object FitbitRetrofitClient {

    private const val BASE_URL = "https://api.fitbit.com/"

    // Reason: the token endpoint returns the access and refresh tokens in the response body and
    // takes the client secret in the request, so BODY logging writes live credentials to logcat.
    // Keep it to debug builds, and redact the headers that carry the bearer and basic auth.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        redactHeader("Authorization")
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: FitbitApiService = retrofit.create(FitbitApiService::class.java)
}
