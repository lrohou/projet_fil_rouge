package com.example.filrouge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Contenu de la page Paramètres
@Composable
fun SettingsPageContent(
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {}
) {
    // État pour la langue sélectionnée (FR ou EN)
    var selectedLang by remember { mutableStateOf("FR") }

    // Couleurs adaptées au thème clair ou sombre
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val dividerColor = if (isDarkTheme) Color.Gray else DarkBlue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Section Langue
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Langue",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            // Bouton FR
            Button(
                onClick = { selectedLang = "FR" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedLang == "FR") DarkBlue else Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "FR",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Bouton EN
            Button(
                onClick = { selectedLang = "EN" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedLang == "EN") DarkBlue else Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "EN",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section Choix du Thème (Clair / Sombre)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Thème",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            // Bouton Thème Clair
            Button(
                onClick = { onToggleDarkTheme(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isDarkTheme) DarkBlue else Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Clair",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Bouton Thème Sombre
            Button(
                onClick = { onToggleDarkTheme(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) DarkBlue else Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Sombre",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = dividerColor, thickness = 2.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Options de paramètres
        Text(
            text = "Changer mon mot de passe",
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = "Changer mon fuseau horaire",
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = dividerColor, thickness = 2.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Section Aide
        Text(
            text = "Aide",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Supprimer mon compte",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = dividerColor, thickness = 2.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Liens en bas
        Text(
            text = "À propos",
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = "Mentions légales",
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
