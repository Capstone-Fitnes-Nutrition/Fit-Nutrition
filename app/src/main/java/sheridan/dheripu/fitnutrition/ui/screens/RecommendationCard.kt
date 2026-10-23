package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sheridan.dheripu.fitnutrition.data.RecommendationUiState
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.recommendation.PrescribedExercise
import sheridan.dheripu.fitnutrition.model.recommendation.ScoreBreakdown
import sheridan.dheripu.fitnutrition.model.recommendation.ScoredRecommendation

/** One suggestion row as the card renders it, independent of meal or workout. */
private data class SuggestionRow(
    val id: String,
    val title: String,
    val summary: String,
    val reasons: List<String>,
    val handled: Boolean
)

@Composable
fun RecommendationCard(
    title: String,
    state: RecommendationUiState,
    acceptLabel: String,
    onGenerate: () -> Unit,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToProfile: () -> Unit = {}
) {
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Ranked for your goal, targets, history, and past feedback.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Button(onClick = onGenerate, enabled = state != RecommendationUiState.Loading) {
                Text(if (state is RecommendationUiState.Idle) "Get suggestions" else "Refresh")
            }
            RecommendationBody(state, acceptLabel, onAccept, onDismiss, onNavigateToProfile)
        }
    }
}

@Composable
private fun RecommendationBody(
    state: RecommendationUiState,
    acceptLabel: String,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val bodyModifier = Modifier.padding(top = 12.dp)
    when (state) {
        RecommendationUiState.Idle -> Unit
        RecommendationUiState.Loading -> CircularProgressIndicator(modifier = bodyModifier)
        RecommendationUiState.SignedOut ->
            Text("Sign in to get personalized suggestions.", modifier = bodyModifier)
        is RecommendationUiState.Error -> Text(
            state.message,
            color = MaterialTheme.colorScheme.error,
            modifier = bodyModifier
        )
        is RecommendationUiState.NeedsProfile -> Column(modifier = bodyModifier) {
            Text("Your profile needs a few details first:")
            state.missingFields.forEach { Text("• ${RecommendationText.missingField(it)}") }
            OutlinedButton(onClick = onNavigateToProfile, modifier = Modifier.padding(top = 8.dp)) {
                Text("Complete profile")
            }
        }
        is RecommendationUiState.Meals -> SuggestionList(
            rows = state.items.map {
                SuggestionRow(it.recommendationId, it.item.recipe.title,
                    RecommendationText.summary(it.item), it.reasons,
                    it.recommendationId in state.handledIds)
            },
            note = state.note,
            notice = state.notice,
            acceptLabel = acceptLabel,
            onAccept = onAccept,
            onDismiss = onDismiss
        )
        is RecommendationUiState.Workout -> SuggestionList(
            rows = state.items.map {
                SuggestionRow(it.recommendationId,
                    it.item.exercise.name.replaceFirstChar(Char::uppercase),
                    RecommendationText.summary(it.item), it.reasons,
                    it.recommendationId in state.handledIds)
            },
            note = state.note,
            notice = state.notice,
            acceptLabel = acceptLabel,
            onAccept = onAccept,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun SuggestionList(
    rows: List<SuggestionRow>,
    note: String?,
    notice: String?,
    acceptLabel: String,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        notice?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        rows.forEach { row ->
            HorizontalDivider()
            SuggestionItem(row, acceptLabel, onAccept, onDismiss)
        }
    }
}

@Composable
private fun SuggestionItem(
    row: SuggestionRow,
    acceptLabel: String,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit
) {
    Column {
        Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(row.summary, style = MaterialTheme.typography.bodyMedium)
        row.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (row.handled) {
                Text("Logged", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 12.dp))
            } else {
                TextButton(onClick = { onAccept(row.id) }) { Text(acceptLabel) }
                TextButton(onClick = { onDismiss(row.id) }) { Text("Not for me") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecommendationCardPreview() {
    val pushUp = PrescribedExercise(Exercise(id = "1", name = "push up", bodyPart = "chest"),
        sets = 3, reps = 12, durationMinutes = 0)
    RecommendationCard(
        title = "Workout recommendation",
        state = RecommendationUiState.Workout(
            items = listOf(
                ScoredRecommendation("workout-1", "1", pushUp, ScoreBreakdown(0.8, emptyMap()),
                    listOf("Your chest is rested", "Matched to your current level"), emptyList())
            ),
            note = "Using today's Fitbit activity level (MODERATE)."
        ),
        acceptLabel = "Done",
        onGenerate = {},
        onAccept = {},
        onDismiss = {}
    )
}
