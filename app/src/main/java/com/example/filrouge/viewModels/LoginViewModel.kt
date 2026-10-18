import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filrouge.data.AuthRepository
import com.example.filrouge.data.toAuthMessage
import kotlinx.coroutines.launch

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

    private fun emailValid() = Patterns.EMAIL_ADDRESS.matcher(username.trim()).matches()

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

    fun onGoogleError() {
        errorMessage = "Connexion Google impossible"
    }

    fun resetPassword() {
        if (!emailValid()) { errorMessage = "Entre ton e-mail pour réinitialiser"; return }
        viewModelScope.launch {
            repo.resetPassword(username.trim())
                .onSuccess { infoMessage = "E-mail de réinitialisation envoyé" }
                .onFailure { errorMessage = it.toAuthMessage() }
        }
    }
}