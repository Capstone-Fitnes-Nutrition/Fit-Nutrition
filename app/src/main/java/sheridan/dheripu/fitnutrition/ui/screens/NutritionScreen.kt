package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import sheridan.dheripu.fitnutrition.data.RecipeViewModel
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.model.RecipeFilters
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader


@Composable
fun NutritionScreen(
    modifier: Modifier = Modifier,
    onRecipeClick: (Int) -> Unit,
    recipeViewModel: RecipeViewModel = viewModel(),
    padding: Modifier
) {
    val recipes by recipeViewModel.recipes.collectAsState()
    val isLoading by recipeViewModel.isLoading.collectAsState()
    val errorMessage by recipeViewModel.errorMessage.collectAsState()
    val filters by recipeViewModel.currentFilters.collectAsState()

    // Load recipes on first launch
    LaunchedEffect(Unit) {
        recipeViewModel.searchRecipes()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        ScreenHeader(
            title = "Nutrition & Recipes",
            subtitle = "Find healthy meals based on your preferences"
        )

        // Search and Filters Section
        RecipeFiltersSection(
            currentFilters = filters,
            onSearch = { query -> recipeViewModel.updateQuery(query) },
            onApplyDiet = { diet -> recipeViewModel.applyDietFilter(diet) },
            onApplyCalorieFilter = { calories -> recipeViewModel.applyCalorieFilter(calories) },
            onApplyProteinFilter = { protein -> recipeViewModel.applyProteinFilter(protein) },
            onClearFilters = { recipeViewModel.clearFilters() }
        )

        // Error Message
        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Recipes List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (recipes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.RestaurantMenu,
                        contentDescription = "No recipes",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "No recipes found",
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recipes) { recipe ->
                    RecipeCard(recipe = recipe,
                    onRecipeClick = onRecipeClick)
                }
            }
        }
    }
}

@Composable
fun RecipeFiltersSection(
    currentFilters: RecipeFilters,
    onSearch: (String) -> Unit,
    onApplyDiet: (String?) -> Unit,
    onApplyCalorieFilter: (Int?) -> Unit,
    onApplyProteinFilter: (Int?) -> Unit,
    onClearFilters: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(currentFilters.query) }
    var selectedDiet by remember { mutableStateOf(currentFilters.diet) }
    var maxCalories by remember { mutableStateOf(currentFilters.maxCalories?.toString() ?: "") }
    var minProtein by remember { mutableStateOf(currentFilters.minProtein?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search recipes...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { onSearch(searchQuery) }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Filters Row
        Text(
            text = "Dietary Filters",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedDiet == "vegetarian",
                onClick = {
                    selectedDiet = if (selectedDiet == "vegetarian") null else "vegetarian"
                    onApplyDiet(selectedDiet)
                },
                label = { Text("Vegetarian") }
            )

            FilterChip(
                selected = selectedDiet == "vegan",
                onClick = {
                    selectedDiet = if (selectedDiet == "vegan") null else "vegan"
                    onApplyDiet(selectedDiet)
                },
                label = { Text("Vegan") }
            )

            FilterChip(
                selected = selectedDiet == "gluten free",
                onClick = {
                    selectedDiet = if (selectedDiet == "gluten free") null else "gluten free"
                    onApplyDiet(selectedDiet)
                },
                label = { Text("Gluten Free") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nutrition Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = maxCalories,
                onValueChange = {
                    maxCalories = it
                    if (it.isBlank() || it == "0") {
                        onApplyCalorieFilter(null)
                    } else {
                        it.toIntOrNull()?.let { calories ->
                            onApplyCalorieFilter(calories)
                        }
                    }
                },
                label = { Text("Max Calories") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = minProtein,
                onValueChange = {
                    minProtein = it
                    if (it.isBlank() || it == "0") {
                        onApplyProteinFilter(null)
                    } else {
                        it.toIntOrNull()?.let { protein ->
                            onApplyProteinFilter(protein)
                        }
                    }
                },
                label = { Text("Min Protein (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clear Filters Button
        if (currentFilters.query.isNotBlank() ||
            currentFilters.diet != null ||
            currentFilters.maxCalories != null ||
            currentFilters.minProtein != null) {
            Button(
                onClick = {
                    searchQuery = ""
                    selectedDiet = null
                    maxCalories = ""
                    minProtein = ""
                    onClearFilters()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Clear All Filters")
            }
        }
    }
}

@Composable
fun RecipeCard(recipe: Recipe,
               onRecipeClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable { onRecipeClick(recipe.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Recipe Image
            AsyncImage(
                model = recipe.image,
                contentDescription = recipe.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            // Recipe Info
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip(
                        icon = Icons.Default.Timer,
                        text = "${recipe.readyInMinutes} min"
                    )
                    InfoChip(
                        icon = Icons.Default.People,
                        text = "${recipe.servings} servings"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Diets/Tags
                if (recipe.diets.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recipe.diets.forEach { diet ->
                            AssistChip(
                                onClick = { },
                                label = { Text(diet.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}