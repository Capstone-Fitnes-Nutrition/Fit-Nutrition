package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sheridan.dheripu.fitnutrition.AuthManager
import sheridan.dheripu.fitnutrition.data.FitnessViewModel
import sheridan.dheripu.fitnutrition.data.ProfileViewModel
import sheridan.dheripu.fitnutrition.ui.components.InfoCard

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    padding: Modifier,
    onNavigateToNutrition: () -> Unit,
    onNavigateToWorkoutAnalytics: () -> Unit,
    onNavigateToNutritionAnalytics: () -> Unit,
    fitnessViewModel: FitnessViewModel = viewModel(key = AuthManager.currentUser?.uid ?: "guest"),
    profileViewModel: ProfileViewModel = viewModel()
) {

    val profileState by profileViewModel.uiState.collectAsState()
    val firstName = profileState.profile?.name
        ?.trim()
        ?.substringBefore(" ")
        .orEmpty()

    LaunchedEffect(AuthManager.currentUser?.uid) {profileViewModel.loadProfile()}

    val workouts = fitnessViewModel.myWorkouts.value
    val stepsState = fitnessViewModel.stepsState

    LaunchedEffect(Unit) { fitnessViewModel.loadSteps() }

    val todayWorkouts = remember(workouts) { buildWorkoutStats(workouts,1 )}
    val todaySteps = remember(stepsState.entries) { buildStepsStats(stepsState.entries,1 )}

    Column(
        modifier = padding
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (firstName.isNotBlank()) "Hi $firstName, Welcome to FitNutrition!" else "Welcome to FitNutrition!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Your holistic health companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Quick Stats
        Text(
            text = "Today's Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = "Calories Out",
                value = "${todayWorkouts.totalCalories}",
                unit = "kcal",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "Daily Steps",
                value = "${todaySteps.total}",
                unit = "steps",
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = "Total Workout Mins",
                value = "${todayWorkouts.totalMinutes}",
                unit = "min",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "Water",
                value = "--",
                unit = "L",
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Card(
            onClick = onNavigateToNutrition,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            ListItem(
                headlineContent = { Text("Log Today's Meals") },
                leadingContent = {
                    Icon(
                        Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            ListItem(
                headlineContent = { Text("Start a Workout") },
                leadingContent = {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            )
        }

        Card(
            onClick = onNavigateToWorkoutAnalytics,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)

        ) {
            ListItem(
                headlineContent = { Text("View Workout Analytics") },
                leadingContent = {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            )
        }

        Card(
            onClick = onNavigateToNutritionAnalytics,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            ListItem(
                headlineContent = { Text("View Nutrition Analytics") },
                leadingContent = {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            )
        }
    }
}
