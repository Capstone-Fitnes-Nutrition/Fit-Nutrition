package sheridan.dheripu.fitnutrition.repository

import sheridan.dheripu.fitnutrition.data.RetrofitClient
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.model.RecipeFilters

class RecipeRepository {
    
    suspend fun searchRecipes(filters: RecipeFilters): List<Recipe> {
        return try {
            val response = RetrofitClient.apiService.searchRecipes(
                apiKey = "80195b40cdad49118dd62a074d9901c0",
                query = filters.query,
                maxCalories = filters.maxCalories,
                minProtein = filters.minProtein,
                diet = filters.diet,
                intolerances = filters.intolerances.joinToString(",").takeIf { it.isNotEmpty() },
                includeIngredients = filters.includeIngredients.joinToString(",").takeIf { it.isNotEmpty() },
                excludeIngredients = filters.excludeIngredients.joinToString(",").takeIf { it.isNotEmpty() }
            )
            
            if (response.isSuccessful) {
                response.body()?.results ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getRecipeById(id: Int): Recipe? {
        return try {
            val response = RetrofitClient.apiService.getRecipeDetails(id)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}