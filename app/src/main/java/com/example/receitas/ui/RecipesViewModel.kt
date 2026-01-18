package com.example.receitas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.receitas.data.FirestoreRecipesRepository
import com.example.receitas.data.Recipe
import com.example.receitas.ui.ai.AiDraft
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecipesViewModel(app: Application) : AndroidViewModel(app) {


    private val _aiDraft = MutableStateFlow<AiDraft?>(null)
    val aiDraft: StateFlow<AiDraft?> = _aiDraft.asStateFlow()

    fun setAiDraft(draft: AiDraft) { _aiDraft.value = draft }

    fun consumeAiDraft(): AiDraft? {
        val v = _aiDraft.value
        _aiDraft.value = null
        return v
    }


    private val repo = FirestoreRecipesRepository()


    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    fun setCurrentUser(uid: String) { _currentUser.value = uid }


    private val _query = MutableStateFlow("")
    fun setQuery(q: String) { _query.value = q }


    val favoriteIds: StateFlow<Set<String>> =
        _currentUser
            .filterNotNull()
            .flatMapLatest { me -> repo.observeFavoriteIds(me) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())


    val recipes: StateFlow<List<Recipe>> =
        combine(_currentUser.filterNotNull(), _query) { me, q -> me to q.trim() }
            .flatMapLatest { (me, q) ->
                // ✅ esse observeVisibleRecipes já marca isFavorite usando users/{uid}/favorites
                repo.observeVisibleRecipes(me = me, q = q)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observeRecipe(id: String): Flow<Recipe?> =
        _currentUser
            .filterNotNull()
            .flatMapLatest { me -> repo.observeRecipe(id = id, me = me) }

    fun canEdit(recipe: Recipe?, me: String?): Boolean =
        recipe != null && me != null && recipe.ownerUid == me


    fun save(recipe: Recipe) {
        val me = _currentUser.value ?: return
        viewModelScope.launch {
            repo.save(me = me, recipe = recipe)
        }
    }

    fun delete(recipeId: String) {
        val me = _currentUser.value ?: return
        viewModelScope.launch {
            repo.delete(me = me, recipeId = recipeId)
        }
    }

    fun setPublic(recipeId: String, isPublic: Boolean) {
        val me = _currentUser.value ?: return
        viewModelScope.launch {
            repo.setPublic(me = me, recipeId = recipeId, isPublic = isPublic)
        }
    }


    fun toggleFavorite(recipeId: String) {
        val me = _currentUser.value ?: return
        viewModelScope.launch {
            repo.toggleFavorite(me = me, recipeId = recipeId)
        }
    }

    fun setFavorite(recipeId: String, value: Boolean) {
        val me = _currentUser.value ?: return
        viewModelScope.launch {
            repo.setFavorite(me = me, recipeId = recipeId, value = value)
        }
    }
}
