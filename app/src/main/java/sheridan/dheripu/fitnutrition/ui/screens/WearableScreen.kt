package sheridan.dheripu.fitnutrition.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import sheridan.dheripu.fitnutrition.data.HealthViewModel

@Composable
fun WearableScreen(padding: Modifier = Modifier) {
    val viewModel: HealthViewModel = viewModel()
    val context = LocalContext.current

    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val healthMetrics by viewModel.healthMetrics.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val useMockData by viewModel.useMockData.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val weeklyMetrics by viewModel.weeklyMetrics.collectAsStateWithLifecycle()

    // Reason: checkAuthentication already syncs when connected. The old version also read
    // isAuthenticated, which is still false at first composition, so the branch never ran.
    LaunchedEffect(Unit) { viewModel.checkAuthentication() }

    Column(
        modifier = padding
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Wearable Device",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Connection Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isAuthenticated) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAuthenticated) "Connected to Fitbit" else "Not Connected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isAuthenticated) "Syncing health data" else "Login to sync your data",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Icon(
                    imageVector = if (isAuthenticated) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isAuthenticated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Error Message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        if (alerts.isNotEmpty()) {
            HealthAlertsSection(alerts)
        }

        MockDataToggle(
            enabled = useMockData,
            onToggle = { viewModel.setUseMockData(it) }
        )

        // Authentication Section
        if (!isAuthenticated) {
            LoginSection(
                configured = viewModel.isFitbitConfigured,
                onLoginClick = {
                    val authUrl = viewModel.getAuthorizationUrl()
                    if (authUrl.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
                    }
                }
            )
        } else {
            // Health Metrics Section
            if (isLoading) {
                LoadingSection()
            } else {
                healthMetrics?.let { metrics ->
                    HealthMetricsSection(
                        metrics = metrics,
                        onRefresh = { viewModel.fetchHealthMetrics() },
                        onLogout = { viewModel.logout() }
                    )
                } ?: run {
                    EmptyMetricsSection(
                        onFetch = { viewModel.fetchHealthMetrics() }
                    )
                }
            }
            WeeklyTrendsSection(weeklyMetrics)
        }
    }
}

/**
 * Switches the Health tab between a real Fitbit account and the offline mock, so the metrics,
 * the alerts and the health-driven recommendations can be demonstrated without a watch.
 */
@Composable
private fun MockDataToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Use sample health data", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = if (enabled) {
                        "Showing seven days of generated metrics. No Fitbit account needed."
                    } else {
                        "Turn on to try the Health tab without a Fitbit account or a watch."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun LoginSection(configured: Boolean, onLoginClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Watch,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Connect Your Fitbit",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Login with your Fitbit account to track steps, heart rate, calories, distance, and sleep data",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onLoginClick,
                enabled = configured,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Login with Fitbit")
            }
            if (!configured) {
                Text(
                    text = "Fitbit is not configured. Add FITBIT_CLIENT_ID and " +
                        "FITBIT_CLIENT_SECRET to local.properties, or switch on sample data.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LoadingSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text("Syncing with Fitbit...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HealthMetricsSection(
    metrics: sheridan.dheripu.fitnutrition.model.HealthMetrics,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Data")
            }
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }

        Text(
            text = "Today's Metrics",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Metrics Grid
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
                value = "${metrics.heartRate}",
                unit = "bpm",
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
                title = "Distance",
                value = String.format("%.2f", metrics.distance),
                unit = "km",
                icon = "📍",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Active",
                value = "${metrics.activeMinutes}",
                unit = "min",
                icon = "⚡",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Activity",
                value = metrics.activityLevel,
                icon = "📊",
                modifier = Modifier.weight(1f)
            )
        }

        MetricCard(
            title = "Sleep",
            value = HealthText.sleep(metrics.sleepMinutes),
            icon = "😴",
            modifier = Modifier.fillMaxWidth()
        )

        // Last Sync Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Last Sync", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = metrics.lastSyncTime.ifEmpty { "Just now" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String = "",
    icon: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 28.sp)
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (unit.isNotEmpty()) {
                    Text(
                        " $unit",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMetricsSection(onFetch: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("No data available", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onFetch) {
                Text("Fetch Data")
            }
        }
    }
}
