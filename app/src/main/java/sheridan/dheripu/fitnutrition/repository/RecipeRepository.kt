package sheridan.dheripu.fitnutrition.repository

import retrofit2.Response
import sheridan.dheripu.fitnutrition.data.RetrofitClient
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.model.RecipeFilters

/** A Spoonacular call that came back with an error status. */
class RecipeApiException(message: String) : Exception(message)

/**
 * Reads recipes from Spoonacular. When no API key is configured the offline samples in
 * [MockRecipes] are filtered locally instead, so the screens still work without a key.
 */
class RecipeRepository {

    suspend fun searchRecipes(filters: RecipeFilters): List<Recipe> {
        if (!RetrofitClient.isConfigured) {
            return MockRecipes.all.filter { it.matches(filters) }
        }

        val response = RetrofitClient.apiService.searchRecipes(
            query = filters.query,
            maxCalories = filters.maxCalories,
            minProtein = filters.minProtein,
            diet = filters.diet,
            intolerances = filters.intolerances.joinToCsv(),
            includeIngredients = filters.includeIngredients.joinToCsv(),
            excludeIngredients = filters.excludeIngredients.joinToCsv()
        )
        if (!response.isSuccessful) {
            throw RecipeApiException(response.failureMessage())
        }
        return response.body()?.results ?: emptyList()
    }

    suspend fun getRecipeById(id: Int): Recipe? {
        if (!RetrofitClient.isConfigured) {
            return MockRecipes.all.find { it.id == id }
        }

        val response = RetrofitClient.apiService.getRecipeDetails(id)
        if (response.code() == 404) {
            return null
        }
        if (!response.isSuccessful) {
            throw RecipeApiException(response.failureMessage())
        }
        return response.body()
    }

    private fun List<String>.joinToCsv(): String? =
        joinToString(",").takeIf { it.isNotEmpty() }

    private fun Response<*>.failureMessage(): String = when (code()) {
        401, 403 -> "Spoonacular rejected the API key. Check SPOONACULAR_API_KEY in " +
            "local.properties."
        402, 429 -> "Spoonacular daily quota is used up. Try again tomorrow."
        else -> "Spoonacular request failed with status ${code()}."
    }

    /** Local equivalent of the Spoonacular filters, for the offline samples. */
    private fun Recipe.matches(filters: RecipeFilters): Boolean {
        if (filters.query.isNotBlank() && !title.contains(filters.query, ignoreCase = true)) {
            return false
        }
        val diet = filters.diet
        if (diet != null && diets.none { it.equals(diet, ignoreCase = true) }) {
            return false
        }
        val calories = filters.maxCalories
        if (calories != null && nutrientAmount("Calories")?.let { it > calories } == true) {
            return false
        }
        val protein = filters.minProtein
        if (protein != null && (nutrientAmount("Protein") ?: 0.0) < protein) {
            return false
        }
        return true
    }

    private fun Recipe.nutrientAmount(name: String): Double? =
        nutrition?.nutrients?.firstOrNull { it.name.equals(name, ignoreCase = true) }?.amount
}
