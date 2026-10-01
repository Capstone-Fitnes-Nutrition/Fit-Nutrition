package sheridan.dheripu.fitnutrition.model

/** A meal saved by a user and used to calculate their nutrition analytics. */
data class NutritionLog(
    val id: String = "",
    val recipeId: Int = 0,
    val mealName: String = "",
    val calories: Int = 0,
    val proteinGrams: Double = 0.0,
    val carbohydratesGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val loggedAt: Long = System.currentTimeMillis()
)
