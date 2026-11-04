package com.example.filrouge.auth

import android.app.Application
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filrouge.R
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran de connexion.
 * Gère la validation des champs, l'état du chargement et les opérations de connexion / mot de passe oublié.
 */
class LoginViewModel @JvmOverloads constructor(
    application: Application,
    private val repo: AuthRepository = AuthRepository()
) : AndroidViewModel(application) {

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
        val app = getApplication<Application>()
        if (!emailValid()) { errorMessage = app.getString(R.string.error_invalid_email); return }
        if (password.length < 6) { errorMessage = app.getString(R.string.error_password_too_short); return }
        viewModelScope.launch {
            isLoading = true
            repo.signIn(username.trim(), password)
                .onSuccess { onSuccess() }
                .onFailure { errorMessage = it.toAuthMessage(app) }
            isLoading = false
        }
    }

    /** Déclenche l'authentification avec Google */
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            isLoading = true
            repo.signInWithGoogle(idToken)
                .onSuccess { onSuccess() }
                .onFailure {
                    errorMessage = it.toAuthMessage(app)
                }
            isLoading = false
        }
    }

    /** Gère l'échec d'initialisation de Google Sign-In */
    fun onGoogleError() {
        errorMessage = getApplication<Application>().getString(R.string.error_google_sign_in)
    }

    /** Envoie un e-mail de réinitialisation de mot de passe à l'adresse renseignée */
    fun resetPassword() {
        val app = getApplication<Application>()
        if (!emailValid()) { errorMessage = app.getString(R.string.error_enter_email_to_reset); return }
        viewModelScope.launch {
            repo.resetPassword(username.trim())
                .onSuccess { infoMessage = app.getString(R.string.info_reset_email_sent) }
                .onFailure { errorMessage = it.toAuthMessage(app) }
        }
    }
}
