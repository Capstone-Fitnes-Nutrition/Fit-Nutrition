package sheridan.dheripu.fitnutrition.model

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val weight: String = "",
    val height: String = "",
    val fitnessGoal: String = "",
    val activityLevel: String = "",
    val dailyCalorieTarget: String = "",
    val dailyProteinTarget: String = "",
    val dietaryPreference: String = "",
    val dietaryRestrictions: List<String> = emptyList(),
    val availableEquipment: List<String> = emptyList()
)
