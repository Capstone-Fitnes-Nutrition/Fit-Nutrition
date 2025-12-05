package sheridan.dheripu.fitnutrition.model

sealed class NavigationItem(
    val route: String,
    val title: String,
    val icon: Int
) {
    object Home : NavigationItem("home", "Home", android.R.drawable.ic_menu_myplaces)
    object Nutrition : NavigationItem("nutrition", "Nutrition", android.R.drawable.ic_menu_edit)
    object Fitness : NavigationItem("fitness", "Fitness", android.R.drawable.ic_menu_compass)
    object Health : NavigationItem("health", "Health", android.R.drawable.ic_media_play)
    object Profile : NavigationItem("profile", "Profile", android.R.drawable.ic_menu_manage)
}