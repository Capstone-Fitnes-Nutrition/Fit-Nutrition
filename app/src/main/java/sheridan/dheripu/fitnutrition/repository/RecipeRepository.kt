package sheridan.dheripu.fitnutrition.repository

import sheridan.dheripu.fitnutrition.data.RetrofitClient
import sheridan.dheripu.fitnutrition.model.Ingredient
import sheridan.dheripu.fitnutrition.model.Nutrient
import sheridan.dheripu.fitnutrition.model.Nutrition
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.model.RecipeFilters
//
//class RecipeRepository {
//
//    suspend fun searchRecipes(filters: RecipeFilters): List<Recipe> {
//        return try {
//            val response = RetrofitClient.apiService.searchRecipes(
//                apiKey = "80195b40cdad49118dd62a074d9901c0",
//                query = filters.query,
//                maxCalories = filters.maxCalories,
//                minProtein = filters.minProtein,
//                diet = filters.diet,
//                intolerances = filters.intolerances.joinToString(",").takeIf { it.isNotEmpty() },
//                includeIngredients = filters.includeIngredients.joinToString(",").takeIf { it.isNotEmpty() },
//                excludeIngredients = filters.excludeIngredients.joinToString(",").takeIf { it.isNotEmpty() }
//            )
//
//            if (response.isSuccessful) {
//                response.body()?.results ?: emptyList()
//            } else {
//                emptyList()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            emptyList()
//        }
//    }
//
//    suspend fun getRecipeById(id: Int): Recipe? {
//        return try {
//            val response = RetrofitClient.apiService.getRecipeDetails(id)
//            if (response.isSuccessful) {
//                response.body()
//            } else {
//                null
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    }
//}

class RecipeRepository {

    suspend fun searchRecipes(filters: RecipeFilters): List<Recipe> {
        println("DEBUG: Using mock data (API quota exceeded)")

        // Simulate API delay
        kotlinx.coroutines.delay(1000)

        // Return mock recipe data
        return createMockRecipes().filter { recipe ->
            // Apply filters to mock data
            var matches = true

            // Filter by query
            if (filters.query.isNotBlank()) {
                matches = matches && recipe.title.contains(filters.query, ignoreCase = true)
            }

            // Filter by diet
            if (filters.diet != null) {
                matches = matches && recipe.diets.any {
                    it.equals(filters.diet!!, ignoreCase = true)
                }
            }

            matches
        }
    }

