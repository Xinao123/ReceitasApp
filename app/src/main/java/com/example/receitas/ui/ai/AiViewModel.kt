package com.example.receitas.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiUiState(
    val ingredients: String = "",
    val restrictions: String = "",
    val cuisine: String = "",
    val servings: Int = 2,
    val allowExtras: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val results: List<AiRecipeSuggestion> = emptyList()
)

class AiViewModel(
    private val repo: AiRepository = AiRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AiUiState())
    val state: StateFlow<AiUiState> = _state

    fun setIngredients(v: String) = _state.update { it.copy(ingredients = v, error = null) }
    fun setRestrictions(v: String) = _state.update { it.copy(restrictions = v, error = null) }
    fun setCuisine(v: String) = _state.update { it.copy(cuisine = v, error = null) }

    fun setServings(v: Int) =
        _state.update { it.copy(servings = v.coerceIn(1, 12), error = null) }

    fun setAllowExtras(v: Boolean) =
        _state.update { it.copy(allowExtras = v, error = null) }

    fun generate() {
        val s = _state.value
        val ing = s.ingredients.trim()
        if (ing.length < 3) {
            _state.update { it.copy(error = "Coloca alguns ingredientes aí 🙃", results = emptyList()) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, results = emptyList()) }
            try {
                val res = repo.suggest(
                    ingredients = ing,
                    restrictions = s.restrictions.trim().ifBlank { null },
                    cuisine = s.cuisine.trim().ifBlank { null },
                    servings = s.servings,
                    allowExtras = s.allowExtras
                )
                _state.update { it.copy(isLoading = false, results = res) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Deu ruim 😵") }
            }
        }
    }

    fun toDraft(s: AiRecipeSuggestion): AiDraft {
        return AiDraft(
            title = s.title,
            category = s.category,
            timeMinutes = s.timeMinutes,
            servings = s.servings,
            ingredientsText = s.ingredients.joinToString("\n"),
            stepsText = s.steps.joinToString("\n")
        )
    }
}
