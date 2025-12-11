package sheridan.dheripu.fitnutrition.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-RapidAPI-Key", "192886c2e0mshf67522f464867ebp14f9e6jsn27c4a67345a9")
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