package sheridan.dheripu.fitnutrition.model

data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val servings: Int,
    val sourceUrl: String?,
    val summary: String,
    val extendedIngredients: List<Ingredient>,
    val nutrition: Nutrition?,
    val diets: List<String>,
    val dishTypes: List<String>
)

data class Ingredient(
    val id: Int,
    val name: String,
    val amount: Double,
    val unit: String,
    val image: String?
)

data class Nutrition(
    val nutrients: List<Nutrient>
)

data class Nutrient(
    val name: String,
    val amount: Double,
    val unit: String
)

data class RecipeSearchResponse(
    val results: List<Recipe>,
    val offset: Int,
    val number: Int,
    val totalResults: Int
)

// Filter options
data class RecipeFilters(
    val query: String = "",
    val maxCalories: Int? = null,
    val minProtein: Int? = null,
    val diet: String? = null,
    val intolerances: List<String> = emptyList(),
    val includeIngredients: List<String> = emptyList(),
    val excludeIngredients: List<String> = emptyList()
)