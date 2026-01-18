package com.example.receitas.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreRecipesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val recipesCol = db.collection("recipes")


    private fun favCol(me: String) =
        db.collection("users").document(me).collection("favorites")

    private fun docToRecipe(docId: String, m: Map<String, Any?>): Recipe {
        fun s(key: String) = (m[key] as? String).orEmpty()
        fun b(key: String, def: Boolean) = (m[key] as? Boolean) ?: def

        fun l(key: String): Long = when (val v = m[key]) {
            is Long -> v
            is Int -> v.toLong()
            is Double -> v.toLong()
            is Float -> v.toLong()
            else -> 0L
        }

        fun i(key: String, def: Int) = when (val v = m[key]) {
            is Long -> v.toInt()
            is Int -> v
            is Double -> v.toInt()
            is Float -> v.toInt()
            else -> def
        }

        fun linesFromAny(key: String): String {
            val v = m[key]
            return when (v) {
                is String -> v.trim()
                is List<*> -> v
                    .mapNotNull { it?.toString()?.trim() }
                    .filter { it.isNotBlank() && it.lowercase() != "null" }
                    .joinToString("\n")
                else -> ""
            }
        }

        return Recipe(
            id = docId,
            ownerUid = s("ownerUid"),
            title = s("title").ifBlank { "Sem título" },
            category = s("category").ifBlank { "Geral" },
            timeMinutes = i("timeMinutes", 15).coerceAtLeast(1),
            servings = i("servings", 1).coerceAtLeast(1),
            ingredients = linesFromAny("ingredients"),
            steps = linesFromAny("steps"),

            // ⚠️ isso aqui vai ser sobrescrito pelo set de favoritos
            isFavorite = b("isFavorite", false),

            isPublic = b("isPublic", false),
            createdAt = l("createdAt"),
            updatedAt = l("updatedAt"),
        )
    }

    private fun matchesQuery(r: Recipe, q: String): Boolean {
        val needle = q.trim()
        if (needle.isBlank()) return true

        val n = needle.lowercase()
        val hay = buildString {
            append(r.title.lowercase())
            append(" ")
            append(r.category.lowercase())
            append(" ")
            append(r.ingredients.lowercase())
        }

        return hay.contains(n)
    }

    private fun textToList(text: String): List<String> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.lowercase() != "null" }


    fun observeFavoriteIds(me: String): Flow<Set<String>> = callbackFlow {
        val reg = favCol(me).addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptySet())
                return@addSnapshotListener
            }
            val ids = snap?.documents.orEmpty().map { it.id }.toSet()
            trySend(ids)
        }
        awaitClose { reg.remove() }
    }

    fun observeVisibleRecipes(me: String, q: String): Flow<List<Recipe>> = callbackFlow {
        var publicMap: Map<String, Recipe> = emptyMap()
        var mineMap: Map<String, Recipe> = emptyMap()
        var favIds: Set<String> = emptySet()

        fun emit() {
            val merged = LinkedHashMap<String, Recipe>()
            publicMap.values.forEach { merged[it.id] = it }
            mineMap.values.forEach { merged[it.id] = it }

            val out = merged.values
                .filter { it.isPublic || it.ownerUid == me }
                .map { r -> r.copy(isFavorite = favIds.contains(r.id)) }
                .filter { matchesQuery(it, q) }
                .sortedByDescending { it.updatedAt }

            trySend(out)
        }

        val regs = mutableListOf<ListenerRegistration>()


        regs += recipesCol
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val docs = snap?.documents.orEmpty()
                publicMap = docs.associate { d ->
                    val data = d.data ?: emptyMap()
                    d.id to docToRecipe(d.id, data)
                }
                emit()
            }


        regs += recipesCol
            .whereEqualTo("ownerUid", me)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val docs = snap?.documents.orEmpty()
                mineMap = docs.associate { d ->
                    val data = d.data ?: emptyMap()
                    d.id to docToRecipe(d.id, data)
                }
                emit()
            }


        regs += favCol(me).addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            favIds = snap?.documents.orEmpty().map { it.id }.toSet()
            emit()
        }

        awaitClose { regs.forEach { it.remove() } }
    }


    fun observeRecipe(id: String, me: String): Flow<Recipe?> = callbackFlow {
        var latestRecipe: Recipe? = null
        var fav = false

        fun emit() {
            val r = latestRecipe ?: run {
                trySend(null); return
            }
            val visible = r.isPublic || r.ownerUid == me
            trySend(if (visible) r.copy(isFavorite = fav) else null)
        }

        val regRecipe = recipesCol.document(id).addSnapshotListener { snap, err ->
            if (err != null) {
                latestRecipe = null
                emit()
                return@addSnapshotListener
            }
            val data = snap?.data ?: run {
                latestRecipe = null
                emit()
                return@addSnapshotListener
            }
            latestRecipe = docToRecipe(snap.id, data)
            emit()
        }

        val regFav = favCol(me).document(id).addSnapshotListener { snap, _ ->
            fav = snap?.exists() == true
            emit()
        }

        awaitClose {
            regRecipe.remove()
            regFav.remove()
        }
    }


    fun save(me: String, recipe: Recipe) {
        val now = System.currentTimeMillis()
        val isNew = recipe.id.isBlank()

        val docRef = if (isNew) recipesCol.document() else recipesCol.document(recipe.id)

        val payload = hashMapOf<String, Any?>(
            "ownerUid" to me,
            "title" to recipe.title.trim(),
            "category" to recipe.category.trim(),
            "timeMinutes" to recipe.timeMinutes,
            "servings" to recipe.servings,
            "ingredients" to textToList(recipe.ingredients),
            "steps" to textToList(recipe.steps),

            // ✅ favorito não mora aqui
            // "isFavorite" to recipe.isFavorite,

            "isPublic" to recipe.isPublic,
            "updatedAt" to now,
        )

        if (isNew) payload["createdAt"] = now

        docRef.set(payload, SetOptions.merge())
    }

    fun delete(me: String, recipeId: String) {
        recipesCol.document(recipeId).get().addOnSuccessListener { snap ->
            val owner = snap.getString("ownerUid").orEmpty()
            if (owner == me) recipesCol.document(recipeId).delete()
        }
    }

    fun setPublic(me: String, recipeId: String, isPublic: Boolean) {
        val now = System.currentTimeMillis()
        recipesCol.document(recipeId).get().addOnSuccessListener { snap ->
            val owner = snap.getString("ownerUid").orEmpty()
            if (owner == me) recipesCol.document(recipeId)
                .update(mapOf("isPublic" to isPublic, "updatedAt" to now))
        }
    }


    fun toggleFavorite(me: String, recipeId: String) {
        val ref = favCol(me).document(recipeId)
        ref.get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                ref.delete()
            } else {
                ref.set(mapOf("createdAt" to System.currentTimeMillis()), SetOptions.merge())
            }
        }
    }

    fun setFavorite(me: String, recipeId: String, value: Boolean) {
        val ref = favCol(me).document(recipeId)
        if (value) {
            ref.set(mapOf("createdAt" to System.currentTimeMillis()), SetOptions.merge())
        } else {
            ref.delete()
        }
    }
}
