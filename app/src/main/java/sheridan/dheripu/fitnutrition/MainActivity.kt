package sheridan.dheripu.fitnutrition

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import sheridan.dheripu.fitnutrition.data.HealthViewModel
import sheridan.dheripu.fitnutrition.model.NavigationItem
import sheridan.dheripu.fitnutrition.ui.navigation.BottomNavigationBar
import sheridan.dheripu.fitnutrition.ui.screens.*
import sheridan.dheripu.fitnutrition.ui.theme.FitNutritionTheme

class MainActivity : ComponentActivity() {

    private val healthViewModel: HealthViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity onCreate")

        // Handle Fitbit OAuth callback
        handleFitbitCallback(intent)

        setContent {
            FitNutritionTheme {
                MainScreen(healthViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "MainActivity onNewIntent")
        setIntent(intent)
        handleFitbitCallback(intent)
    }

    /**
     * CRITICAL: Handle Fitbit OAuth callback
     * This captures the authorization code when Fitbit redirects back to your app
     */
    private fun handleFitbitCallback(intent: Intent?) {
        Log.d(TAG, "========== FITBIT CALLBACK HANDLER ==========")
        Log.d(TAG, "Intent: $intent")
        Log.d(TAG, "Intent action: ${intent?.action}")
        Log.d(TAG, "Intent data: ${intent?.data}")

        val data: Uri? = intent?.data

        if (data != null) {
            Log.d(TAG, "Intent data URI: $data")
            Log.d(TAG, "URI scheme: ${data.scheme}")
            Log.d(TAG, "URI host: ${data.host}")
            Log.d(TAG, "URI path: ${data.path}")

            // Check if this is the Fitbit callback
            if (data.scheme == "fitnutrition" && data.host == "fitbit") {
                Log.d(TAG, "✅ Fitbit callback detected!")

                val code = data.getQueryParameter("code")
                val error = data.getQueryParameter("error")

                Log.d(TAG, "Authorization code: ${code?.take(10)}...")
                Log.d(TAG, "Error parameter: $error")

                when {
                    code != null -> {
                        Log.d(TAG, "✅ Authorization code received, processing...")
                        Log.d(TAG, "Calling healthViewModel.handleAuthorizationCode()")

                        // Pass the code to ViewModel to exchange for token
                        healthViewModel.handleAuthorizationCode(code)
                    }
                    error != null -> {
                        Log.e(TAG, "❌ OAuth error: $error")
                        // TODO: Show error to user
                    }
                    else -> {
                        Log.w(TAG, "⚠️ Callback received but no code or error found")
                    }
                }
            } else {
                Log.d(TAG, "Not a Fitbit callback (scheme=${data.scheme}, host=${data.host})")
            }
        } else {
            Log.d(TAG, "Intent data is null - not a deep link")
        }

        Log.d(TAG, "========== FITBIT CALLBACK HANDLER END ==========")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(healthViewModel: HealthViewModel) {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf(NavigationItem.Home.route) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onItemSelected = { item ->
                    currentRoute = item.route
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavigationItem.Home.route) {
                HomeScreen(padding = Modifier.padding(innerPadding))
            }
            composable(NavigationItem.Nutrition.route) {
                NutritionScreen()
            }
            composable(NavigationItem.Fitness.route) {
                FitnessScreen(padding = Modifier.padding(innerPadding))
            }
            composable(NavigationItem.Health.route) {
                // Pass the shared ViewModel to WearableScreen
                WearableScreen(padding = Modifier.padding(innerPadding))
            }
            composable(NavigationItem.Profile.route) {
                ProfileScreen( onLogout = TODO(), modifier = TODO())
            }
        }
    }
}
