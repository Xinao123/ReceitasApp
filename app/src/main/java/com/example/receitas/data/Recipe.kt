package com.example.receitas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey
    val id: String = "",

    val ownerUid: String = "",
    val title: String = "",
    val description: String = "",

    val category: String = "Geral",
    val timeMinutes: Int = 15,
    val servings: Int = 1,


    val ingredients: String = "",
    val steps: String = "",

    val isFavorite: Boolean = false,
    val isPublic: Boolean = false,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
