package com.example.receitas.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    private var bootResolved = false
    private var bootJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { fb ->
        val u = fb.currentUser

        if (u != null) {

            bootJob?.cancel()
            bootResolved = true
            _state.value = AuthState.LoggedIn(u.uid, u.email)
        } else {

            if (bootResolved) {
                _state.value = AuthState.LoggedOut
            } else {

                scheduleBootFallbackToLoggedOut()
            }
        }
    }

    init {

        auth.currentUser?.let { u ->
            bootResolved = true
            _state.value = AuthState.LoggedIn(u.uid, u.email)
        } ?: run {

            scheduleBootFallbackToLoggedOut()
        }

        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        bootJob?.cancel()
        auth.removeAuthStateListener(authListener)
    }

    private fun scheduleBootFallbackToLoggedOut() {
        bootJob?.cancel()
        bootJob = viewModelScope.launch {

            delay(800)

            if (!bootResolved && auth.currentUser == null && _state.value is AuthState.Loading) {
                bootResolved = true
                _state.value = AuthState.LoggedOut
            }
        }
    }

    fun clearError() { _error.value = null }

    private fun normalizeUserToEmail(user: String): String {
        val raw = user.trim()
        if (raw.contains("@")) return raw

        val safe = raw.lowercase().replace(Regex("[^a-z0-9._-]"), "")
        return "${safe.ifBlank { "user" }}@receitas.app"
    }

    fun signIn(user: String, pass: String) {
        clearError()

        val email = normalizeUserToEmail(user)
        if (pass.length < 6) {
            _error.value = "Senha precisa ter no mínimo 6 caracteres 👀"
            return
        }

        auth.signInWithEmailAndPassword(email, pass)
            .addOnFailureListener { e ->
                _error.value = e.message ?: "Falhou ao entrar 😵"
            }

    }

    fun signUp(user: String, pass: String, pass2: String) {
        clearError()

        if (pass != pass2) {
            _error.value = "As senhas não batem 🤡"
            return
        }
        if (pass.length < 6) {
            _error.value = "Senha precisa ter no mínimo 6 caracteres 👀"
            return
        }

        val email = normalizeUserToEmail(user)

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val uid = res.user?.uid ?: return@addOnSuccessListener

                db.collection("users").document(uid).set(
                    mapOf(
                        "username" to user.trim(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            .addOnFailureListener { e ->
                _error.value = e.message ?: "Falhou ao cadastrar 😵"
            }

    }

    fun signOut() {
        clearError()
        auth.signOut()

    }
}
