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
import com.example.filrouge.data.Alarm
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

// Couleurs utilisées dans toute l'application
val LightBlue = Color(0xFFD0EBFF)
val CardBlue = Color(0xFFD0EBFF)
val DarkBlue = Color(0xFF0055A4)
val NavBarBlue = Color(0xFFB8DEFF)

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
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
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

        if (alarms.isEmpty()) {
            Text("Aucune alarme pour l'instant", color = textColor, fontSize = 14.sp)
        }

        alarms.forEach { alarm ->
            key(alarm.id) {
                AlarmCard(
                    alarm = alarm,
                    isDarkTheme = isDarkTheme,
                    onToggle = { checked -> onToggle(alarm, checked) },
                    onDelete = { onDelete(alarm) }
                )
            }
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
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = subtitleColor)
                }
                Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAlarmDialog(
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timeState = rememberTimePickerState(initialHour = 7, initialMinute = 0, is24Hour = true)
    val dialogBg = if (isDarkTheme) Color(0xFF1E2A38) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Nouvelle Alarme", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = textColor) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timeState.hour, timeState.minute) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Créer", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = if (isDarkTheme) Color(0xFF90CAFF) else DarkBlue)
            }
        }
    )
}
