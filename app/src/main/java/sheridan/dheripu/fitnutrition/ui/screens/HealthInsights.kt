package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sheridan.dheripu.fitnutrition.model.HealthMetrics
import sheridan.dheripu.fitnutrition.model.health.HealthAlert
import sheridan.dheripu.fitnutrition.model.health.HealthAlertType

/** Display helpers for health insights. */
object HealthText {
    fun sleep(minutes: Int): String =
        if (minutes <= 0) "--" else "${minutes / 60}h ${"%02d".format(minutes % 60)}m"
}

@Composable
fun HealthAlertsSection(alerts: List<HealthAlert>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alerts.forEach { alert ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Column {
                        Text(alert.title, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(alert.message, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}

/** Steps per saved day for the last week, scaled to the best day. */
@Composable
fun WeeklyTrendsSection(week: List<HealthMetrics>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Last 7 Days",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (week.isEmpty()) {
            Text(
                "Synced days will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        val maxSteps = week.maxOf { it.steps }.coerceAtLeast(1)
        week.sortedBy { it.date }.forEach { day ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(day.date, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${day.steps} steps · sleep ${HealthText.sleep(day.sleepMinutes)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                LinearProgressIndicator(
                    progress = { day.steps.toFloat() / maxSteps },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthInsightsPreview() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HealthAlertsSection(
            listOf(
                HealthAlert(
                    HealthAlertType.POOR_SLEEP_STREAK,
                    "Poor sleep three nights in a row",
                    "You slept under 6 hours for the last three nights."
                )
            )
        )
        WeeklyTrendsSection(
            listOf(
                HealthMetrics(date = "2026-10-21", steps = 6400, sleepMinutes = 410),
                HealthMetrics(date = "2026-10-22", steps = 11200, sleepMinutes = 350)
            )
        )
    }
}
