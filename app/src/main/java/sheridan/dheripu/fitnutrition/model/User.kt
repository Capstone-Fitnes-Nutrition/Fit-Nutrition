package sheridan.dheripu.fitnutrition.model

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val weight: String = "",
    val height: String = "",
    val fitnessGoal: String = "Weight Loss"
)