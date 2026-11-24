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
    var setsInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    var timeInput by remember { mutableStateOf("") }
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

    if (displayInput && selectedExercise != null) {
        AlertDialog(
            onDismissRequest = { displayInput = false },
            title = { Text("Add ${selectedExercise!!.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = setsInput,
                        onValueChange = { setsInput = it },
                        label = { Text("Sets") },
                        modifier = Modifier.fillMaxWidth()

                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = { repsInput = it },
                        label = { Text("Reps") },
                        modifier = Modifier.fillMaxWidth()

                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = timeInput,
                        onValueChange = { timeInput = it },
                        label = { Text("Time (mins)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = {
                        if (setsInput.isNotBlank() && repsInput.isNotBlank() && timeInput.isNotBlank()) {
                            viewModel.addWorkoutItem(
                                WorkoutItem(
                                    exercise = selectedExercise!!,
                                    sets = setsInput.toIntOrNull() ?: 0,
                                    reps = repsInput.toIntOrNull() ?: 0,
                                    time = timeInput.toIntOrNull() ?: 0
                                )
                            )
                        }

                        setsInput = ""
                        repsInput = ""
                        timeInput = ""
                        selectedExercise = null
                        displayInput = false
                        viewModel.clearExercises()
                        searchString = ""
                        selectedFilter = ""
                    }
                ) {
                    Text("Add!")
                }
            },
            dismissButton = {
                TextButton(onClick = { displayInput = false }) {
                    Text("Cancel")

                }
            }
        )
    }

    if (displayDetails && selectedExercise != null) {
        AlertDialog(
            onDismissRequest = { displayDetails = false },
            title = { Text(selectedExercise!!.name) },
            text = {
                Column {
                    Text(
                        text = "Target Muscle: ${selectedExercise!!.target}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Instructions:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    selectedExercise!!.instructions.forEachIndexed { index, instruction ->
                        Text("${index + 1}. $instruction")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { displayDetails = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
