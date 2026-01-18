@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.receitas.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.receitas.ui.RecipesViewModel
import com.example.receitas.ui.ai.AiRecipeScreen
import com.example.receitas.ui.ai.AiViewModel

enum class HomeTab { ALL, MINE, FAVORITES }

private object HomeRoutes {
    const val ALL = "all"
    const val MINE = "mine"
    const val FAVORITES = "favorites"
    const val AI = "ai"
}

@Composable
fun HomeScreen(
    vm: RecipesViewModel,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onLogout: () -> Unit
) {
    val innerNav = rememberNavController()
    val backStack by innerNav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: HomeRoutes.ALL

    val isAi = route == HomeRoutes.AI

    val title = when (route) {
        HomeRoutes.MINE -> "Minhas receitas 👤"
        HomeRoutes.FAVORITES -> "Favoritas ❤️"
        HomeRoutes.AI -> "Receitas por IA ✨"
        else -> "Receitas 🍲"
    }

    fun go(dest: String) {
        innerNav.navigate(dest) {
            launchSingleTop = true
            restoreState = true
            popUpTo(innerNav.graph.findStartDestination().id) { saveState = true }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Outlined.Logout,
                            contentDescription = "Sair",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },

        floatingActionButton = {
            if (!isAi) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Nova receita") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },

        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                NavigationBarItem(
                    selected = route == HomeRoutes.ALL,
                    onClick = { go(HomeRoutes.ALL) },
                    icon = { Icon(Icons.Outlined.MenuBook, contentDescription = null) },
                    label = { Text("Início") },
                    colors = itemColors
                )

                NavigationBarItem(
                    selected = route == HomeRoutes.MINE,
                    onClick = { go(HomeRoutes.MINE) },
                    icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
                    label = { Text("Minhas") },
                    colors = itemColors
                )

                NavigationBarItem(
                    selected = route == HomeRoutes.FAVORITES,
                    onClick = { go(HomeRoutes.FAVORITES) },
                    icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
                    label = { Text("Favoritas") },
                    colors = itemColors
                )

                NavigationBarItem(
                    selected = route == HomeRoutes.AI,
                    onClick = { go(HomeRoutes.AI) },
                    icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                    label = { Text("IA") },
                    colors = itemColors
                )
            }
        }
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 12.dp)
        ) {
            NavHost(
                navController = innerNav,
                startDestination = HomeRoutes.ALL
            ) {
                composable(HomeRoutes.ALL) {
                    RecipeListScreen(
                        vm = vm,
                        tab = HomeTab.ALL,
                        onOpen = onOpen,
                        contentPadding = PaddingValues(bottom = pad.calculateBottomPadding())
                    )
                }

                composable(HomeRoutes.MINE) {
                    RecipeListScreen(
                        vm = vm,
                        tab = HomeTab.MINE,
                        onOpen = onOpen,
                        contentPadding = PaddingValues(bottom = pad.calculateBottomPadding())
                    )
                }

                composable(HomeRoutes.FAVORITES) {
                    RecipeListScreen(
                        vm = vm,
                        tab = HomeTab.FAVORITES,
                        onOpen = onOpen,
                        contentPadding = PaddingValues(bottom = pad.calculateBottomPadding())
                    )
                }

                composable(HomeRoutes.AI) {
                    val aiVm: AiViewModel = viewModel()

                    AiRecipeScreen(
                        vm = aiVm,
                        onUseDraft = { draft ->
                            vm.setAiDraft(draft)
                            onAdd()
                        }
                    )
                }
            }
        }
    }
}
