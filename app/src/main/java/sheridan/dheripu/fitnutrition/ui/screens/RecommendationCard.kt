package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RecommendationCard(
    title: String,
    recommendation: String?,
    onGenerate: () -> Unit,
    needsProfile: Boolean = false,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Prepares profile and history context, then shows a sample response without calling AI.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Button(onClick = onGenerate) { Text("Get recommendation") }
            recommendation?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (needsProfile) {
                OutlinedButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Complete profile")
                }
            }
        }
    }
}
