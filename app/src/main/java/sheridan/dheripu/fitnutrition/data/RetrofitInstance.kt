package sheridan.dheripu.fitnutrition.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-RapidAPI-Key", "978d400a3amsh7cce0b13181bf48p177261jsn6990a1cd42cb")
            .addHeader("X-RapidAPI-Host", "exercisedb.p.rapidapi.com")
            .build()
        chain.proceed(request)
    }.build()

    val api: ExerciseApiService by lazy{
        Retrofit.Builder()
            .baseUrl("https://exercisedb.p.rapidapi.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExerciseApiService::class.java)
    }
}