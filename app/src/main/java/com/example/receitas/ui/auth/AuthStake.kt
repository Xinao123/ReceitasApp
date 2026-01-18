package com.example.receitas.ui.auth

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val uid: String, val email: String?) : AuthState
}
