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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sheridan.dheripu.fitnutrition.data.RecommendationUiState
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.recommendation.MealPick
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
    onRestore: (String) -> Unit = {},
    onDismissUndo: () -> Unit = {},
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
            RecommendationBody(
                state, acceptLabel, onAccept, onDismiss, onRestore, onDismissUndo,
                onNavigateToProfile
            )
        }
    }
}

@Composable
private fun RecommendationBody(
    state: RecommendationUiState,
    acceptLabel: String,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDismissUndo: () -> Unit,
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
            rows = state.items.map { it.mealRow(state.handledIds) },
            dismissedRows = state.dismissed.map { it.mealRow(state.handledIds) },
            undoableId = state.undoableId,
            note = state.note,
            notice = state.notice,
            acceptLabel = acceptLabel,
            onAccept = onAccept,
            onDismiss = onDismiss,
            onRestore = onRestore,
            onDismissUndo = onDismissUndo
        )
        is RecommendationUiState.Workout -> SuggestionList(
            rows = state.items.map { it.workoutRow(state.handledIds) },
            dismissedRows = state.dismissed.map { it.workoutRow(state.handledIds) },
            undoableId = state.undoableId,
            note = state.note,
            notice = state.notice,
            acceptLabel = acceptLabel,
            onAccept = onAccept,
            onDismiss = onDismiss,
            onRestore = onRestore,
            onDismissUndo = onDismissUndo
        )
    }
}

private fun ScoredRecommendation<MealPick>.mealRow(handledIds: Set<String>) = SuggestionRow(
    id = recommendationId,
    title = item.recipe.title,
    summary = RecommendationText.summary(item),
    reasons = reasons,
    handled = recommendationId in handledIds
)

private fun ScoredRecommendation<PrescribedExercise>.workoutRow(handledIds: Set<String>) = SuggestionRow(
    id = recommendationId,
    title = item.exercise.name.replaceFirstChar(Char::uppercase),
    summary = RecommendationText.summary(item),
    reasons = reasons,
    handled = recommendationId in handledIds
)

@Composable
private fun SuggestionList(
    rows: List<SuggestionRow>,
    dismissedRows: List<SuggestionRow>,
    undoableId: String?,
    note: String?,
    notice: String?,
    acceptLabel: String,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDismissUndo: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        notice?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        dismissedRows.firstOrNull { it.id == undoableId }?.let { undoable ->
            UndoRow(
                title = undoable.title,
                onUndo = { onRestore(undoable.id) },
                onHide = onDismissUndo
            )
        }
        rows.forEach { row ->
            HorizontalDivider()
            SuggestionItem(row, acceptLabel, onAccept, onDismiss)
        }
        if (dismissedRows.isNotEmpty()) {
            HorizontalDivider()
            DismissedSection(dismissedRows, onRestore)
        }
    }
}

/**
 * The undo offer straight after a dismissal. Without it a tap on "Not for me" is silent and
 * final, which is the one thing the user cannot take back by refreshing.
 */
@Composable
private fun UndoRow(title: String, onUndo: () -> Unit, onHide: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Hid $title.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onUndo) { Text("Undo") }
            TextButton(onClick = onHide) { Text("OK") }
        }
    }
}

/** Everything the user has turned down, so nothing disappears without a way back. */
@Composable
private fun DismissedSection(rows: List<SuggestionRow>, onRestore: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Hide dismissed (${rows.size})" else "Dismissed (${rows.size})")
        }
        if (expanded) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(row.title, style = MaterialTheme.typography.bodyMedium)
                        Text(row.summary, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onRestore(row.id) }) { Text("Restore") }
                }
            }
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
