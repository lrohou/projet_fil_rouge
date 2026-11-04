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
 * ViewModel gérant la création de compte.
 * Valide la saisie des informations (pseudo, email, mot de passe) et communique avec le repository.
 */
class SignUpViewModel @JvmOverloads constructor(
    application: Application,
    private val repo: AuthRepository = AuthRepository()
) : AndroidViewModel(application) {

    var displayName by mutableStateOf("")
        private set
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onDisplayNameChange(value: String) { displayName = value; errorMessage = null }
    fun onEmailChange(value: String) { email = value; errorMessage = null }
    fun onPasswordChange(value: String) { password = value; errorMessage = null }

    /** Valide le formulaire et exécute la création de compte */
    fun signUp(onSuccess: () -> Unit) {
        val app = getApplication<Application>()
        if (displayName.isBlank()) { errorMessage = app.getString(R.string.error_username_required); return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            errorMessage = app.getString(R.string.error_invalid_email); return
        }
        if (password.length < 6) { errorMessage = app.getString(R.string.error_password_too_short); return }

        viewModelScope.launch {
            isLoading = true
            repo.signUp(displayName.trim(), email.trim(), password)
                .onSuccess { onSuccess() }
                .onFailure { errorMessage = it.toAuthMessage(app) }
            isLoading = false
        }
    }
}
