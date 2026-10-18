package com.example.filrouge

import LoginViewModel
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import android.content.Intent
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.viewmodel.compose.viewModel

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

// DataStore pour sauvegarder les paramètres de l'application
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parametres")
val DarkThemeKey = booleanPreferencesKey("is_dark_theme")

class MainActivity : ComponentActivity() {
    @SuppressLint("FlowOperatorInvokedInComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            //lecture de la préférence du thème dans le DataStore
            val isDarkTheme by context.dataStore.data.map { preferences ->
                preferences[DarkThemeKey] ?: false
            }.collectAsState(initial = false)

            FilRougeTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                AppNavHost(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { newValue ->
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
        Text("Mon réveil social", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(48.dp))
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo de l'app",
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.size(48.dp))

        TextField(
            value = username,
            onValueChange = onUsernameChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            placeholder = { Text("Enter Email Address") }
        )

        Spacer(Modifier.size(8.dp))

        TextField(
            value = password,
            onValueChange = onPasswordChange,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            placeholder = { Text("Enter Password") }
        )

        Text(
            "Forgot your password?",
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
            else Text("Sign In")
        }

        Spacer(Modifier.size(72.dp))

        Text(
            buildAnnotatedString {
                append("No account yet? ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "signup",
                        styles = TextLinkStyles(style = SpanStyle(color = Color.Blue)),
                        linkInteractionListener = { onSignUpClick() }
                    )
                ) {
                    append("Click here ")
                }
                append("to sign up.")
            },
            fontSize = 14.sp
        )
    }
}

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
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
            onSignUpClick = {}
        )
    }
}