package sheridan.dheripu.fitnutrition.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sheridan.dheripu.fitnutrition.ui.components.InfoCard
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader

private data class AnalyticsData (
    val workouts: Int,
    val totalMinutes: Int,
    val avgMinutes: Int,
    val calories: Int,

)

private val weeklyData = AnalyticsData(
    workouts = 6,
    totalMinutes = 260,
    avgMinutes = 43,
    calories = 1850
)

private val monthlyData = AnalyticsData(
    workouts = 24,
    totalMinutes = 1010,
    avgMinutes = 42,
    calories = 7400
)

@Composable
fun WorkoutAnalyticsScreen(onBackClick: () -> Unit) {
    BackHandler { onBackClick()}

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Weekly", "Monthly")
    val data = if (selectedTab == 0) weeklyData else monthlyData

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize()
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")

        }

        ScreenHeader(
            title = "Workout Analytics",
            subtitle = "Track your workout progress"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)

        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {selectedTab = index},
                        text = {Text(title)}
                    )
                }
            }

            Text (
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row( modifier = Modifier.fillMaxWidth()) {
                InfoCard(
                    title = "Workouts",
                    value = "${data.workouts}",
                    unit = "total",
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "Total Duration",
                    value = "${data.totalMinutes}",
                    unit = "min",
                    modifier = Modifier.weight(1f)
                )
            }
            Row( modifier = Modifier.fillMaxWidth()) {
                InfoCard(
                    title = "Avg Duration",
                    value = "${data.avgMinutes}",
                    unit = "min",
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "Calories Burnt",
                    value = "${data.calories}",
                    unit = "kcal",
                    modifier = Modifier.weight(1f)
                )
            }
        }

    }
}