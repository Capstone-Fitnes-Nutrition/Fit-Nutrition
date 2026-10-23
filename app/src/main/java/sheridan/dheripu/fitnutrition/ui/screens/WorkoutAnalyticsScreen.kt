package sheridan.dheripu.fitnutrition.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import sheridan.dheripu.fitnutrition.AuthManager
import sheridan.dheripu.fitnutrition.data.FitnessViewModel
import sheridan.dheripu.fitnutrition.data.StepsUiState
import sheridan.dheripu.fitnutrition.model.WorkoutItem
import sheridan.dheripu.fitnutrition.repository.StepEntry
import sheridan.dheripu.fitnutrition.repository.dateKey
import sheridan.dheripu.fitnutrition.ui.components.InfoCard
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.find


// estimate value
private const val CALORIES_PER_MINUTE = 5

private data class WorkoutStats(
    val totalWorkouts: Int,
    val totalMinutes: Int,
    val avgMinutes: Int,
    val totalCalories: Int,
    val previousMinutes: Int,
    val topMuscles: List<Pair<String, Int>>,
    val topExercises: List<Pair<String, Int>>,
)

private data class DaySteps(val dayStart: Long, val steps:Int)

private data class StepsStats(
    val days: List<DaySteps>,
    val total: Int,
    val average: Int,
    val max: Int
)

@Composable
fun WorkoutAnalyticsScreen(
    onBackClick: () -> Unit,
    fitnessViewModel: FitnessViewModel = viewModel(key = AuthManager.currentUser?.uid ?: "guest")
) {
    BackHandler { onBackClick() }

    val workouts = fitnessViewModel.myWorkouts.value
    val stepsState = fitnessViewModel.stepsState

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Weekly", "Monthly")
    val days = if (selectedTab == 0) 7 else 30

    LaunchedEffect(Unit) { fitnessViewModel.loadSteps() }

    val workoutStats = remember(workouts, days) { buildWorkoutStats(workouts, days) }
    val stepsStats = remember(stepsState.entries, days) { buildStepsStats(stepsState.entries, days) }

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
            title = "Workout Analytics",
            subtitle = "Track your workout progress"
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {selectedTab = index},
                    text = {Text(title)}
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            WorkoutSection(
                stats = workoutStats,
                periodName = if (selectedTab == 0) "week" else "month"
            )
            StepsSection(
                state = stepsState,
                stats = stepsStats,
                isWeekly = selectedTab == 0,
                onRetry = fitnessViewModel::loadSteps
            )
        }
    }
}
@Composable
private fun WorkoutSection(stats: WorkoutStats, periodName: String) {
    SectionTitle("Quick Stats")

    Row(modifier = Modifier.fillMaxWidth()) {
        InfoCard("Workouts", "${stats.totalWorkouts}", "total", Modifier.weight(1f))
        InfoCard("Total Duration", "${stats.totalMinutes}", "min", Modifier.weight(1f))
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        InfoCard("Avg Duration", "${stats.avgMinutes}", "min", Modifier.weight(1f))
        InfoCard("Calories Burnt", "${stats.totalCalories}", "kcal", Modifier.weight(1f))
    }

    val trend = trendText(stats.totalMinutes, stats.previousMinutes, periodName)
    if (trend.isNotEmpty()) {
        Text(
            text = trend,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    SectionTitle("Most trained muscles")
    CountCard(stats.topMuscles)

    SectionTitle("Most logged exercises")
    CountCard(stats.topExercises)
}

@Composable
private fun CountCard(items: List<Pair<String, Int>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (items.isEmpty()) {
                Text("No workouts in this period")
            } else {
                items.forEachIndexed { index, (label, count) ->
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.weight(1f))
                        Text(
                            text = if (count == 1) "1 workout" else "$count workouts",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsSection(
    state: StepsUiState,
    stats: StepsStats,
    isWeekly: Boolean,
    onRetry: () -> Unit
) {
    SectionTitle("Steps")

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.errorMessage != null -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Try again")
                }
            }
        }

        else -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoCard("Total Steps", "${stats.total}", "steps", Modifier.weight(1f))
                InfoCard("Avg (logged days)", "${stats.average}", "steps", Modifier.weight(1f))
            }
            StepsChart(stats = stats, isWeekly = isWeekly)
        }
    }
}

@Composable
private fun StepsChart(stats: StepsStats, isWeekly: Boolean) {
    val weekdayFormat = remember { SimpleDateFormat("E", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(12.dp)
        ) {
            stats.days.forEachIndexed { index, day ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 1.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((day.steps * 100 / stats.max).dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = when {
                            isWeekly -> weekdayFormat.format(day.dayStart)
                            index == 0 || (index + 1) % 5 == 0 -> dayFormat.format(day.dayStart)
                            else -> " "
                        },
                        fontSize = 10.sp,
                        modifier = Modifier.wrapContentWidth(unbounded = true)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}
private fun buildWorkoutStats(workouts: List<WorkoutItem>, days: Int): WorkoutStats {
    val windowStart = startOfDayDaysAgo(days - 1)
    val previousStart = startOfDayDaysAgo(days * 2 - 1)

    val current = workouts.filter { it.completedAt >= windowStart }
    val previousMinutes = workouts
        .filter { it.completedAt >= previousStart && it.completedAt < windowStart }
        .sumOf { it.time }
    val totalMinutes = current.sumOf { it.time }

    return WorkoutStats(
        totalWorkouts = current.size,
        totalMinutes = totalMinutes,
        avgMinutes = if (current.isEmpty()) 0 else totalMinutes / current.size,
        totalCalories = totalMinutes * CALORIES_PER_MINUTE,
        previousMinutes = previousMinutes,
        topMuscles = current.map { it.exercise.target }.topCounts(),
        topExercises = current.map { it.exercise.name }.topCounts()
    )
}

private fun buildStepsStats(entries: List<StepEntry>, days: Int): StepsStats {
    val firstDay = Calendar.getInstance().apply { timeInMillis = startOfDayDaysAgo(days - 1) }

    val dailySteps = List(days) { i ->
        val millis = (firstDay.clone() as Calendar)
            .apply { add(Calendar.DAY_OF_YEAR, i) }
            .timeInMillis
        val key = dateKey(millis)
        DaySteps(millis, entries.find { it.dateKey == key }?.steps ?: 0)
    }

    val total = dailySteps.sumOf { it.steps }
    val loggedDays = dailySteps.count { it.steps > 0 }

    return StepsStats(
        days = dailySteps,
        total = total,
        average = if (loggedDays > 0) total / loggedDays else 0,
        max = maxOf(dailySteps.maxOf { it.steps }, 1)
    )
}

private fun List<String>.topCounts(): List<Pair<String, Int>> =
    filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(3)
        .map { it.key to it.value }

private fun startOfDayDaysAgo(daysAgo: Int): Long = Calendar.getInstance().run {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    add(Calendar.DAY_OF_YEAR, -daysAgo)
    timeInMillis
}

private fun trendText(current: Int, previous: Int, periodName: String): String {
    if (previous == 0) {
        return if (current == 0) "" else "No workouts last $periodName to compare."
    }
    val percent = (current - previous) * 100 / previous
    return when {
        percent > 0 -> "Total duration is up $percent% from last $periodName."
        percent < 0 -> "Total duration is down ${-percent}% from last $periodName."
        else -> "Total duration is the same as last $periodName."
    }
}