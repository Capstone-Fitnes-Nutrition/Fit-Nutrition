package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
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
import sheridan.dheripu.fitnutrition.data.NutritionViewModel
import sheridan.dheripu.fitnutrition.data.ProfileViewModel
import sheridan.dheripu.fitnutrition.model.NutritionLog
import sheridan.dheripu.fitnutrition.ui.components.InfoCard
import java.util.Calendar

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    padding: Modifier,
    onNavigateToNutrition: () -> Unit,
    onNavigateToWorkoutAnalytics: () -> Unit,
    onNavigateToNutritionAnalytics: () -> Unit,
    fitnessViewModel: FitnessViewModel = viewModel(key = "fitness-${AuthManager.currentUser?.uid ?: "guest"}"),
    profileViewModel: ProfileViewModel = viewModel(),
    nutritionViewModel: NutritionViewModel = viewModel(key = "nutrition-${AuthManager.currentUser?.uid ?: "guest"}")
) {

    val profileState by profileViewModel.uiState.collectAsState()
    val nutritionState by nutritionViewModel.uiState.collectAsState()
    val firstName = profileState.profile?.name
        ?.trim()
        ?.substringBefore(" ")
        .orEmpty()

    LaunchedEffect(AuthManager.currentUser?.uid) {
        profileViewModel.loadProfile()
        nutritionViewModel.loadLogs()
        fitnessViewModel.loadWorkouts()
        fitnessViewModel.loadSteps()
    }

    val workouts = fitnessViewModel.myWorkouts.value
    val stepsState = fitnessViewModel.stepsState

    // Derive the dashboard values directly from the observable sources. This avoids
    // retaining the initial empty totals while Firestore restores cached records.
    val todayWorkouts = buildWorkoutStats(workouts, 1)
    val todaySteps = buildStepsStats(stepsState.entries, 1)
    val todayCaloriesIn = nutritionState.logs.todayCalorieIntake()

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
                title = "Calories In",
                value = todayCaloriesIn.toString(),
                unit = "kcal",
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
                title = "Daily Steps",
                value = "${todaySteps.total}",
                unit = "steps",
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
                        Icons.Default.Favorite,
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
                        Icons.Default.Favorite,
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
                        Icons.Default.Favorite,
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
                        Icons.Default.Favorite,
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

private fun List<NutritionLog>.todayCalorieIntake(): Int {
    val startOfToday = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    return filter { it.loggedAt >= startOfToday }.sumOf { it.calories }
}
