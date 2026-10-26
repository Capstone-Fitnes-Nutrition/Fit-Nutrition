package sheridan.dheripu.fitnutrition.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sheridan.dheripu.fitnutrition.data.FitnessViewModel
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.model.analytics.WorkoutAnalytics
import sheridan.dheripu.fitnutrition.model.analytics.WorkoutAnalyticsFactory
import sheridan.dheripu.fitnutrition.ui.components.InfoCard
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutAnalyticsScreen(
    onBackClick: () -> Unit,
    fitnessViewModel: FitnessViewModel = viewModel()
) {
    BackHandler { onBackClick() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Weekly", "Monthly")
    val daysInPeriod = if (selectedTab == 0) 7 else 30

    val workouts by fitnessViewModel.myWorkouts
    val isLoading by fitnessViewModel.isLoadingWorkouts
    val errorMessage by fitnessViewModel.workoutsError

    val workoutsInPeriod = remember(workouts, daysInPeriod) {
        WorkoutAnalyticsFactory.withinPeriod(workouts, daysInPeriod)
    }
    val analytics = remember(workouts, daysInPeriod) {
        WorkoutAnalyticsFactory.from(workouts, daysInPeriod)
    }

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        ScreenHeader(
            title = "Workout Analytics",
            subtitle = "Track your workout progress"
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when {
            isLoading && workouts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null && workouts.isEmpty() -> {
                WorkoutAnalyticsError(
                    message = errorMessage ?: "Unable to load workout analytics",
                    onRetry = fitnessViewModel::loadWorkouts
                )
            }

            workoutsInPeriod.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No workouts logged in this period. Add exercises on the " +
                            "Fitness tab to start tracking.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {
                WorkoutAnalyticsContent(
                    analytics = analytics,
                    workouts = workoutsInPeriod
                )
            }
        }
    }
}

@Composable
private fun WorkoutAnalyticsContent(
    analytics: WorkoutAnalytics,
    workouts: List<WorkoutItem>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Quick Stats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = "Workouts",
                value = analytics.sessions.toString(),
                unit = "days",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "Total Duration",
                value = analytics.totalMinutes.toString(),
                unit = "min",
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = "Avg Duration",
                value = analytics.averageSessionMinutes.toString(),
                unit = "min / day",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "Exercises",
                value = analytics.exercisesLogged.toString(),
                unit = "logged",
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = "Sets",
                value = analytics.totalSets.toString(),
                unit = "total",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "Reps",
                value = analytics.totalReps.toString(),
                unit = "total",
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Recent workouts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        workouts.take(5).forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.exercise.name, fontWeight = FontWeight.Medium)
                        Text(
                            text = formatWorkoutDate(item.completedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Text(
                        text = "${item.sets}x${item.reps} · ${item.time} min",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutAnalyticsError(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Workout analytics unavailable", style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                Text("Retry")
            }
        }
    }
}

private fun formatWorkoutDate(timestamp: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
