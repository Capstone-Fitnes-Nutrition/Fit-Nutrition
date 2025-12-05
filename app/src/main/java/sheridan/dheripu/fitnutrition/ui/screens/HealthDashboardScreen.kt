package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.data.HealthViewModel

/**
 * Fitbit Health Dashboard Screen
 */
@Composable
fun HealthDashboardScreen(
    viewModel: HealthViewModel,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val metrics by viewModel.healthMetrics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val weeklyMetrics by viewModel.weeklyMetrics.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            HealthDashboardHeader(
                isAuthenticated = isAuthenticated,
                onLoginClick = onLoginClick,
                onLogoutClick = onLogoutClick
            )
        }

        // Error Message
        if (error != null) {
            item {
                ErrorCard(message = error!!) {
//                    viewModel.clearError()
                }
            }
        }

        // Loading State
        if (isLoading) {
            item {
                LoadingCard()
            }
        }

        // Metrics Cards
        if (isAuthenticated && metrics != null) {
            item {
                MetricsGrid(metrics = metrics!!)
            }

            item {
                LastSyncCard(metrics = metrics!!)
            }

            item {
                RefreshButton(
                    isLoading = isLoading,
                    onClick = { viewModel.fetchTodayMetrics() }
                )
            }

            item {
                WeeklyMetricsSection(metrics = weeklyMetrics)
            }
        }

        // No Authentication State
        if (!isAuthenticated) {
            item {
                NoAuthenticationCard(onLoginClick = onLoginClick)
            }
        }
    }
}

@Composable
fun HealthDashboardHeader(
    isAuthenticated: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Health Dashboard",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isAuthenticated) "Connected to Fitbit" else "Not Connected",
                fontSize = 12.sp,
                color = if (isAuthenticated) Color.Green else Color.Gray
            )
        }

        if (isAuthenticated) {
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.8f)
                )
            ) {
                Text("Logout", color = Color.White)
            }
        } else {
            Button(onClick = onLoginClick) {
                Text("Login with Fitbit")
            }
        }
    }
}

@Composable
fun MetricsGrid(metrics: HealthMetrics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Steps",
                value = metrics.steps.toString(),
                icon = "👟",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Heart Rate",
                value = "${metrics.heartRate} bpm",
                icon = "❤️",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Calories",
                value = metrics.calories.toString(),
                icon = "🔥",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Active",
                value = "${metrics.activeMinutes} min",
                icon = "⚡",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Distance",
                value = String.format("%.2f km", metrics.distance),
                icon = "📍",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Activity",
                value = metrics.activityLevel,
                icon = "📊",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                icon,
                fontSize = 28.sp
            )

            Text(
                title,
                fontSize = 12.sp,
                color = Color.Gray
            )

            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LastSyncCard(metrics: HealthMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Last Sync", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    metrics.lastSyncTime.ifEmpty { "Just now" },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun RefreshButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text("Sync with Fitbit")
    }
}

@Composable
fun WeeklyMetricsSection(metrics: List<HealthMetrics>) {
    if (metrics.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Weekly Summary",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalSteps = metrics.sumOf { it.steps.toLong() }
                val avgHeartRate = if (metrics.isNotEmpty()) {
                    (metrics.sumOf { it.heartRate.toLong() } / metrics.size).toInt()
                } else 0
                val totalCalories = metrics.sumOf { it.calories.toLong() }

                WeeklyMetricRow("Total Steps", totalSteps.toString())
                WeeklyMetricRow("Avg Heart Rate", "$avgHeartRate bpm")
                WeeklyMetricRow("Total Calories", totalCalories.toString())
                WeeklyMetricRow("Days Tracked", metrics.size.toString())
            }
        }
    }
}

@Composable
fun WeeklyMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Error", fontWeight = FontWeight.Bold, color = Color.Red)
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
            Text(message, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun LoadingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun NoAuthenticationCard(onLoginClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Connect Your Fitbit",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Login with your Fitbit account to track your health metrics",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLoginClick) {
                Text("Login with Fitbit")
            }
        }
    }
}
