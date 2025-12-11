package sheridan.dheripu.fitnutrition.repository

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import sheridan.dheripu.fitnutrition.data.RetrofitInstance
import sheridan.dheripu.fitnutrition.model.Exercise

class FitnessRepository {

    fun getExercisesByBodyPart(bodyPart: String, onSuccess: (List<Exercise>) -> Unit, onError: (String) -> Unit) {
        if (bodyPart.isBlank()) {
            onError("Body part cannot be empty")
            return
        }

        val call = RetrofitInstance.api.getExercisesByBodyPart(bodyPart.lowercase())

        call.enqueue(object : Callback<List<Exercise>> {
            override fun onResponse(call: Call<List<Exercise>>, response: Response<List<Exercise>>) {
                if (response.isSuccessful) {
                    val exercises = response.body() ?: emptyList()
                    onSuccess(exercises)
                } else if (response.code() == 404 || response.code() == 422) {
                    onError("No exercises found for \"$bodyPart\".")
                }
                else {
                    onError("Error ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Exercise>>, t: Throwable) {
                onError("Failed to fetch any exercises. Something might be wrong with wifi")
            }
        })
    }
}