    private fun createMockRecipes(): List<Recipe> {
        return listOf(
            Recipe(
                id = 1,
                title = "Vegetarian Pasta Primavera",
                image = "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=400&h=300&fit=crop",
                readyInMinutes = 25,
                servings = 4,
                sourceUrl = "https://example.com/pasta",
                summary = "A fresh and colorful pasta dish loaded with seasonal vegetables. This vegetarian recipe is perfect for a quick weeknight dinner.",
                extendedIngredients = listOf(
                    Ingredient(1, "pasta", 200.0, "g", "pasta.jpg"),
                    Ingredient(2, "bell peppers", 2.0, "pieces", "bell-pepper.jpg"),
                    Ingredient(3, "zucchini", 1.0, "piece", "zucchini.jpg"),
                    Ingredient(4, "cherry tomatoes", 150.0, "g", "tomato.jpg"),
                    Ingredient(5, "garlic", 3.0, "cloves", "garlic.jpg")
                ),
                nutrition = Nutrition(
                    listOf(
                        Nutrient("Calories", 350.0, "kcal"),
                        Nutrient("Protein", 12.0, "g"),
                        Nutrient("Carbohydrates", 60.0, "g"),
                        Nutrient("Fat", 8.0, "g")
                    )
                ),
                diets = listOf("vegetarian", "vegan"),
                dishTypes = listOf("main course", "pasta")
            ),
            Recipe(
                id = 2,
                title = "Grilled Chicken Salad",
                image = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=300&fit=crop",
                readyInMinutes = 20,
                servings = 2,
                sourceUrl = "https://example.com/salad",
                summary = "A protein-packed salad with grilled chicken, fresh greens, and a light lemon vinaigrette. Perfect for a healthy lunch.",
                extendedIngredients = listOf(
                    Ingredient(1, "chicken breast", 300.0, "g", "chicken.jpg"),
                    Ingredient(2, "mixed greens", 150.0, "g", "lettuce.jpg"),
                    Ingredient(3, "cherry tomatoes", 100.0, "g", "tomato.jpg"),
                    Ingredient(4, "cucumber", 1.0, "piece", "cucumber.jpg"),
                    Ingredient(5, "lemon", 1.0, "piece", "lemon.jpg")
                ),
                nutrition = Nutrition(
                    listOf(
                        Nutrient("Calories", 280.0, "kcal"),
                        Nutrient("Protein", 35.0, "g"),
                        Nutrient("Carbohydrates", 10.0, "g"),
                        Nutrient("Fat", 12.0, "g")
                    )
                ),
                diets = listOf("gluten free"),
                dishTypes = listOf("salad", "lunch")
            ),
            Recipe(
                id = 3,
                title = "Vegetable Stir Fry",
                image = "https://images.unsplash.com/photo-1546069901-d5bfd2cbfb1f?w=400&h=300&fit=crop",
                readyInMinutes = 15,
                servings = 3,
                sourceUrl = "https://example.com/stirfry",
                summary = "Quick and healthy vegetable stir fry with tofu. Packed with nutrients and ready in under 15 minutes.",
                extendedIngredients = listOf(
                    Ingredient(1, "tofu", 200.0, "g", "tofu.jpg"),
                    Ingredient(2, "broccoli", 150.0, "g", "broccoli.jpg"),
                    Ingredient(3, "carrots", 2.0, "pieces", "carrot.jpg"),
                    Ingredient(4, "soy sauce", 30.0, "ml", "soy-sauce.jpg"),
                    Ingredient(5, "ginger", 10.0, "g", "ginger.jpg")
                ),
                nutrition = Nutrition(
                    listOf(
                        Nutrient("Calories", 220.0, "kcal"),
                        Nutrient("Protein", 18.0, "g"),
                        Nutrient("Carbohydrates", 25.0, "g"),
                        Nutrient("Fat", 7.0, "g")
                    )
                ),
                diets = listOf("vegetarian", "vegan", "gluten free"),
                dishTypes = listOf("main course", "asian")
            ),
            Recipe(
                id = 4,
                title = "Greek Yogurt Parfait",
                image = "https://images.unsplash.com/photo-1488477181946-6428a0291777?w=400&h=300&fit=crop",
                readyInMinutes = 5,
                servings = 1,
                sourceUrl = "https://example.com/parfait",
                summary = "A quick and healthy breakfast with Greek yogurt, fresh berries, and granola. High in protein and antioxidants.",
                extendedIngredients = listOf(
                    Ingredient(1, "greek yogurt", 200.0, "g", "yogurt.jpg"),
                    Ingredient(2, "mixed berries", 100.0, "g", "berries.jpg"),
                    Ingredient(3, "granola", 50.0, "g", "granola.jpg"),
                    Ingredient(4, "honey", 15.0, "ml", "honey.jpg")
                ),
                nutrition = Nutrition(
                    listOf(
                        Nutrient("Calories", 320.0, "kcal"),
                        Nutrient("Protein", 25.0, "g"),
                        Nutrient("Carbohydrates", 45.0, "g"),
                        Nutrient("Fat", 6.0, "g")
                    )
                ),
                diets = listOf("vegetarian"),
                dishTypes = listOf("breakfast", "snack")
            ),
            Recipe(
                id = 5,
                title = "Salmon with Roasted Vegetables",
                image = "https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=400&h=300&fit=crop",
                readyInMinutes = 30,
                servings = 2,
                sourceUrl = "https://example.com/salmon",
                summary = "Healthy baked salmon served with a medley of roasted seasonal vegetables. Rich in omega-3 fatty acids.",
                extendedIngredients = listOf(
                    Ingredient(1, "salmon fillet", 400.0, "g", "salmon.jpg"),
                    Ingredient(2, "asparagus", 200.0, "g", "asparagus.jpg"),
                    Ingredient(3, "sweet potato", 2.0, "pieces", "sweet-potato.jpg"),
                    Ingredient(4, "lemon", 1.0, "piece", "lemon.jpg"),
                    Ingredient(5, "olive oil", 15.0, "ml", "olive-oil.jpg")
                ),
                nutrition = Nutrition(
                    listOf(
                        Nutrient("Calories", 420.0, "kcal"),
                        Nutrient("Protein", 38.0, "g"),
                        Nutrient("Carbohydrates", 30.0, "g"),
                        Nutrient("Fat", 18.0, "g")
                    )
                ),
                diets = listOf("gluten free"),
                dishTypes = listOf("main course", "dinner")
            )
        )
    }

    suspend fun getRecipeById(id: Int): Recipe? {
        // Find mock recipe by ID
        return createMockRecipes().find { it.id == id }
    }
}