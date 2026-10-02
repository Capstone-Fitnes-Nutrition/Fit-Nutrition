package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sheridan.dheripu.fitnutrition.AuthManager
import sheridan.dheripu.fitnutrition.data.ProfileViewModel
import sheridan.dheripu.fitnutrition.model.User
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader

private val fitnessGoals = listOf("Weight Loss", "Muscle Gain", "Maintenance", "General Fitness")
private val activityLevels = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active")
private val dietaryPreferences = listOf("No preference", "Vegetarian", "Vegan", "Pescatarian", "Gluten-free")

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val currentUser = AuthManager.currentUser
    val uiState by profileViewModel.uiState.collectAsState()
    var isEditing by rememberSaveable { mutableStateOf(false) }

    var name by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var fitnessGoal by rememberSaveable { mutableStateOf(fitnessGoals.first()) }
    var activityLevel by rememberSaveable { mutableStateOf(activityLevels.first()) }
    var calorieTarget by rememberSaveable { mutableStateOf("") }
    var proteinTarget by rememberSaveable { mutableStateOf("") }
    var dietaryPreference by rememberSaveable { mutableStateOf(dietaryPreferences.first()) }
    var dietaryRestrictions by rememberSaveable { mutableStateOf("") }
    var availableEquipment by rememberSaveable { mutableStateOf("") }

    fun populateForm(profile: User?) {
        name = profile?.name.orEmpty()
        weight = profile?.weight.orEmpty()
        height = profile?.height.orEmpty()
        fitnessGoal = profile?.fitnessGoal?.takeIf { it in fitnessGoals } ?: fitnessGoals.first()
        activityLevel = profile?.activityLevel?.takeIf { it in activityLevels } ?: activityLevels.first()
        calorieTarget = profile?.dailyCalorieTarget.orEmpty()
        proteinTarget = profile?.dailyProteinTarget.orEmpty()
        dietaryPreference = profile?.dietaryPreference?.takeIf { it in dietaryPreferences }
            ?: dietaryPreferences.first()
        dietaryRestrictions = profile?.dietaryRestrictions.orEmpty().joinToString(", ")
        availableEquipment = profile?.availableEquipment.orEmpty().joinToString(", ")
    }

    LaunchedEffect(currentUser?.uid) {
        profileViewModel.loadProfile()
    }

    LaunchedEffect(uiState.profile?.id, uiState.isProfileMissing) {
        if (!isEditing) {
            populateForm(uiState.profile)
            if (uiState.isProfileMissing) isEditing = true
        }
    }

    LaunchedEffect(uiState.saveMessage) {
        if (uiState.saveMessage != null) {
            isEditing = false
            profileViewModel.clearSaveMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "Profile",
            subtitle = "Your account, nutrition and fitness preferences"
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            isEditing -> {
                ProfileEditor(
                    name = name,
                    onNameChange = { name = it },
                    weight = weight,
                    onWeightChange = { weight = it },
                    height = height,
                    onHeightChange = { height = it },
                    fitnessGoal = fitnessGoal,
                    onFitnessGoalChange = { fitnessGoal = it },
                    activityLevel = activityLevel,
                    onActivityLevelChange = { activityLevel = it },
                    calorieTarget = calorieTarget,
                    onCalorieTargetChange = { calorieTarget = it },
                    proteinTarget = proteinTarget,
                    onProteinTargetChange = { proteinTarget = it },
                    dietaryPreference = dietaryPreference,
                    onDietaryPreferenceChange = { dietaryPreference = it },
                    dietaryRestrictions = dietaryRestrictions,
                    onDietaryRestrictionsChange = { dietaryRestrictions = it },
                    availableEquipment = availableEquipment,
                    onAvailableEquipmentChange = { availableEquipment = it },
                    isSaving = uiState.isSaving,
                    onCancel = {
                        populateForm(uiState.profile)
                        isEditing = false
                    },
                    onSave = {
                        profileViewModel.saveProfile(
                            name = name,
                            weight = weight,
                            height = height,
                            fitnessGoal = fitnessGoal,
                            activityLevel = activityLevel,
                            dailyCalorieTarget = calorieTarget,
                            dailyProteinTarget = proteinTarget,
                            dietaryPreference = dietaryPreference,
                            dietaryRestrictions = dietaryRestrictions,
                            availableEquipment = availableEquipment
                        )
                    }
                )
            }

            uiState.errorMessage != null && uiState.profile == null -> {
                ProfileErrorCard(
                    message = uiState.errorMessage ?: "Unable to load your profile",
                    onRetry = profileViewModel::loadProfile
                )
            }

            else -> {
                ProfileSummary(
                    profile = uiState.profile,
                    email = currentUser?.email.orEmpty(),
                    onEdit = {
                        populateForm(uiState.profile)
                        isEditing = true
                    }
                )
            }
        }

        uiState.errorMessage?.takeIf { isEditing || uiState.profile != null }?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                profileViewModel.clearProfile()
                AuthManager.logout()
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Sign Out")
        }
    }
}

