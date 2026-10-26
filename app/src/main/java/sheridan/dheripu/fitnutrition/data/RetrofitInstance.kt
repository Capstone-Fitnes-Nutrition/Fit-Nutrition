package sheridan.dheripu.fitnutrition.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import sheridan.dheripu.fitnutrition.BuildConfig

object RetrofitInstance {

    // Reason: the key comes from local.properties (or the environment) so it stays out of git.
    private val apiKey = BuildConfig.EXERCISE_API_KEY

    /** False when EXERCISE_API_KEY is missing, so callers can explain the failure. */
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-RapidAPI-Key", apiKey)
            .addHeader("X-RapidAPI-Host", BuildConfig.EXERCISE_API_HOST)
            .build()
        chain.proceed(request)
    }.build()

    val api: ExerciseApiService by lazy{
        Retrofit.Builder()
            .baseUrl("https://${BuildConfig.EXERCISE_API_HOST}/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExerciseApiService::class.java)
    }
}