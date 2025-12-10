package sheridan.dheripu.fitnutrition.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sheridan.dheripu.fitnutrition.model.Recipe
import sheridan.dheripu.fitnutrition.model.RecipeFilters
import sheridan.dheripu.fitnutrition.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel : ViewModel() {
    private val repository = RecipeRepository()
    
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _currentFilters = MutableStateFlow(RecipeFilters())
    val currentFilters: StateFlow<RecipeFilters> = _currentFilters.asStateFlow()
    
    fun searchRecipes(filters: RecipeFilters = currentFilters.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val result = repository.searchRecipes(filters)
                _recipes.value = result
                _currentFilters.value = filters
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load recipes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearFilters() {
        _currentFilters.value = RecipeFilters()
        searchRecipes()
    }
    
    fun updateQuery(query: String) {
        val newFilters = currentFilters.value.copy(query = query)
        searchRecipes(newFilters)
    }
    
    fun applyDietFilter(diet: String?) {
        val newFilters = currentFilters.value.copy(diet = diet)
        searchRecipes(newFilters)
    }
    
    fun applyCalorieFilter(maxCalories: Int?) {
        val newFilters = currentFilters.value.copy(maxCalories = maxCalories)
        searchRecipes(newFilters)
    }
    
    fun applyProteinFilter(minProtein: Int?) {
        val newFilters = currentFilters.value.copy(minProtein = minProtein)
        searchRecipes(newFilters)
    }

    suspend fun getRecipeById(id: Int): Recipe? {
        return try {
            repository.getRecipeById(id)
        } catch (e: Exception) {
            null
        }
    }
}