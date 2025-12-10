package sheridan.dheripu.fitnutrition.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import sheridan.dheripu.fitnutrition.data.RecipeViewModel
import sheridan.dheripu.fitnutrition.model.Ingredient
import sheridan.dheripu.fitnutrition.model.Recipe
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Int,
    onBackClick: () -> Unit,
    recipeViewModel: RecipeViewModel = viewModel()
) {
    // State variables
    var recipe by remember { mutableStateOf<Recipe?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load recipe details when screen opens
    LaunchedEffect(recipeId) {
        isLoading = true
        try {
            val result = recipeViewModel.getRecipeById(recipeId)
            if (result != null) {
                recipe = result
            } else {
                errorMessage = "Could not load recipe details"
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    // Scaffold with TopAppBar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Recipe Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading recipe details...")
                }
            }
        }
        // Error state
        else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
        }
        // Success state - show recipe
        else if (recipe != null) {
            RecipeDetailContent(
                recipe = recipe!!,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun RecipeDetailContent(
    recipe: Recipe,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // 1. RECIPE IMAGE
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = recipe.image,
                    contentDescription = "Recipe image: ${recipe.title}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // 2. RECIPE TITLE AND BASIC INFO
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Quick stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RecipeStatItem(
                        icon = Icons.Default.Timer,
                        value = "${recipe.readyInMinutes}",
                        label = "Minutes"
                    )
                    RecipeStatItem(
                        icon = Icons.Default.People,
                        value = "${recipe.servings}",
                        label = "Servings"
                    )
                    RecipeStatItem(
                        icon = Icons.Default.LocalFireDepartment,
                        value = getCalories(recipe),
                        label = "Calories"
                    )
                }
                
                // Dietary tags
                if (recipe.diets.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recipe.diets.forEach { diet ->
                            SuggestionChip(
                                onClick = { },
                                label = { 
                                    Text(
                                        diet.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // 3. NUTRITION FACTS SECTION
        item {
            NutritionSection(recipe)
        }
        
        // 4. INGREDIENTS SECTION
        item {
            IngredientsSection(recipe.extendedIngredients)
        }
        
        // 5. SUMMARY SECTION
        item {
            SummarySection(recipe.summary)
        }
        
        // 6. SOURCE LINK (if available)
        item {
            recipe.sourceUrl?.let { url ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    onClick = {

                    }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "External link",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "View Full Recipe Online",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap to open in browser",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeStatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun NutritionSection(recipe: Recipe) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MonitorWeight,
                    contentDescription = "Nutrition",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nutrition Facts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show key nutrients
            val keyNutrients = listOf("Calories", "Protein", "Carbohydrates", "Fat")
            recipe.nutrition?.nutrients?.filter { nutrient ->
                keyNutrients.any { it.equals(nutrient.name, ignoreCase = true) }
            }?.forEach { nutrient ->
                NutrientRow(
                    name = nutrient.name,
                    amount = nutrient.amount,
                    unit = nutrient.unit
                )
            }
        }
    }
}

@Composable
fun NutrientRow(name: String, amount: Double, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "${amount.roundToInt()} $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
    Divider(
        modifier = Modifier.padding(vertical = 4.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
}

@Composable
fun IngredientsSection(ingredients: List<Ingredient>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Ingredients",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ingredients",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ingredients.forEachIndexed { index, ingredient ->
                IngredientItem(
                    ingredient = ingredient,
                    number = index + 1,
                    showDivider = index < ingredients.size - 1
                )
            }
        }
    }
}

@Composable
fun IngredientItem(
    ingredient: Ingredient,
    number: Int,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Ingredient details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ingredient.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${"%.1f".format(ingredient.amount)} ${ingredient.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Ingredient image (if available)
            ingredient.image?.let { imageUrl ->
                AsyncImage(
                    model = "https://spoonacular.com/cdn/ingredients_100x100/$imageUrl",
                    contentDescription = ingredient.name,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
        
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun SummarySection(summary: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Summary",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About This Recipe",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Clean HTML from summary
            val cleanSummary = remember(summary) {
                summary
                    .replace("<[^>]*>".toRegex(), " ")
                    .replace("&nbsp;", " ")
                    .replace("\\s+".toRegex(), " ")
                    .trim()
            }
            
            Text(
                text = cleanSummary,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
            )
        }
    }
}

// Helper function to get calories
private fun getCalories(recipe: Recipe): String {
    return recipe.nutrition?.nutrients
        ?.find { it.name.equals("Calories", ignoreCase = true) }
        ?.let { "${it.amount.roundToInt()}" }
        ?: "N/A"
}