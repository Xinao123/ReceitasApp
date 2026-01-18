package com.example.receitas.ui.ai

data class AiRecipeSuggestion(
    val id: String,
    val title: String,
    val servings: Int,
    val timeMinutes: Int,
    val category: String,
    val ingredients: List<String>,
    val steps: List<String>,
    val extraNeeded: List<String> = emptyList(),
    val notes: String? = null
)

data class AiDraft(
    val title: String,
    val category: String,
    val timeMinutes: Int,
    val servings: Int,
    val ingredientsText: String,
    val stepsText: String
)
