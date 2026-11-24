package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import sheridan.dheripu.fitnutrition.data.FitnessViewModel
import sheridan.dheripu.fitnutrition.data.StepsViewModel
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader
import sheridan.dheripu.fitnutrition.data.RecommendationViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    padding: Modifier = Modifier,
    viewModel: FitnessViewModel = viewModel(),
    // Reason: keyed so the workout card does not share state with the meal card on Nutrition.
    recommendationViewModel: RecommendationViewModel = viewModel(key = "workout-recommendations"),
    stepsViewModel: StepsViewModel = viewModel(),
    onNavigateToProfile: () -> Unit
) {
    var searchString by remember { mutableStateOf("") }
    val recommendationState by recommendationViewModel.state.collectAsStateWithLifecycle()
    val stepsState by stepsViewModel.state.collectAsStateWithLifecycle()
    val exercises = viewModel.exercises.value
    val myWorkouts = viewModel.myWorkouts.value
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var displayInput by remember { mutableStateOf(false) }
    var displayDetails by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("") }

    // Reason: one scrolling list for the whole screen. With a plain Column everything below the
    // recommendation card became unreachable once it filled with suggestions.
    LazyColumn(
        modifier = Modifier
            .then(padding)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ScreenHeader(
                title = "Fitness",
                subtitle = "Workout plans and tracking"
            )
        }

        item {
            RecommendationCard(
                title = "Workout recommendation",
                state = recommendationState,
                acceptLabel = "Done",
                onGenerate = { recommendationViewModel.loadWorkout() },
                onAccept = recommendationViewModel::accept,
                onDismiss = { recommendationViewModel.dismiss(it) },
                onRestore = { recommendationViewModel.restore(it) },
                onDismissUndo = { recommendationViewModel.clearUndo() },
                onNavigateToProfile = onNavigateToProfile
            )
        }

        item {
            StepEntryCard(
                state = stepsState,
                onSave = stepsViewModel::saveSteps,
                onInputOpened = {
                    stepsViewModel.consumeMessage()
                    stepsViewModel.clearError()
                }
            )
        }

        item { Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            OutlinedTextField(
                value = searchString,
                onValueChange = { searchString = it },
                label = { Text("Search body part (eg, back, chest)") },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { viewModel.fetchExercisesByBodyPart(searchString) },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("Go!")
            }
        } }

        if (searchString.isNotBlank() || selectedFilter.isNotBlank()) {
            item {
                Button(
                    onClick = {
                        selectedFilter = ""
                        searchString = ""
                        viewModel.clearExercises()
                    },
                ) {
                    Text("Clear Filters")
                }
            }
        }

        item {
            Text(
                text = "Exercise Filters",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val bodyParts = listOf("Chest", "Back", "Waist")

                bodyParts.forEach { part ->
                    FilterChip(
                        selected = (selectedFilter == part),
                        onClick = {
                            selectedFilter = part
                            viewModel.fetchExercisesByBodyPart(part)
                        },
                        label = {
                            Text(part)
                        }
                    )
                }
            }
        }

        viewModel.errorMessage?.let { error ->
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        items(exercises, key = { it.id }) { exercise ->
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    modifier = Modifier.clickable {
                        selectedExercise = exercise
                        displayDetails = true
                    },
                    headlineContent = { Text(exercise.name) },
                    supportingContent = { Text("Target: ${exercise.target}") },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                selectedExercise = exercise
                                displayInput = true
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add exercise")
                        }
                    }
                )
            }
        }

        item {
            Text(
                "My Workouts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        if (myWorkouts.isEmpty()) {
            item { Text("No exercises added yet") }
        } else {
            items(
                myWorkouts,
                key = { "${it.exercise.id}-${it.completedAt}" }
            ) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(item.exercise.name) },
                        supportingContent = {
                            Text(
                                "Target: ${item.exercise.target}\n" +
                                        "Sets: ${item.sets} Reps: " +
                                        "${item.reps} Time: ${item.time} min"
                            )
                        }
                    )
                }
            }
        }
    }

    selectedExercise?.takeIf { displayInput }?.let { exercise ->
        LogExerciseDialog(
            exercise = exercise,
            onDismiss = {
                displayInput = false
                selectedExercise = null
            },
            onConfirm = { sets, reps, minutes ->
                viewModel.addWorkoutItem(
                    WorkoutItem(exercise = exercise, sets = sets, reps = reps, time = minutes)
                )
                displayInput = false
                selectedExercise = null
            }
        )
    }

    selectedExercise?.takeIf { displayDetails }?.let { exercise ->
        AlertDialog(
            onDismissRequest = { displayDetails = false },
            title = { Text(exercise.name) },
            text = {
                Column {
                    Text(
                        text = "Target Muscle: ${exercise.target}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Instructions:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    exercise.instructions.forEachIndexed { index, instruction ->
                        Text("${index + 1}. $instruction")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { displayDetails = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Logs one exercise.
 *
 * The old version required sets, reps and time all at once and, if any was blank, closed and
 * saved nothing without saying so. That contradicted the data too: strength work carries no
 * time and cardio carries no sets or reps, which is how the engine prescribes them. It now
 * takes either a sets-and-reps pair or a duration, keeps the fields numeric, and refuses to
 * close with an explanation rather than silently discarding the entry.
 */
@Composable
private fun LogExerciseDialog(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onConfirm: (sets: Int, reps: Int, minutes: Int) -> Unit
) {
    var sets by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    val setsValue = sets.toIntOrNull() ?: 0
    val repsValue = reps.toIntOrNull() ?: 0
    val minutesValue = minutes.toIntOrNull() ?: 0
    val hasStrength = setsValue > 0 && repsValue > 0
    val hasDuration = minutesValue > 0
    val isValid = hasStrength || hasDuration

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${exercise.name.replaceFirstChar(Char::uppercase)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter sets and reps, or a time for cardio. You do not need both.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WholeNumberField(
                        value = sets,
                        onValueChange = { sets = it; showError = false },
                        label = "Sets",
                        modifier = Modifier.weight(1f)
                    )
                    WholeNumberField(
                        value = reps,
                        onValueChange = { reps = it; showError = false },
                        label = "Reps",
                        modifier = Modifier.weight(1f)
                    )
                }
                WholeNumberField(
                    value = minutes,
                    onValueChange = { minutes = it; showError = false },
                    label = "Time (minutes)",
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        text = if (setsValue > 0 || repsValue > 0) {
                            "Sets and reps go together. Fill in both, or give a time instead."
                        } else {
                            "Add sets and reps, or a time, before saving."
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(setsValue, repsValue, minutesValue)
                    } else {
                        showError = true
                    }
                }
            ) {
                Text("Log exercise")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** A text field that only ever holds digits, with the number keypad up. */
@Composable
private fun WholeNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = { entered ->
            // Reason: the fields were plain text, so letters went in and toIntOrNull() turned
            // them into a silent zero. Filtering here means the value is always parseable.
            onValueChange(entered.filter(Char::isDigit).take(MAX_DIGITS))
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction
        ),
        modifier = modifier
    )
}

private const val MAX_DIGITS = 4
