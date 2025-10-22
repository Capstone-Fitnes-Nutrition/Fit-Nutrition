package sheridan.dheripu.fitnutrition.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sheridan.dheripu.fitnutrition.ui.components.InfoCard
import sheridan.dheripu.fitnutrition.ui.components.ScreenHeader

@Composable
fun NutritionScreen(padding: Modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "Nutrition",
            subtitle = "Your personalized meal plans"
        )

        // Today's Meal Plan
        Text(
            text = "Today's Meal Plan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        val meals = listOf(
            "Breakfast: Oatmeal with Berries - 350 kcal",
            "Lunch: Grilled Chicken Salad - 450 kcal",
            "Snack: Greek Yogurt - 150 kcal",
            "Dinner: Salmon with Vegetables - 500 kcal"
        )

        meals.forEach { meal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                ListItem(
                    headlineContent = { Text(meal) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nutritional Summary
        Text(
            text = "Nutritional Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        InfoCard(title = "Protein", value = "85", unit = "g")
        InfoCard(title = "Carbs", value = "210", unit = "g")
        InfoCard(title = "Fat", value = "65", unit = "g")
    }
}