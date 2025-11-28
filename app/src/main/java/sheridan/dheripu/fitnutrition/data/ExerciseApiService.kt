package sheridan.dheripu.fitnutrition.data

import retrofit2.http.GET
import retrofit2.http.Path
import sheridan.dheripu.fitnutrition.model.Exercise
import retrofit2.Call

interface ExerciseApiService {
    @GET("exercises/bodyPart/{bodyPart}")
    fun getExercisesByBodyPart(@Path("bodyPart") bodyPart: String):
            Call<List<Exercise>>
}