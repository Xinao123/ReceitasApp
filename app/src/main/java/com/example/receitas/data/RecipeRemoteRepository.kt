package com.example.receitas.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class RecipeRemoteRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val col = db.collection("recipes")

    suspend fun create(recipe: Recipe, ownerUid: String): String {
        val now = Timestamp.now()
        val doc = col.document()
        val payload = mapOf(
            "ownerUid" to ownerUid,
            "title" to recipe.title,
            "category" to recipe.category,
            "timeMinutes" to recipe.timeMinutes,
            "servings" to recipe.servings,
            "ingredients" to recipe.ingredients,
            "steps" to recipe.steps,
            "isPublic" to recipe.isPublic,
            "isFavorite" to recipe.isFavorite,
            "createdAt" to now,
            "updatedAt" to now
        )
        doc.set(payload).await()
        return doc.id
    }

    suspend fun update(recipeId: String, recipe: Recipe, ownerUid: String, createdAt: Timestamp?) {
        val now = Timestamp.now()
        val payload = mapOf(
            "ownerUid" to ownerUid,
            "title" to recipe.title,
            "category" to recipe.category,
            "timeMinutes" to recipe.timeMinutes,
            "servings" to recipe.servings,
            "ingredients" to recipe.ingredients,
            "steps" to recipe.steps,
            "isPublic" to recipe.isPublic,
            "isFavorite" to recipe.isFavorite,
            "createdAt" to (createdAt ?: now),
            "updatedAt" to now
        )
        col.document(recipeId).set(payload).await()
    }

    suspend fun delete(recipeId: String) {
        col.document(recipeId).delete().await()
    }

    suspend fun setPublic(recipeId: String, isPublic: Boolean) {
        col.document(recipeId).update(
            mapOf("isPublic" to isPublic, "updatedAt" to Timestamp.now())
        ).await()
    }

    suspend fun toggleFavorite(recipeId: String, current: Boolean) {
        col.document(recipeId).update(
            mapOf("isFavorite" to !current, "updatedAt" to Timestamp.now())
        ).await()
    }

    fun queryPublic(): Query =
        col.whereEqualTo("isPublic", true).orderBy("updatedAt", Query.Direction.DESCENDING)

    fun queryMine(uid: String): Query =
        col.whereEqualTo("ownerUid", uid).orderBy("updatedAt", Query.Direction.DESCENDING)
}
