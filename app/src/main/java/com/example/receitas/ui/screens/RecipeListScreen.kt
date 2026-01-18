@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.receitas.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.receitas.data.Recipe
import com.example.receitas.ui.RecipesViewModel

@Composable
fun RecipeListScreen(
    vm: RecipesViewModel,
    tab: HomeTab,
    onOpen: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val recipes by vm.recipes.collectAsState()
    val me by vm.currentUser.collectAsState()

    var q by remember { mutableStateOf("") }

    val filtered = remember(recipes, tab, me) {
        when (tab) {
            HomeTab.ALL -> recipes
            HomeTab.MINE -> recipes.filter { vm.canEdit(it, me) } // ✅ sem ownerUsername aqui
            HomeTab.FAVORITES -> recipes.filter { it.isFavorite }
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 6.dp)
    ) {
        // 🔎 Busca
        OutlinedTextField(
            value = q,
            onValueChange = {
                q = it
                vm.setQuery(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            singleLine = true,
            placeholder = {
                Text(
                    when (tab) {
                        HomeTab.ALL -> "Buscar receitas"
                        HomeTab.MINE -> "Buscar nas minhas"
                        HomeTab.FAVORITES -> "Buscar nas favoritas"
                    }
                )
            },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (q.isNotBlank()) {
                    IconButton(onClick = {
                        q = ""
                        vm.setQuery("")
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Limpar")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {


            AssistChip(
                onClick = { },
                label = {
                    Text(
                        when (tab) {
                            HomeTab.ALL -> "Tudo"
                            HomeTab.MINE -> "Só você"
                            HomeTab.FAVORITES -> "Corações"
                        }
                    )
                }
            )
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            EmptyState(tab = tab)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 0.dp,
                    bottom = 120.dp // ✅ fixo pra não depender de calculateBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { r ->
                    RecipeCardPremium(
                        recipe = r,
                        isMine = vm.canEdit(r, me), // ✅ sem ownerUsername aqui
                        onClick = { onOpen(r.id.toString()) }, // ✅ manda STRING
                        onFav = { vm.toggleFavorite(r.id) } // mantém Long pq teu VM ainda tá Long
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(tab: HomeTab) {
    val (title, subtitle) = when (tab) {
        HomeTab.ALL -> "Nada por aqui 😴" to "Cria uma receita ou usa a IA."
        HomeTab.MINE -> "Você não criou nada ainda 👀" to "Bora lançar a primeira."
        HomeTab.FAVORITES -> "Sem favoritas 💔" to "Marca as que você curtir."
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecipeCardPremium(
    recipe: Recipe,
    isMine: Boolean,
    onClick: () -> Unit,
    onFav: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatPill(icon = Icons.Outlined.Schedule, text = "${recipe.timeMinutes} min")
                        StatPill(icon = Icons.Outlined.PersonOutline, text = "${recipe.servings} porções")
                    }
                }

                IconButton(onClick = onFav) {
                    Icon(
                        imageVector = if (recipe.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = if (recipe.isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(recipe.category.ifBlank { "Geral" }) }
                )

                if (isMine) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Minha") }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