@Composable
private fun ProfileSummary(profile: User?, email: String, onEdit: () -> Unit) {
    val displayValue = { value: String -> value.ifBlank { "Not set" } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = profile?.name?.ifBlank { "Your profile" } ?: "Finish setting up your profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = email.ifBlank { "Not signed in" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ProfileValue("Fitness goal", displayValue(profile?.fitnessGoal.orEmpty()))
            ProfileValue("Weight", profile?.weight?.takeIf { it.isNotBlank() }?.plus(" kg") ?: "Not set")
            ProfileValue("Height", profile?.height?.takeIf { it.isNotBlank() }?.plus(" cm") ?: "Not set")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Nutrition preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Button(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(if (profile == null) "Set Up Profile" else "Edit Profile")
    }
}

@Composable
private fun ProfileValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProfileEditor(
    name: String,
    onNameChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    fitnessGoal: String,
    onFitnessGoalChange: (String) -> Unit,
    activityLevel: String,
    onActivityLevelChange: (String) -> Unit,
    calorieTarget: String,
    onCalorieTargetChange: (String) -> Unit,
    proteinTarget: String,
    onProteinTargetChange: (String) -> Unit,
    dietaryPreference: String,
    onDietaryPreferenceChange: (String) -> Unit,
    dietaryRestrictions: String,
    onDietaryRestrictionsChange: (String) -> Unit,
    availableEquipment: String,
    onAvailableEquipmentChange: (String) -> Unit,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Your health profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "These preferences are saved to your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = onWeightChange,
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.size(12.dp))
                OutlinedTextField(
                    value = height,
                    onValueChange = onHeightChange,
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ProfileDropdown(
                label = "Fitness goal",
                value = fitnessGoal,
                options = fitnessGoals,
                onSelected = onFitnessGoalChange
            )

            Spacer(modifier = Modifier.height(12.dp))
            ProfileDropdown(
                label = "Activity level",
                value = activityLevel,
                options = activityLevels,
                onSelected = onActivityLevelChange
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Nutrition targets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = calorieTarget,
                onValueChange = onCalorieTargetChange,
                label = { Text("Daily calorie target (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = proteinTarget,
                onValueChange = onProteinTargetChange,
                label = { Text("Daily protein target (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProfileDropdown(
                label = "Dietary preference",
                value = dietaryPreference,
                options = dietaryPreferences,
                onSelected = onDietaryPreferenceChange
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = dietaryRestrictions,
                onValueChange = onDietaryRestrictionsChange,
                label = { Text("Dietary restrictions (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = availableEquipment,
                onValueChange = onAvailableEquipmentChange,
                label = { Text("Available workout equipment (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileErrorCard(message: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Profile unavailable", style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                Text("Try again")
            }
        }
    }
}
