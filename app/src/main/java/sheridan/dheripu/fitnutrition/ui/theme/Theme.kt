package sheridan.dheripu.fitnutrition.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FitNutritionColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    secondary = PrimaryBlue,
    tertiary = AccentOrange,
    background = BackgroundWhite,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
)

@Composable
fun FitNutritionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FitNutritionColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}