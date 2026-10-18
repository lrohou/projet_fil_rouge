package com.example.filrouge

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Couleurs utilisées dans toute l'application
val LightBlue = Color(0xFFD0EBFF)
val CardBlue = Color(0xFFD0EBFF)
val DarkBlue = Color(0xFF0055A4)
val NavBarBlue = Color(0xFFB8DEFF)

// Modèle de données pour une alarme
data class Alarm(
    val id: Int,
    val time: String,
    val subtitle: String,
    val isEnabled: Boolean
)

// Modèle de données pour une notification
data class AppNotification(
    val id: Int,
    val title: String,
    val message: String
)

// Contenu de la page Alarmes (accueil)
@Composable
fun AlarmPageContent(
    alarms: List<Alarm>,
    isDarkTheme: Boolean = false,
    onToggle: (Int, Boolean) -> Unit,
    onNewAlarmClick: () -> Unit
) {
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else LightBlue
    val textColor = if (isDarkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Bouton "Nouvelle Alarme"
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nouvelle\nAlarme",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    lineHeight = 32.sp
                )
                // Icône + dans un cercle blanc
                IconButton(
                    onClick = onNewAlarmClick,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.plus),
                        contentDescription = "Ajouter une alarme",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.Gray, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Alarmes en cours",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = textColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Liste des alarmes
        alarms.forEachIndexed { index, alarm ->
            AlarmCard(
                alarm = alarm,
                isDarkTheme = isDarkTheme,
                onToggle = { isChecked -> onToggle(index, isChecked) }
            )
            Spacer(modifier = Modifier.height(17.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Carte individuelle d'une alarme
@Composable
fun AlarmCard(
    alarm: Alarm,
    isDarkTheme: Boolean = false,
    onToggle: (Boolean) -> Unit
) {
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else CardBlue
    val timeBg = if (isDarkTheme) Color(0xFF2C3E50) else Color.White
    val timeTextColor = if (isDarkTheme) Color.White else Color.Black
    val subtitleColor = if (isDarkTheme) Color(0xFFCCCCCC) else Color.Black

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Heure dans un rectangle arrondi
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = timeBg
                ) {
                    Text(
                        text = alarm.time,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = timeTextColor,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alarm.subtitle,
                    fontSize = 13.sp,
                    color = subtitleColor
                )
            }
            // Interrupteur on/off
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

// Popup de création d'alarme fictive
@Composable
fun NewAlarmDialog(
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit
) {
    // États locaux pour les champs du formulaire
    var hour by remember { mutableStateOf("07") }
    var minute by remember { mutableStateOf("00") }
    val dialogBg = if (isDarkTheme) Color(0xFF1E2A38) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Nouvelle Alarme",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = textColor
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sélection de l'heure
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Champ heure
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { if (it.length <= 2) hour = it },
                        label = { Text("HH") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(
                        text = " : ",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    // Champ minute
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { if (it.length <= 2) minute = it },
                        label = { Text("MM") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Label
                OutlinedTextField(
                    value = "demain",
                    onValueChange = {},
                    label = { Text("Jour") },
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        // Bouton de confirmation (ne fait rien de réel)
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Créer", color = Color.White)
            }
        },
        // Bouton d'annulation
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = if (isDarkTheme) Color(0xFF90CAFF) else DarkBlue)
            }
        }
    )
}
