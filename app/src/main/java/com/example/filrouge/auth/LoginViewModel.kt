package com.example.filrouge.auth

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran de connexion.
 * Gère la validation des champs, l'état du chargement et les opérations de connexion / mot de passe oublié.
 */
class LoginViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    var username by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var infoMessage by mutableStateOf<String?>(null)
        private set

    fun onUsernameChange(value: String) { username = value; errorMessage = null }
    fun onPasswordChange(value: String) { password = value; errorMessage = null }

    /** Vérifie la validité du format d'adresse e-mail */
    private fun emailValid() = Patterns.EMAIL_ADDRESS.matcher(username.trim()).matches()

    /** Déclenche la tentative de connexion e-mail/mot de passe */
    fun signIn(onSuccess: () -> Unit) {
        if (!emailValid()) { errorMessage = "Adresse e-mail invalide"; return }
        if (password.length < 6) { errorMessage = "6 caractères minimum"; return }
        viewModelScope.launch {
            isLoading = true
            repo.signIn(username.trim(), password)
                .onSuccess { onSuccess() }
                .onFailure { errorMessage = it.toAuthMessage() }
            isLoading = false
        }
    }

    /** Déclenche l'authentification avec Google */
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            repo.signInWithGoogle(idToken)
                .onSuccess { onSuccess() }
                .onFailure {
                    errorMessage = it.toAuthMessage()
                }
            isLoading = false
        }
    }

    /** Gère l'échec d'initialisation de Google Sign-In */
    fun onGoogleError() {
        errorMessage = "Connexion Google impossible"
    }

    /** Envoie un e-mail de réinitialisation de mot de passe à l'adresse renseignée */
    fun resetPassword() {
        if (!emailValid()) { errorMessage = "Entre ton e-mail pour réinitialiser"; return }
        viewModelScope.launch {
            repo.resetPassword(username.trim())
                .onSuccess { infoMessage = "E-mail de réinitialisation envoyé" }
                .onFailure { errorMessage = it.toAuthMessage() }
        }
    }
}
