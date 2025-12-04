package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import sheridan.dheripu.fitnutrition.model.Exercise
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    padding: Modifier = Modifier,
    viewModel: FitnessViewModel = viewModel()
) {
    var searchString by remember { mutableStateOf("") }
    val exercises = viewModel.exercises
    val myWorkouts = viewModel.myWorkouts
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var displayInput by remember { mutableStateOf(false) }
    var displayDetails by remember { mutableStateOf(false) }
    var setsInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    var timeInput by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "Fitness",
            subtitle = "Workout plans and tracking"
        )

        Row(
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
        }

        if (searchString.isNotBlank() || selectedFilter.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Exercise Filters",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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

        viewModel.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
        }


        if (exercises.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)

            ) {
                items(exercises) { exercise ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
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
            }
        }

        Text(
            "My Workouts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)

        )

        if (myWorkouts.isEmpty()) {
            Text("No exercises added yet")
        } else {
            myWorkouts.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
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