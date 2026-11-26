package sheridan.dheripu.fitnutrition

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import sheridan.dheripu.fitnutrition.data.HealthViewModel
import sheridan.dheripu.fitnutrition.model.NavigationItem
import sheridan.dheripu.fitnutrition.ui.navigation.BottomNavigationBar
import sheridan.dheripu.fitnutrition.ui.screens.*
import sheridan.dheripu.fitnutrition.ui.theme.FitNutritionTheme

class MainActivity : ComponentActivity() {

    private val healthViewModel: HealthViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"
        private const val FITBIT_SCHEME = "fitnutrition"
        private const val FITBIT_HOST = "fitbit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reason: Android draws behind the status and navigation bars. Without this the app
        // letterboxes itself on modern devices; with it, Scaffold's innerPadding keeps content
        // clear of the system bars and the keyboard.
        enableEdgeToEdge()

        handleFitbitCallback(intent)

        setContent {
            FitNutritionTheme {
                FitNutritionApp(healthViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleFitbitCallback(intent)
    }

    /**
     * Finishes the Fitbit OAuth redirect. The URI is not logged: it carries the authorization
     * code, which is a live credential until it is exchanged.
     */
    private fun handleFitbitCallback(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data == null || data.scheme != FITBIT_SCHEME || data.host != FITBIT_HOST) return

        val code = data.getQueryParameter("code")
        val error = data.getQueryParameter("error")
        when {
            code != null -> healthViewModel.handleAuthorizationCode(
                code = code,
                state = data.getQueryParameter("state")
            )
            error != null -> Log.w(TAG, "Fitbit returned an OAuth error: $error")
            else -> Log.w(TAG, "Fitbit callback carried neither a code nor an error")
        }
    }
}

@Composable
fun FitNutritionApp(healthViewModel: HealthViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    var appState by remember { mutableStateOf<AppState>(AppState.Loading) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        showSplash = false
        appState = if (AuthManager.isUserLoggedIn) {
            AppState.MainApp
        } else {
            AppState.Auth
        }
    }

    if (showSplash) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        when (appState) {
            AppState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            AppState.Auth -> {
                AuthNavigation(
                    onAuthSuccess = { appState = AppState.MainApp }
                )
            }
            AppState.MainApp -> {
                MainAppScreen(
                    healthViewModel = healthViewModel,
                    onLogout = {
                        AuthManager.logout()
                        appState = AppState.Auth
                    },
                    onNavigateToRecipeDetail = { recipeId ->
                        appState = AppState.RecipeDetail(recipeId)
                    },
                    onNavigateToWorkoutAnalytics = {
                        appState = AppState.WorkoutAnalytics
                    },
                    onNavigateToNutritionAnalytics = {
                        appState = AppState.NutritionAnalytics
                    }
                )
            }
            is AppState.RecipeDetail -> {
                // Show recipe detail screen
                RecipeDetailScreen(
                    recipeId = (appState as AppState.RecipeDetail).recipeId,
                    onBackClick = { appState = AppState.MainApp }
                )
            }
            is AppState.WorkoutAnalytics -> {
                // Show workout analytics screen
                WorkoutAnalyticsScreen(
                    onBackClick = { appState = AppState.MainApp }
                )
            }
            is AppState.NutritionAnalytics -> {
                NutritionAnalyticsScreen(
                    onBackClick = { appState = AppState.MainApp }
                )
            }
        }
    }
}

@Composable
fun AuthNavigation(onAuthSuccess: () -> Unit) {
    var showLogin by remember { mutableStateOf(true) }

    if (showLogin) {
        LoginScreen(
            onLoginSuccess = onAuthSuccess,
            onNavigateToRegister = { showLogin = false }
        )
    } else {
        RegisterScreen(
            onRegisterSuccess = onAuthSuccess,
            onNavigateToLogin = { showLogin = true }
        )
    }
}

@Composable
fun MainAppScreen(
    healthViewModel: HealthViewModel,
    onNavigateToRecipeDetail: (Int) -> Unit,
    onLogout: () -> Unit,
    onNavigateToWorkoutAnalytics : () -> Unit,
    onNavigateToNutritionAnalytics: () -> Unit,
) {

    var currentRoute by remember { mutableStateOf(NavigationItem.Home.route) }
    val context = LocalContext.current

    val navigationItems = listOf(
        NavigationItem.Home,
        NavigationItem.Nutrition,
        NavigationItem.Fitness,
        NavigationItem.Health,
        NavigationItem.Profile
    )
    val currentScreen = navigationItems.find { it.route == currentRoute } ?: NavigationItem.Home

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentScreen.route,
                onItemSelected = { screen -> currentRoute = screen.route }
            )
        }
    ) { innerPadding ->
        when (currentScreen) {
            is NavigationItem.Home -> {
                HomeScreen(padding = Modifier.padding(innerPadding),
                    onNavigateToNutrition = {
                        currentRoute = NavigationItem.Nutrition.route
                    },
                    onNavigateToWorkoutAnalytics = onNavigateToWorkoutAnalytics,
                    onNavigateToNutritionAnalytics = onNavigateToNutritionAnalytics)
            }
            is NavigationItem.Nutrition -> {
                NutritionScreen(padding = Modifier.padding(innerPadding),
                    onRecipeClick = onNavigateToRecipeDetail,
                    onNavigateToNutritionAnalytics = onNavigateToNutritionAnalytics,
                    onNavigateToProfile = { currentRoute = NavigationItem.Profile.route })
            }
            is NavigationItem.Fitness -> {
                FitnessScreen(
                    padding = Modifier.padding(innerPadding),
                    onNavigateToProfile = { currentRoute = NavigationItem.Profile.route }
                )
            }
            is NavigationItem.Health -> {
                WearableScreen(padding = Modifier.padding(innerPadding))
            }
            is NavigationItem.Profile -> {
                ProfileScreen(
                    onLogout = onLogout,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

sealed class AppState {
    object Loading : AppState()
    object Auth : AppState()
    object MainApp : AppState()
    data class RecipeDetail(val recipeId: Int) : AppState()
    object WorkoutAnalytics : AppState()
    object NutritionAnalytics : AppState()
}
