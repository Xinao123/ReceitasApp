package com.example.receitas

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.receitas.ui.RecipesViewModel
import com.example.receitas.ui.auth.AuthScreen
import com.example.receitas.ui.auth.AuthState
import com.example.receitas.ui.auth.AuthViewModel
import com.example.receitas.ui.screens.HomeScreen
import com.example.receitas.ui.screens.RecipeDetailScreen
import com.example.receitas.ui.screens.RecipeEditScreen
import com.example.receitas.ui.theme.ReceitasTheme

private object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{id}"
    const val EDIT_NEW = "edit"
    const val EDIT_EXISTING = "edit/{id}"

    fun detail(id: String) = "detail/${Uri.encode(id)}"
    fun edit(id: String) = "edit/${Uri.encode(id)}"
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ReceitasTheme {
                val authVm: AuthViewModel = viewModel()
                val authState by authVm.state.collectAsState()

                when (val s = authState) {
                    is AuthState.Loading -> LoadingScreen()

                    is AuthState.LoggedOut -> AuthScreen(vm = authVm)

                    is AuthState.LoggedIn -> {
                        val vm: RecipesViewModel = viewModel()
                        val nav = rememberNavController()

                        val me = s.uid
                        LaunchedEffect(me) { vm.setCurrentUser(me) }

                        NavHost(
                            navController = nav,
                            startDestination = Routes.HOME
                        ) {
                            composable(Routes.HOME) {
                                HomeScreen(
                                    vm = vm,
                                    onOpen = { id -> nav.navigate(Routes.detail(id)) },
                                    onAdd = { nav.navigate(Routes.EDIT_NEW) },
                                    onLogout = { authVm.signOut() }
                                )
                            }

                            composable(
                                route = Routes.DETAIL,
                                arguments = listOf(navArgument("id") { type = NavType.StringType })
                            ) { backStack ->
                                val id = backStack.arguments?.getString("id") ?: ""
                                RecipeDetailScreen(
                                    recipeId = id,
                                    vm = vm,
                                    onBack = { nav.popBackStack() },
                                    onEdit = { rid -> nav.navigate(Routes.edit(rid)) }
                                )
                            }


                            composable(Routes.EDIT_NEW) {
                                RecipeEditScreen(
                                    recipeId = "",
                                    vm = vm,
                                    onBack = { nav.popBackStack() }
                                )
                            }


                            composable(
                                route = Routes.EDIT_EXISTING,
                                arguments = listOf(navArgument("id") { type = NavType.StringType })
                            ) { backStack ->
                                val id = backStack.arguments?.getString("id") ?: ""
                                RecipeEditScreen(
                                    recipeId = id,
                                    vm = vm,
                                    onBack = { nav.popBackStack() }
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text(
                text = "Carregando suas receitas… 🍲",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
