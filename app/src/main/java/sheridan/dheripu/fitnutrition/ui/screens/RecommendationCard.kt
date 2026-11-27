package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val metric: SuggestionMetric,
    val summary: String,
    val reasons: List<String>,
    val handled: Boolean
)

/**
 * The suggestions card for a screen.
 *
 * Takes its placement from [modifier] and adds no outer padding of its own, so it lines up
 * with whatever else the screen is showing. It used to inset itself by 16dp on top of the
 * list's own padding, which left it sitting further in from the edge than every card beside
 * it on the Fitness tab.
 */
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
    onNavigateToProfile: () -> Unit = {},
    onOpen: ((String) -> Unit)? = null
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = onGenerate,
                    enabled = state != RecommendationUiState.Loading
                ) {
                    Text(if (state is RecommendationUiState.Idle) "Suggest" else "Refresh")
                }
            }
            RecommendationBody(
                state, acceptLabel, onAccept, onDismiss, onRestore, onDismissUndo,
                onNavigateToProfile, onOpen
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
    onNavigateToProfile: () -> Unit,
    onOpen: ((String) -> Unit)?
) {
    val bodyModifier = Modifier.padding(top = 8.dp)
    when (state) {
        RecommendationUiState.Idle -> Text(
            text = "Ranked against your goal, targets, history and past feedback.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = bodyModifier
        )
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
            onOpen = onOpen,
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
            onOpen = onOpen,
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
    metric = RecommendationText.metric(item),
    summary = RecommendationText.summary(item),
    reasons = reasons,
    handled = recommendationId in handledIds
)

private fun ScoredRecommendation<PrescribedExercise>.workoutRow(handledIds: Set<String>) =
    SuggestionRow(
        id = recommendationId,
        title = item.exercise.name.replaceFirstChar(Char::uppercase),
        metric = RecommendationText.metric(item),
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
    onDismissUndo: () -> Unit,
    onOpen: ((String) -> Unit)?
) {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (note != null || notice != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                notice?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        dismissedRows.firstOrNull { it.id == undoableId }?.let { undoable ->
            UndoRow(
                title = undoable.title,
                onUndo = { onRestore(undoable.id) },
                onHide = onDismissUndo
            )
        }
        // Reason: spacing separates these, not rules. A divider between every suggestion drew
        // more lines than there were suggestions, and the card already bounds the group.
        rows.forEach { row -> SuggestionItem(row, acceptLabel, onAccept, onDismiss, onOpen) }
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hid $title.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
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
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 0.dp, vertical = 8.dp
            )
        ) {
            Text(if (expanded) "Hide dismissed (${rows.size})" else "Dismissed (${rows.size})")
        }
        if (expanded) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(row.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            row.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onRestore(row.id) }) { Text("Restore") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionItem(
    row: SuggestionRow,
    acceptLabel: String,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onOpen: ((String) -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Reason: the title and the figures are what the user reads to decide, so that is
        // what opens the detail. Only meals have one to open; the workout card passes no
        // handler and stays inert rather than offering a tap that goes nowhere.
        val openable = if (onOpen == null) {
            Modifier
        } else {
            Modifier.clickable { onOpen(row.id) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().then(openable),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = row.metric.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Reason: the figure the user actually chooses on, pulled out of the run-on
            // summary line and given the weight it was always carrying.
            Text(
                text = row.metric.lead,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End
            )
        }
        if (row.reasons.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.reasons.forEach { reason -> ReasonTag(reason) }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (row.handled) {
                Text(
                    text = "Logged",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                FilledTonalButton(onClick = { onAccept(row.id) }) { Text(acceptLabel) }
                TextButton(onClick = { onDismiss(row.id) }) { Text("Not for me") }
            }
        }
    }
}

/** A single reason the engine gave, shown as a tag rather than a bulleted line. */
@Composable
private fun ReasonTag(reason: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = reason,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecommendationCardPreview() {
    val pushUp = PrescribedExercise(
        Exercise(id = "1", name = "push up", bodyPart = "chest", equipment = "body weight"),
        sets = 3, reps = 12, durationMinutes = 0
    )
    RecommendationCard(
        title = "Workout recommendation",
        state = RecommendationUiState.Workout(
            items = listOf(
                ScoredRecommendation(
                    "workout-1", "1", pushUp, ScoreBreakdown(0.8, emptyMap()),
                    listOf("Your chest is rested", "Matched to your current level"), emptyList()
                )
            ),
            note = "Using today's synced activity level (MODERATE)."
        ),
        acceptLabel = "Done",
        onGenerate = {},
        onAccept = {},
        onDismiss = {},
        modifier = Modifier.padding(16.dp)
    )
}
