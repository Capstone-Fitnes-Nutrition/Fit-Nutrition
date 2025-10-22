package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sheridan.dheripu.fitnutrition.ui.components.InfoCard
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader


@Composable
fun WearableScreen(padding: Modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "Wearable Device",
            subtitle = "Fitbit data integration"
        )

        // Device Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Fitbit Connected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Last sync: 5 minutes ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Health Metrics
        Text(
            text = "Today's Health Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        InfoCard(title = "Heart Rate", value = "72", unit = "bpm")
        InfoCard(title = "Sleep", value = "7.5", unit = "hours")
        InfoCard(title = "Active Minutes", value = "45", unit = "min")

        // Activity Rings (Mock)
        Text(
            text = "Activity Rings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = "Steps",
                value = "7,542",
                unit = "/10,000",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "Calories",
                value = "420",
                unit = "active kcal",
                modifier = Modifier.weight(1f)
            )
        }
    }
}