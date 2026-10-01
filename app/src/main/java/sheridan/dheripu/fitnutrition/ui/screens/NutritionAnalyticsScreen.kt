package sheridan.dheripu.fitnutrition.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sheridan.dheripu.fitnutrition.data.NutritionViewModel
import sheridan.dheripu.fitnutrition.model.NutritionLog
import sheridan.dheripu.fitnutrition.ui.components.InfoCard
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class NutritionAnalyticsData(
    val meals: Int,
    val calories: Int,
    val proteinGrams: Int,
    val carbohydratesGrams: Int,
    val fatGrams: Int,
    val averageCalories: Int
)

@Composable
fun NutritionAnalyticsScreen(
    onBackClick: () -> Unit,
    nutritionViewModel: NutritionViewModel = viewModel()
) {
    BackHandler { onBackClick() }

    val uiState by nutritionViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Weekly", "Monthly")
    val daysInPeriod = if (selectedTab == 0) 7 else 30
    val logsInPeriod = remember(uiState.logs, daysInPeriod) {
        uiState.logs.filter { it.loggedAt >= startOfDayDaysAgo(daysInPeriod - 1) }
    }
    val analytics = remember(logsInPeriod) { logsInPeriod.toAnalyticsData() }

    LaunchedEffect(Unit) {
        nutritionViewModel.loadLogs()
    }

    DisposableEffect(Unit) {
        onDispose { nutritionViewModel.clearLogs() }
    }

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        ScreenHeader(
            title = "Nutrition Analytics",
            subtitle = "Your logged meals and macros"
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null && uiState.logs.isEmpty() -> {
                AnalyticsError(
                    message = uiState.errorMessage ?: "Unable to load nutrition analytics",
                    onRetry = nutritionViewModel::loadLogs
                )
            }

            logsInPeriod.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No meals logged in this period. Open a recipe and tap Log Meal to start tracking.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {
                NutritionAnalyticsContent(
                    analytics = analytics,
                    logs = logsInPeriod
                )
            }
        }
    }
}

@Composable
private fun NutritionAnalyticsContent(
    analytics: NutritionAnalyticsData,
    logs: List<NutritionLog>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Quick Stats",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = "Meals",
                value = analytics.meals.toString(),
                unit = "logged",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "Calories",
                value = analytics.calories.toString(),
                unit = "kcal",
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoCard(
                title = "Protein",
                value = analytics.proteinGrams.toString(),
                unit = "g",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "Average meal",
                value = analytics.averageCalories.toString(),
                unit = "kcal",
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Macro totals",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                MacroTotal("Protein", analytics.proteinGrams)
                Spacer(modifier = Modifier.height(8.dp))
                MacroTotal("Carbohydrates", analytics.carbohydratesGrams)
                Spacer(modifier = Modifier.height(8.dp))
                MacroTotal("Fat", analytics.fatGrams)
            }
        }

        Text(
            text = "Recent meals",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        logs.take(5).forEach { log ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(log.mealName, fontWeight = FontWeight.Medium)
                        Text(
                            text = formatLogDate(log.loggedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Text("${log.calories} kcal", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MacroTotal(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text("$value g", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AnalyticsError(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nutrition analytics unavailable", style = MaterialTheme.typography.titleMedium)
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            androidx.compose.material3.OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Try again")
            }
        }
    }
}

private fun List<NutritionLog>.toAnalyticsData(): NutritionAnalyticsData {
    val totalCalories = sumOf { it.calories }
    return NutritionAnalyticsData(
        meals = size,
        calories = totalCalories,
        proteinGrams = sumOf { it.proteinGrams }.roundToInt(),
        carbohydratesGrams = sumOf { it.carbohydratesGrams }.roundToInt(),
        fatGrams = sumOf { it.fatGrams }.roundToInt(),
        averageCalories = if (isEmpty()) 0 else totalCalories / size
    )
}

private fun startOfDayDaysAgo(daysAgo: Int): Long = Calendar.getInstance().run {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    add(Calendar.DAY_OF_YEAR, -daysAgo)
    timeInMillis
}

private fun formatLogDate(timestamp: Long): String =
    SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()).format(Date(timestamp))
