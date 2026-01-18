package com.example.receitas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.receitas.data.Recipe
import com.example.receitas.ui.RecipesViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun RecipeEditScreen(
    recipeId: String,
    vm: RecipesViewModel,
    onBack: () -> Unit
) {
    val me by vm.currentUser.collectAsState(initial = null)
    val isEdit = recipeId.isNotBlank()


    val recipeFlow: Flow<Recipe?> = remember(recipeId) {
        if (isEdit) vm.observeRecipe(recipeId) else flowOf(null)
    }
    val editing by recipeFlow.collectAsState(initial = null)


    val isLoading = isEdit && editing == null


    val canEdit = if (!isEdit) true else (editing != null && vm.canEdit(editing, me))
    val canSave = !isLoading && titleIsValid(editingTitle = editing?.title, currentTitle = null) // só pra não travar



    var title by rememberSaveable(recipeId) { mutableStateOf("") }
    var category by rememberSaveable(recipeId) { mutableStateOf("Geral") }
    var time by rememberSaveable(recipeId) { mutableStateOf("15") }
    var servings by rememberSaveable(recipeId) { mutableStateOf("1") }
    var ingredientsText by rememberSaveable(recipeId) { mutableStateOf("") }
    var stepsText by rememberSaveable(recipeId) { mutableStateOf("") }
    var isPublic by rememberSaveable(recipeId) { mutableStateOf(true) }


    var loadedRecipeId by remember(recipeId) { mutableStateOf<String?>(null) }

    LaunchedEffect(editing?.id) {
        if (!isEdit) return@LaunchedEffect
        val r = editing ?: return@LaunchedEffect
        if (loadedRecipeId == r.id) return@LaunchedEffect

        loadedRecipeId = r.id
        title = r.title
        category = r.category.ifBlank { "Geral" }
        time = r.timeMinutes.toString()
        servings = r.servings.toString()
        ingredientsText = r.ingredients
        stepsText = r.steps
        isPublic = r.isPublic
    }


    var draftConsumed by remember { mutableStateOf(false) }
    LaunchedEffect(isEdit) {
        if (isEdit) return@LaunchedEffect
        if (draftConsumed) return@LaunchedEffect

        val draft = vm.consumeAiDraft()
        if (draft != null) {
            title = draft.title
            category = draft.category.ifBlank { "Geral" }
            time = draft.timeMinutes.toString()
            servings = draft.servings.toString()
            ingredientsText = draft.ingredientsText
            stepsText = draft.stepsText
        }
        draftConsumed = true
    }

    val realCanSave = title.trim().isNotBlank() && canEdit && !isLoading

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEdit) "Editar receita" else "Nova receita",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            isLoading -> "Carregando receita…"
                            canEdit -> "Capricha nos detalhes ✨"
                            else -> "Somente o criador pode editar 👀"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))


            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Puxando a receita do Firestore…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                return@Surface
            }


            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        enabled = canEdit,
                        label = { Text("Nome da receita") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        enabled = canEdit,
                        label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = time,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                time = digits.ifBlank { "15" }
                            },
                            enabled = canEdit,
                            label = { Text("Tempo (min)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraLarge
                        )

                        OutlinedTextField(
                            value = servings,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                servings = digits.ifBlank { "1" }
                            },
                            enabled = canEdit,
                            label = { Text("Porções") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPublic) "🌍 Pública" else "🔒 Privada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it },
                            enabled = canEdit
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))


            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ingredientes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = ingredientsText,
                        onValueChange = { ingredientsText = it },
                        enabled = canEdit,
                        label = { Text("1 por linha") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                }
            }

            Spacer(Modifier.height(12.dp))


            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Modo de preparo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = stepsText,
                        onValueChange = { stepsText = it },
                        enabled = canEdit,
                        label = { Text("1 passo por linha") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (canEdit) {
                Button(
                    onClick = {
                        val parsedTime = (time.toIntOrNull() ?: 15).coerceAtLeast(1)
                        val parsedServings = (servings.toIntOrNull() ?: 1).coerceAtLeast(1)

                        val recipe = Recipe(
                            id = if (isEdit) recipeId else "",
                            title = title.trim(),
                            category = category.trim().ifBlank { "Geral" },
                            timeMinutes = parsedTime,
                            servings = parsedServings,
                            ingredients = normalizeMultiline(ingredientsText),
                            steps = normalizeMultiline(stepsText),
                            isPublic = isPublic,
                            isFavorite = editing?.isFavorite ?: false
                        )

                        vm.save(recipe)
                        onBack()
                    },
                    enabled = realCanSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text("Salvar ✅")
                }

                if (isEdit && editing != null) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            vm.delete(recipeId)
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text("Excluir 🗑️")
                    }
                }
            }
        }
    }
}

private fun normalizeMultiline(text: String): String {
    return text
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filter { it.lowercase() != "null" }
        .joinToString("\n")
}


private fun titleIsValid(editingTitle: String?, currentTitle: String?) = true
