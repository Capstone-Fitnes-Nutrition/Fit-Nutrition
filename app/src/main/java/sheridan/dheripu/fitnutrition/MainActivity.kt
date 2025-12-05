package sheridan.dheripu.fitnutrition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import sheridan.dheripu.fitnutrition.data.FitbitService
import sheridan.dheripu.fitnutrition.data.HealthViewModel
import sheridan.dheripu.fitnutrition.data.HealthViewModelFactory
import sheridan.dheripu.fitnutrition.repository.HealthRepository
import sheridan.dheripu.fitnutrition.model.NavigationItem
import sheridan.dheripu.fitnutrition.ui.navigation.BottomNavigationBar
import sheridan.dheripu.fitnutrition.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitNutritionApp()
        }
    }
}

@Composable
fun FitNutritionApp() {
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
                    onLogout = { appState = AppState.Auth }
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
fun MainAppScreen(onLogout: () -> Unit) {
    var currentRoute by remember { mutableStateOf(NavigationItem.Home.route) }

    val context = LocalContext.current
    val healthViewModel: HealthViewModel = viewModel(
        factory = HealthViewModelFactory(
            FitbitService(context),
            HealthRepository()
        )
    )

    val navigationItems = listOf(
        NavigationItem.Home,
        NavigationItem.Nutrition,
        NavigationItem.Fitness,
        NavigationItem.Wearable,
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
            is NavigationItem.Home -> HomeScreen(Modifier.padding(innerPadding))
            is NavigationItem.Nutrition -> NutritionScreen(Modifier.padding(innerPadding))
            is NavigationItem.Fitness -> FitnessScreen(Modifier.padding(innerPadding))
            is NavigationItem.Wearable -> WearableScreen(healthViewModel, Modifier.padding(innerPadding))
            is NavigationItem.Profile -> ProfileScreen(
                onLogout = onLogout,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

sealed class AppState {
    object Loading : AppState()
    object Auth : AppState()
    object MainApp : AppState()
}
