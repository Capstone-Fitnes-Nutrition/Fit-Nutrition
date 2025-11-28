package sheridan.dheripu.fitnutrition.model

data class Exercise (
    val id: String,
    val name: String,
    val bodyPart: String,
    val target: String,
    val instructions: List<String>,
)

