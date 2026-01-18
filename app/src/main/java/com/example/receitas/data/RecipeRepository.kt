package com.example.receitas.data

import kotlinx.coroutines.flow.Flow

class RecipeRepository(
    private val remote: FirestoreRecipesRepository = FirestoreRecipesRepository()
) {

    fun observeVisibleRecipes(me: String, q: String): Flow<List<Recipe>> {
        return remote.observeVisibleRecipes(me = me, q = q)
    }


    fun observeRecipe(id: String, me: String): Flow<Recipe?> {
        return remote.observeRecipe(id = id, me = me)
    }


    fun save(me: String, recipe: Recipe) {
        remote.save(me = me, recipe = recipe)
    }

    fun delete(me: String, recipeId: String) {
        remote.delete(me = me, recipeId = recipeId)
    }

    fun setPublic(me: String, recipeId: String, isPublic: Boolean) {
        remote.setPublic(me = me, recipeId = recipeId, isPublic = isPublic)
    }

    fun toggleFavorite(me: String, recipeId: String) {
        remote.toggleFavorite(me = me, recipeId = recipeId)
    }
}
