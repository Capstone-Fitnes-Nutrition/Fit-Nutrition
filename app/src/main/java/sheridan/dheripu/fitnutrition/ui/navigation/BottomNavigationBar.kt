package sheridan.dheripu.fitnutrition.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import sheridan.dheripu.fitnutrition.model.NavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onItemSelected: (NavigationItem) -> Unit
) {
    val navigationItems = listOf(
        NavigationItem.Home,
        NavigationItem.Nutrition,
        NavigationItem.Fitness,
        NavigationItem.Health ,
        NavigationItem.Profile ,
    )

    NavigationBar {
        navigationItems.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        // Using default icons for now - replace with your own drawables
                        imageVector = when (item) {
                            NavigationItem.Home -> Icons.Filled.Home
                            NavigationItem.Nutrition -> Icons.Filled.LocalDining
                            NavigationItem.Fitness -> Icons.Filled.FitnessCenter
                            NavigationItem.Health -> Icons.Filled.Watch
                            NavigationItem.Profile -> Icons.Filled.Person
                        },
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = { onItemSelected(item) }
            )
        }
    }
}