package com.example.filrouge.viewModels

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filrouge.data.AuthRepository
import com.example.filrouge.data.toAuthMessage
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

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

    fun signUp(onSuccess: () -> Unit) {
        if (displayName.isBlank()) { errorMessage = "Pseudo requis"; return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            errorMessage = "Adresse e-mail invalide"; return
        }
        if (password.length < 6) { errorMessage = "6 caractères minimum"; return }

        viewModelScope.launch {
            isLoading = true
            repo.signUp(displayName.trim(), email.trim(), password)
                .onSuccess { onSuccess() }
                .onFailure { errorMessage = it.toAuthMessage() }
            isLoading = false
        }
    }
}