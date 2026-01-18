@file:OptIn(ExperimentalLayoutApi::class)

package com.example.receitas.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AiRecipeCard(
    r: AiRecipeSuggestion,
    onEdit: () -> Unit,
    onUse: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    fun clean(s: String?): String? {
        val t = s?.trim()
        if (t.isNullOrBlank()) return null
        if (t.equals("null", ignoreCase = true)) return null
        return t
    }

    val title = r.title.ifBlank { "Receita sem título" }
    val notes = clean(r.notes)
    val ingredientsClean = r.ingredients.mapNotNull(::clean)
    val stepsClean = r.steps.mapNotNull(::clean)
    val extraClean = r.extraNeeded.mapNotNull(::clean)

    val ingredientsCount = ingredientsClean.size
    val stepsCount = stepsClean.size
    val extrasCount = extraClean.size

    val previewIng = ingredientsClean.take(4).joinToString(", ")
    val previewStep = stepsClean.firstOrNull()?.let { clean(it) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ===== Header =====
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (expanded) 3 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetaPill(
                            icon = Icons.Outlined.Restaurant,
                            text = "${r.servings} porções"
                        )
                        MetaPill(
                            icon = Icons.Outlined.Schedule,
                            text = "${r.timeMinutes} min"
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Recolher" else "Expandir"
                    )
                }
            }

            // ===== Chips =====
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cat = r.category.ifBlank { "Geral" }
                SuggestionChip(onClick = { }, label = { Text(cat) })
                SuggestionChip(onClick = { }, label = { Text("$ingredientsCount ing") })
                SuggestionChip(onClick = { }, label = { Text("$stepsCount passos") })
                if (extrasCount > 0) {
                    SuggestionChip(onClick = { }, label = { Text("$extrasCount extras") })
                }
            }

            // ===== Preview (colapsado) =====
            if (!expanded) {
                if (previewIng.isNotBlank()) {
                    Text(
                        text = "Ingredientes: $previewIng${if (ingredientsCount > 4) "…" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Ingredientes: (não veio nada 😅)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (extrasCount > 0) {
                    val extraPreview = extraClean.take(2).joinToString(", ")
                    Text(
                        text = "Extras: $extraPreview${if (extrasCount > 2) "…" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                previewStep?.let {
                    Text(
                        text = "1º passo: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ===== Expandido (detalhes) =====
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    SectionTitle("Ingredientes")
                    BulletList(ingredientsClean.ifEmpty { listOf("Nada de ingredientes veio da IA 😅") })

                    if (extraClean.isNotEmpty()) {
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SectionTitle("Vai precisar também")
                        BulletList(extraClean)
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SectionTitle("Modo de preparo")

                    if (stepsClean.isEmpty()) {
                        Text("Sem passos ainda 😅", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            stepsClean.forEachIndexed { idx, step ->
                                Text(
                                    text = "${idx + 1}. $step",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    notes?.let {
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ===== Ações =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(if (expanded) "Fechar" else "Detalhes")
                }

                Button(
                    onClick = onUse,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Text("Usar", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun MetaPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(
        text = t,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
    }
}
