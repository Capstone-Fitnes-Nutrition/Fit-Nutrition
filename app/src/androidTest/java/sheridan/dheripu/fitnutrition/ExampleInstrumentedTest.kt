package sheridan.dheripu.fitnutrition

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import sheridan.dheripu.fitnutrition.MainActivity

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("sheridan.dheripu.fitnutrition", appContext.packageName)
    }

    @Test
    fun appLaunchesToTheExpectedScreenForCurrentAuthState() {
        composeRule.mainClock.advanceTimeBy(2_000)
        if (AuthManager.isUserLoggedIn) {
            composeRule.onNodeWithText("Welcome to FitNutrition!").assertIsDisplayed()
            composeRule.onNodeWithText("Quick Actions").assertIsDisplayed()
        } else {
            composeRule.onNodeWithText("Welcome to FitNutrition").assertIsDisplayed()
            composeRule.onNodeWithText("Sign in to your account").assertIsDisplayed()
        }
    }
}
