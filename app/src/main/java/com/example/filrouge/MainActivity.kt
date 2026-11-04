package com.example.filrouge

import com.example.filrouge.auth.LoginViewModel
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filrouge.ui.theme.FilRougeTheme
import androidx.lifecycle.viewmodel.compose.viewModel

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.filrouge.navigation.AppNavigation
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// DataStore pour sauvegarder les paramètres de l'application
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parametres")
val DarkThemeKey = booleanPreferencesKey("is_dark_theme")

/**
 * Activité principale de l'application.
 * Configure le rendu Edge-to-Edge, écoute la préférence du thème sombre via DataStore,
 * et lance le composable de navigation globale.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            //lecture de la préférence du thème dans le DataStore (mémorisé pour éviter de recréer le Flow à chaque recomposition)
            val isDarkTheme by remember(context) {
                context.dataStore.data.map { preferences ->
                    preferences[DarkThemeKey] ?: false
                }
            }.collectAsState(initial = false)

            FilRougeTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { newValue ->
                        // Sauvegarde asynchrone du choix de thème
                        scope.launch {
                            context.dataStore.edit { preferences ->
                                preferences[DarkThemeKey] = newValue
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Formulaire de connexion affichant les champs e-mail/mot de passe,
 * le bouton de connexion Google ainsi que les liens de réinitialisation et d'inscription.
 */
@Composable
fun LoginForm(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.login_app_title), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(48.dp))
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(R.string.desc_app_logo),
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.size(48.dp))

        TextField(
            value = username,
            onValueChange = onUsernameChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            placeholder = { Text(stringResource(R.string.placeholder_email)) }
        )

        Spacer(Modifier.size(8.dp))

        TextField(
            value = password,
            onValueChange = onPasswordChange,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            placeholder = { Text(stringResource(R.string.placeholder_password)) }
        )

        Text(
            stringResource(R.string.forgot_password),
            fontSize = 10.sp,
            color = Color(100, 100, 100),
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onForgotPassword)
        )

        Spacer(Modifier.size(8.dp))

        errorMessage?.let {
            Text(it, color = Color(150, 0, 0), fontSize = 12.sp)
        }
        infoMessage?.let {
            Text(it, fontSize = 12.sp)
        }

        Spacer(Modifier.size(16.dp))

        Button(onClick = onClick, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.sign_in))
        }
        Spacer(Modifier.size(8.dp))

        OutlinedButton(
            onClick = onGoogleClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.continue_with_google))
        }

        Spacer(Modifier.size(72.dp))

        Text(
            buildAnnotatedString {
                append(stringResource(R.string.no_account_yet))
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "signup",
                        styles = TextLinkStyles(style = SpanStyle(color = Color.Blue)),
                        linkInteractionListener = { onSignUpClick() }
                    )
                ) {
                    append(stringResource(R.string.click_here))
                }
                append(stringResource(R.string.to_sign_up))
            },
            fontSize = 14.sp
        )
    }
}

/**
 * Écran principal de connexion gérant la logique d'authentification e-mail
 * et l'intégration de Credential Manager pour l'authentification avec Google.
 */
@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Déclenchement du flux d'authentification Google via CredentialManager
    val onGoogleClick: () -> Unit = {
        scope.launch {
            try {
                val option = GetSignInWithGoogleOption
                    .Builder(context.getString(R.string.default_web_client_id))
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val result = CredentialManager.create(context).getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    viewModel.signInWithGoogle(idToken, onNavigateToHome)
                } else {
                    viewModel.onGoogleError()
                }
            } catch (e: GetCredentialCancellationException) {
            } catch (e: GetCredentialException) {
                viewModel.onGoogleError()
            }
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LoginForm(
            username = viewModel.username,
            onUsernameChange = viewModel::onUsernameChange,
            password = viewModel.password,
            onPasswordChange = viewModel::onPasswordChange,
            isLoading = viewModel.isLoading,
            errorMessage = viewModel.errorMessage,
            infoMessage = viewModel.infoMessage,
            onClick = { viewModel.signIn(onNavigateToHome) },
            onGoogleClick = onGoogleClick,
            onForgotPassword = viewModel::resetPassword,
            onSignUpClick = onNavigateToSignUp,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginFormPreview() {
    FilRougeTheme {
        LoginForm(
            username = "etudiant@uqac.ca",
            onUsernameChange = {},
            password = "password123",
            onPasswordChange = {},
            isLoading = false,
            errorMessage = null,
            infoMessage = null,
            onClick = {},
            onForgotPassword = {},
            onGoogleClick = {},
            onSignUpClick = {}
        )
    }
}
