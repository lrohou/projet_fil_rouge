package com.example.filrouge.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.example.filrouge.R
import com.example.filrouge.alarms.DarkBlue

/**
 * Contenu principal de l'onglet "Paramètres".
 * Permet de basculer entre le thème clair et sombre, de choisir la langue (FR / EN)
 * et de consulter les options de l'application.
 */
@Composable
fun SettingsPageContent(
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {}
) {
    // Couleurs adaptées au thème clair ou sombre
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val dividerColor = if (isDarkTheme) Color.Gray else DarkBlue

    // Lecture de la locale active pour mettre en évidence le bon bouton
    val currentLang by remember {
        derivedStateOf {
            AppCompatDelegate.getApplicationLocales()
                .toLanguageTags()
                .take(2)
                .lowercase()
                .ifEmpty { "fr" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Section Choix du Thème (Clair / Sombre)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme),
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
                    text = stringResource(R.string.settings_theme_light),
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
                    text = stringResource(R.string.settings_theme_dark),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = dividerColor, thickness = 2.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Section Choix de la Langue (FR / EN)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            // Bouton Français
            Button(
                onClick = {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags("fr")
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentLang == "fr") DarkBlue else Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text("FR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // Bouton Anglais
            Button(
                onClick = {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags("en")
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentLang == "en") DarkBlue else Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text("EN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = dividerColor, thickness = 2.dp)

        Spacer(modifier = Modifier.height(24.dp))
    }
}
