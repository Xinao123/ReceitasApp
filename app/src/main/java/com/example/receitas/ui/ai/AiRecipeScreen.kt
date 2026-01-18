@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.example.receitas.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@Composable
fun AiRecipeScreen(
    vm: AiViewModel,
    onUseDraft: (AiDraft) -> Unit
) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    val hasIngredients = state.ingredients.trim().length >= 3


    LaunchedEffect(state.results.size) {
        if (state.results.isNotEmpty()) listState.animateScrollToItem(2)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Me passa os ingredientes e eu te devolvo 3 sugestões completas. Bem direto ao ponto 😌",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            RequestCard(
                servings = state.servings,
                ingredients = state.ingredients,
                restrictions = state.restrictions,
                cuisine = state.cuisine,
                allowExtras = state.allowExtras,              // ✅ NOVO
                onAllowExtras = vm::setAllowExtras,          // ✅ NOVO
                isLoading = state.isLoading,
                error = state.error,
                resultsEmpty = state.results.isEmpty(),
                hasIngredients = hasIngredients,
                onServings = vm::setServings,
                onIngredients = vm::setIngredients,
                onRestrictions = vm::setRestrictions,
                onCuisine = vm::setCuisine,
                onGenerate = vm::generate,
                onClear = {
                    vm.setIngredients("")
                    vm.setRestrictions("")
                    vm.setCuisine("")
                    vm.setServings(2)
                    vm.setAllowExtras(false)
                },
                onExample = {
                    vm.setIngredients("ovo\narroz\ncebola\nalho\nmolho de tomate")
                    vm.setRestrictions("")
                    vm.setCuisine("fogão")
                    vm.setServings(2)
                    vm.setAllowExtras(true)
                }
            )
        }

        if (state.results.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Escolhe uma:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${state.results.size} sugestões para ${state.servings} porções",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(state.results, key = { idx, r -> "${r.id}_${idx}" }) { _, r ->
                AiRecipeCard(
                    r = r,
                    onEdit = { onUseDraft(vm.toDraft(r)) },
                    onUse = { onUseDraft(vm.toDraft(r)) }
                )
            }
        }
    }
}

@Composable
private fun RequestCard(
    servings: Int,
    ingredients: String,
    restrictions: String,
    cuisine: String,
    allowExtras: Boolean,
    onAllowExtras: (Boolean) -> Unit,
    isLoading: Boolean,
    error: String?,
    resultsEmpty: Boolean,
    hasIngredients: Boolean,
    onServings: (Int) -> Unit,
    onIngredients: (String) -> Unit,
    onRestrictions: (String) -> Unit,
    onCuisine: (String) -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    onExample: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header compacto + menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Seu pedido", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Porções + ingredientes. O resto é opcional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Mais")
                }

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Limpar") },
                        leadingIcon = { Icon(Icons.Outlined.CleaningServices, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onClear()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Exemplo") },
                        leadingIcon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onExample()
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ServingsRow(
                servings = servings,
                onChange = onServings
            )

            OutlinedTextField(
                value = ingredients,
                onValueChange = onIngredients,
                label = { Text("Ingredientes") },
                placeholder = { Text("Ex: picanha\nsal grosso\n(ou separado por vírgula)") },
                supportingText = { Text("Dica: quanto mais específico, melhor (ex: “picanha 1kg”).") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )


            AllowExtrasRow(
                allowExtras = allowExtras,
                onToggle = onAllowExtras
            )


            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { advancedOpen = !advancedOpen },
                    label = { Text(if (advancedOpen) "Menos opções" else "Mais opções") },
                    leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                    trailingIcon = {
                        Icon(
                            if (advancedOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null
                        )
                    }
                )

                if (!advancedOpen && (restrictions.isNotBlank() || cuisine.isNotBlank())) {
                    Text(
                        "• configurado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }

            AnimatedVisibility(visible = advancedOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = restrictions,
                            onValueChange = onRestrictions,
                            label = { Text("Restrições") },
                            placeholder = { Text("Sem lactose, low carb…") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cuisine,
                            onValueChange = onCuisine,
                            label = { Text("Equipamento") },
                            placeholder = { Text("Air fryer, forno…") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    EquipmentChips(
                        cuisine = cuisine,
                        onPick = onCuisine
                    )
                }
            }

            Button(
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth(),
                enabled = hasIngredients && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                    Text("Gerando…", modifier = Modifier.padding(start = 10.dp))
                } else {
                    Icon(Icons.Outlined.Whatshot, contentDescription = null)
                    Text(
                        if (allowExtras) "Gerar 3 sugestões (com extras)" else "Gerar 3 sugestões (estrito)",
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!isLoading && !hasIngredients) {
                Text(
                    "Coloca pelo menos 2 ingredientes pra eu conseguir mandar umas ideias boas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            error?.let { msg ->
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (!isLoading && resultsEmpty) {
                AssistChip(
                    onClick = { onIngredients("ovo\narroz\nfrango\ncebola\nalho") },
                    label = { Text("Dica: toca aqui pra colar um combo campeão 😌") }
                )
            }
        }
    }
}

@Composable
private fun AllowExtrasRow(
    allowExtras: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                    Text(
                        " Permitir extras",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    if (allowExtras)
                        "A IA pode sugerir temperos/molhos (vai aparecer em “Vai precisar também”)."
                    else
                        "Modo estrito: só usa os ingredientes que você digitou.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = allowExtras,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun ServingsRow(
    servings: Int,
    onChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Porções", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onChange(servings - 1) }, enabled = servings > 1) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Menos")
                }
                Text(
                    servings.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { onChange(servings + 1) }, enabled = servings < 12) {
                    Icon(Icons.Outlined.Add, contentDescription = "Mais")
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1, 2, 4, 6, 12).forEach { v ->
                FilterChip(
                    selected = servings == v,
                    onClick = { onChange(v) },
                    label = { Text("$v") }
                )
            }
        }
    }
}

@Composable
private fun EquipmentChips(
    cuisine: String,
    onPick: (String) -> Unit
) {
    val cur = cuisine.lowercase()

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = cur.contains("forno"),
            onClick = { onPick("forno") },
            label = { Text("Forno") }
        )
        FilterChip(
            selected = cur.contains("air fryer") || cur.contains("airfryer"),
            onClick = { onPick("air fryer") },
            label = { Text("Air fryer") }
        )
        FilterChip(
            selected = cur.contains("fog"),
            onClick = { onPick("fogão") },
            label = { Text("Fogão") }
        )
    }
}
