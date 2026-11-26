package sheridan.dheripu.fitnutrition.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The scheme only defined nine roles, so every container and variant role the screens use
 * fell through to Material's baseline defaults, which are purple. Filling the set out is what
 * makes a card read as FitNutrition green rather than stock Material.
 */
private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = NeutralLightSurface,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Blue40,
    onSecondary = NeutralLightSurface,
    secondaryContainer = Blue90,
    onSecondaryContainer = Blue10,
    tertiary = Orange40,
    onTertiary = NeutralLightSurface,
    tertiaryContainer = Orange90,
    onTertiaryContainer = Orange10,
    error = ErrorLight,
    onError = NeutralLightSurface,
    errorContainer = ErrorLightContainer,
    onErrorContainer = ErrorLightOnContainer,
    background = NeutralLightBackground,
    onBackground = NeutralLightOn,
    surface = NeutralLightSurface,
    onSurface = NeutralLightOn,
    surfaceVariant = NeutralLightVariant,
    onSurfaceVariant = NeutralLightOnVariant,
    outline = NeutralLightOutline,
    outlineVariant = NeutralLightOutlineVariant
)

/**
 * Dark is a scheme in its own right, not an inversion. The hues keep their identity by moving
 * up the tonal ramp: a light green on a dark surface, with the dark tones carrying the text.
 */
private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Blue80,
    onSecondary = Blue20,
    secondaryContainer = Blue30,
    onSecondaryContainer = Blue90,
    tertiary = Orange80,
    onTertiary = Orange20,
    tertiaryContainer = Orange30,
    onTertiaryContainer = Orange90,
    error = ErrorDark,
    onError = ErrorDarkOn,
    errorContainer = ErrorDarkContainer,
    onErrorContainer = ErrorDarkOnContainer,
    background = NeutralDarkBackground,
    onBackground = NeutralDarkOn,
    surface = NeutralDarkSurface,
    onSurface = NeutralDarkOn,
    surfaceVariant = NeutralDarkVariant,
    onSurfaceVariant = NeutralDarkOnVariant,
    outline = NeutralDarkOutline,
    outlineVariant = NeutralDarkOutlineVariant
)

/**
 * [darkTheme] follows the system setting by default. Dynamic Color is deliberately not used:
 * the app's identity is its own green, and deriving the scheme from the device wallpaper would
 * recolour it per phone. Typography stays on the Material type scale, which is what keeps text
 * following the system font-size setting.
 */
@Composable
fun FitNutritionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
