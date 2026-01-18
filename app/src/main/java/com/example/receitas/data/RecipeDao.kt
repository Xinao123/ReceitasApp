package com.example.receitas.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes WHERE isPublic = 1 ORDER BY id DESC")
    fun observeVisibleRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeById(id: Long): Flow<Recipe?>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): Recipe?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recipe: Recipe): Long

    @Query("DELETE FROM recipes WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun deleteByIdIfOwner(id: Long, ownerUid: String): Int

    @Query("UPDATE recipes SET isPublic = 1 WHERE id = :id AND ownerUid = :ownerUid")
    suspend fun setPublicIfOwner(id: Long, ownerUid: String): Int

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun countAll(): Int
}
