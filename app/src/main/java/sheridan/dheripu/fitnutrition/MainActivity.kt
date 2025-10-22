package sheridan.dheripu.fitnutrition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import sheridan.dheripu.fitnutrition.model.NavigationItem
import sheridan.dheripu.fitnutrition.ui.navigation.BottomNavigationBar
import sheridan.dheripu.fitnutrition.ui.screens.*
import sheridan.dheripu.fitnutrition.ui.theme.FitNutritionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitNutritionTheme {
                var currentRoute by rememberSaveable { mutableStateOf(NavigationItem.Home.route) }

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
                            currentRoute = currentRoute,
                            onItemSelected = { screen ->
                                currentRoute = screen.route
                            }
                        )
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        NavigationItem.Home -> HomeScreen(Modifier.padding(innerPadding))
                        NavigationItem.Nutrition -> NutritionScreen(Modifier.padding(innerPadding))
                        NavigationItem.Fitness -> FitnessScreen(Modifier.padding(innerPadding))
                        NavigationItem.Wearable -> WearableScreen(Modifier.padding(innerPadding))
                        NavigationItem.Profile -> ProfileScreen(Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}
