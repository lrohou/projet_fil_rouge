package com.example.filrouge

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filrouge.data.SnoozeManager
import com.example.filrouge.ui.theme.FilRougeTheme
import com.example.filrouge.viewModels.ProfileUiState
import com.example.filrouge.viewModels.ProfileViewModel

// Page profil de l'utilisateur
@Composable
fun ProfileScreen(
    userId: String? = null,
    isDarkTheme: Boolean = false,
    viewModel: ProfileViewModel = viewModel(),
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val uiState by viewModel.uiState.collectAsState()

    // Sauvegarder le nom dès qu'il est chargé depuis Firestore (pour AlarmReceiver / AlarmRingActivity)
    val context = LocalContext.current
    LaunchedEffect(uiState.displayName) {
        if (uiState.displayName.isNotBlank()) {
            SnoozeManager.saveUserName(context, uiState.displayName)
        }
    }

    ProfileContent(
        uiState = uiState,
        isDarkTheme = isDarkTheme,
        onBack = onBack,
        onLogout = onLogout
    )
}

@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    isDarkTheme: Boolean = false,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val bgColor = if (isDarkTheme) Color(0xFF121212) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else CardBlue
    val subTextColor = if (isDarkTheme) Color.LightGray else Color.Gray

    Scaffold(containerColor = bgColor) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Bouton retour
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        text = "← Retour",
                        color = if (isDarkTheme) Color(0xFF90CAFF) else DarkBlue,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            //icône de profil
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(DarkBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initial = uiState.displayName.trim().take(1).uppercase()
                if (initial.isNotEmpty()) {
                    Text(
                        text = initial,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = "Photo de profil",
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            //nom du user
            Text(
                text = uiState.displayName.ifBlank { "Utilisateur" },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            //email du user
            if (uiState.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.email,
                    fontSize = 14.sp,
                    color = subTextColor
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Carte d'informations
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Informations",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Ligne nom
                    ProfileInfoRow(
                        label = "Nom",
                        value = uiState.displayName.ifBlank { "Non renseigné" },
                        textColor = textColor
                    )
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Ligne email
                    ProfileInfoRow(
                        label = "Email",
                        value = uiState.email.ifBlank { "Non renseigné" },
                        textColor = textColor
                    )
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Ligne fuseau horaire
                    ProfileInfoRow(
                        label = "Fuseau horaire",
                        value = uiState.timeZone,
                        textColor = textColor
                    )
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Ligne nombre d'alarmes actives
                    ProfileInfoRow(
                        label = "Alarmes actives",
                        value = "${uiState.activeAlarmsCount}",
                        textColor = textColor
                    )
                }
            }

            // Bouton de déconnexion (uniquement sur son propre profil)
            if (uiState.isCurrentUser) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Se déconnecter",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Ligne d'information du profil (label + valeur)
@Composable
fun ProfileInfoRow(label: String, value: String, textColor: Color = Color.Black) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    FilRougeTheme {
        ProfileContent(
            uiState = ProfileUiState(
                displayName = "Nans",
                email = "loanrohou@gmail.com",
                timeZone = "UTC+1 (Paris)",
                activeAlarmsCount = 2
            ),
            isDarkTheme = false,
            onBack = {},
            onLogout = {}
        )
    }
}