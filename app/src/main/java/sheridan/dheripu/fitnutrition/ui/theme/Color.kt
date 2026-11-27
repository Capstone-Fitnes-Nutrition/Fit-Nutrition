package sheridan.dheripu.fitnutrition.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Material 3 colour roles for the FitNutrition scheme.
 *
 * The brand is the green, blue and orange the app has always used. Those three are the hue
 * anchors; each is given the tones Material needs so a role resolves in both schemes.
 *
 * The brand green, blue and orange are kept as the container tones rather than as `primary`.
 * At full saturation they cannot carry white label text: white on #4CAF50 is about 2.8:1,
 * under the 4.5:1 a button label needs. The darker tones below carry the text; the original
 * brand values stay visible on the containers and accents, where they sit under dark text.
 */

// Green, the primary hue.
internal val Green40 = Color(0xFF1B6E26)
internal val Green10 = Color(0xFF002106)
internal val Green90 = Color(0xFFA6F2A4)
internal val Green80 = Color(0xFF8BD88A)
internal val Green30 = Color(0xFF02531A)
internal val Green20 = Color(0xFF00390E)

// Blue, the secondary hue.
internal val Blue40 = Color(0xFF0B5FA5)
internal val Blue10 = Color(0xFF001D36)
internal val Blue90 = Color(0xFFD1E4FF)
internal val Blue80 = Color(0xFF9FCAFF)
internal val Blue30 = Color(0xFF00497D)
internal val Blue20 = Color(0xFF003258)

// Orange, the tertiary hue, used for warnings and "not connected" states.
internal val Orange40 = Color(0xFF8A5000)
internal val Orange10 = Color(0xFF2C1600)
internal val Orange90 = Color(0xFFFFDDB7)
internal val Orange80 = Color(0xFFFFB95C)
internal val Orange30 = Color(0xFF683D00)
internal val Orange20 = Color(0xFF482900)

// Neutrals, tinted towards the green so surfaces agree with the brand.
internal val NeutralLightSurface = Color(0xFFFDFDF6)
internal val NeutralLightBackground = Color(0xFFFAFAF5)
internal val NeutralLightOn = Color(0xFF1A1C19)
internal val NeutralLightVariant = Color(0xFFDEE5D9)
internal val NeutralLightOnVariant = Color(0xFF424940)
internal val NeutralLightOutline = Color(0xFF72796F)
internal val NeutralLightOutlineVariant = Color(0xFFC2C9BD)

internal val NeutralDarkSurface = Color(0xFF121411)
internal val NeutralDarkBackground = Color(0xFF1A1C19)
internal val NeutralDarkOn = Color(0xFFE2E3DD)
internal val NeutralDarkVariant = Color(0xFF424940)
internal val NeutralDarkOnVariant = Color(0xFFC2C9BD)
internal val NeutralDarkOutline = Color(0xFF8C9388)
internal val NeutralDarkOutlineVariant = Color(0xFF424940)

// Error, Material's standard pair; the brand has no red of its own.
internal val ErrorLight = Color(0xFFBA1A1A)
internal val ErrorLightContainer = Color(0xFFFFDAD6)
internal val ErrorLightOnContainer = Color(0xFF410002)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val ErrorDarkOn = Color(0xFF690005)
internal val ErrorDarkContainer = Color(0xFF93000A)
internal val ErrorDarkOnContainer = Color(0xFFFFDAD6)

/** The brand values the scheme is built from, kept for reference and previews. */
val PrimaryGreen = Color(0xFF4CAF50)
val PrimaryBlue = Color(0xFF2196F3)
val AccentOrange = Color(0xFFFF9800)
