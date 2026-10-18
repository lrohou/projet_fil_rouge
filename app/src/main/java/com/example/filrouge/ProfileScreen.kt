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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filrouge.ui.theme.FilRougeTheme

// Page profil de l'utilisateur
@Composable
fun ProfileScreen(
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

            // Grande icône de profil
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(DarkBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_person),
                    contentDescription = "Photo de profil",
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nom de l'utilisateur
            Text(
                text = "Utilisateur",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Text(
                text = "utilisateur@email.com",
                fontSize = 14.sp,
                color = subTextColor
            )

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
                    ProfileInfoRow("Nom", "Utilisateur", textColor)
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    // Ligne email
                    ProfileInfoRow("Email", "utilisateur@email.com", textColor)
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    // Ligne fuseau horaire
                    ProfileInfoRow("Fuseau horaire", "UTC+1 (Paris)", textColor)
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    // Ligne nombre d'alarmes
                    ProfileInfoRow("Alarmes actives", "3", textColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton de déconnexion
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
        ProfileScreen(
            isDarkTheme = false,
            onBack = {},
            onLogout = {}
        )
    }
}