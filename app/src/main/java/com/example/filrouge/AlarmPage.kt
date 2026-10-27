package com.example.filrouge

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filrouge.data.Alarm
import androidx.compose.ui.tooling.preview.Preview
import com.example.filrouge.ui.theme.FilRougeTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close

//couleurs de l'application
val LightBlue = Color(0xFFD0EBFF)
val CardBlue = Color(0xFFD0EBFF)
val DarkBlue = Color(0xFF0055A4)
val NavBarBlue = Color(0xFFB8DEFF)

//modèle de données pour une notification
data class AppNotification(
    val id: String,
    val title: String,
    val message: String
)

//liste des sons disponibles (noms des fichiers dans res/raw sans extension)
val availableSounds = listOf(
    "aura_tone",
    "birds",
    "daybreak",
    "early_riser",
    "earth_day",
    "happy_day",
    "melodic_bones",
    "ping",
    "slow_morning"
)

//noms des sons
fun soundDisplayName(name: String): String = name.replace("_", " ")
    .replaceFirstChar { it.uppercase() }

//contenu de la page Alarmes (accueil)
@Composable
fun AlarmPageContent(
    alarms: List<Alarm>,
    isDarkTheme: Boolean = false,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
    onNewAlarmClick: () -> Unit,
    onEditAlarm: (Alarm) -> Unit = {}
) {
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else LightBlue
    val textColor = if (isDarkTheme) Color.White else Color.Black

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        //alerte si l'autorisation d'affichage par-dessus les autres applis n'est pas accordée
        if (!hasOverlayPermission) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚠️ Affichage plein écran",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF856404)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pour que le réveil prenne tout l'écran même quand vous êtes sur une autre application, autorisez l'affichage par-dessus.",
                        fontSize = 12.sp,
                        color = Color(0xFF856404)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Autoriser l'affichage", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        //bouton nouvelle alarme
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
                //icône + dans un cercle blanc
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
                    onDelete = { onDelete(alarm) },
                    onClick = { onEditAlarm(alarm) }
                )
            }
            Spacer(modifier = Modifier.height(17.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

//carte individuelle d'une alarme
@Composable
fun AlarmCard(
    alarm: Alarm,
    isDarkTheme: Boolean = false,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit = {}
) {
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else CardBlue
    val timeBg = if (isDarkTheme) Color(0xFF2C3E50) else Color.White
    val timeTextColor = if (isDarkTheme) Color.White else Color.Black
    val subtitleColor = if (isDarkTheme) Color(0xFFCCCCCC) else Color.Black

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                //nom de l'alarme
                if (alarm.name.isNotBlank()) {
                    Text(
                        text = alarm.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtitleColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                //heure dans un rectangle arrondi
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

//dialog création / édition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditDialog(
    isDarkTheme: Boolean = false,
    existingAlarm: Alarm? = null, // null = création, non-null = édition
    contactsList: List<Contact>,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, name: String, days: List<Int>, soundName: String, vibrate: Boolean, contacts: List<String>) -> Unit
) {
    val isEdit = existingAlarm != null
    val dialogTitle = if (isEdit) "Modifier l'alarme" else "Nouvelle Alarme"

    val timeState = rememberTimePickerState(
        initialHour = existingAlarm?.hour ?: 7,
        initialMinute = existingAlarm?.minute ?: 0,
        is24Hour = true
    )
    var name by remember { mutableStateOf(existingAlarm?.name ?: "") }
    var selectedDays by remember { mutableStateOf(existingAlarm?.days ?: emptyList<Int>()) }
    var soundName by remember { mutableStateOf(existingAlarm?.soundName ?: "aura_tone") }
    var vibrate by remember { mutableStateOf(existingAlarm?.vibrate ?: false) }
    var showSoundPicker by remember { mutableStateOf(false) }

    var selectedContacts by remember { mutableStateOf(existingAlarm?.participantIds ?: emptyList<String>()) }
    var showContacts by remember { mutableStateOf(false) }

    val dialogBg = if (isDarkTheme) Color(0xFF1E2A38) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    if (showSoundPicker) {
        SoundPickerDialog(
            isDarkTheme = isDarkTheme,
            currentSound = soundName,
            onDismiss = { showSoundPicker = false },
            onSelect = { selected ->
                soundName = selected
                showSoundPicker = false
            }
        )
        return
    }

    if (showContacts) {
        ContactsPickerDialog(
            isDarkTheme = isDarkTheme,
            contactsList = contactsList, // On transmet la vraie liste
            selectedList = selectedContacts,
            onDismiss = { showContacts = false },
            onContactToggled = { contactUid -> // On travaille désormais avec l'UID
                selectedContacts = if (selectedContacts.contains(contactUid)) {
                    selectedContacts - contactUid
                } else {
                    selectedContacts + contactUid
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(dialogTitle, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = textColor)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //nom de l'alarme
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'alarme") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // timepicker
                TimePicker(state = timeState)

                Spacer(modifier = Modifier.height(16.dp))

                // jours de la semaine
                Text("Jours de répétition", fontWeight = FontWeight.Medium, color = textColor, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                DaySelector(
                    selectedDays = selectedDays,
                    isDarkTheme = isDarkTheme,
                    onDaysChanged = { selectedDays = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // sélection du son
                Text("Son", fontWeight = FontWeight.Medium, color = textColor, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF2C3E50) else Color(0xFFE8F1F8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSoundPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            soundDisplayName(soundName),
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text("▶", color = DarkBlue, fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // vibreur
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibreur", fontWeight = FontWeight.Medium, color = textColor, fontSize = 14.sp)
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showContacts = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ajouter des contacts", color = Color.White)
                    }

                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timeState.hour, timeState.minute, name, selectedDays, soundName, vibrate, selectedContacts) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (isEdit) "Enregistrer" else "Créer", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = if (isDarkTheme) Color(0xFF90CAFF) else DarkBlue)
            }
        }
    )
}

//======== sélecteur de jours========

@Composable
fun DaySelector(
    selectedDays: List<Int>,
    isDarkTheme: Boolean,
    onDaysChanged: (List<Int>) -> Unit
) {
    val dayNames = listOf("L", "M", "M", "J", "V", "S", "D")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dayNames.forEachIndexed { index, letter ->
            val dayNumber = index + 1 // 1=lundi .. 7=dimanche
            val isSelected = dayNumber in selectedDays

            Surface(
                shape = CircleShape,
                color = if (isSelected) DarkBlue else if (isDarkTheme) Color(0xFF2C3E50) else Color(0xFFE8F1F8),
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        onDaysChanged(
                            if (isSelected) selectedDays - dayNumber
                            else selectedDays + dayNumber
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = letter,
                        color = if (isSelected) Color.White else if (isDarkTheme) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

//======== sélecteur de sons========

@Composable
fun SoundPickerDialog(
    isDarkTheme: Boolean,
    currentSound: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val context = LocalContext.current
    val dialogBg = if (isDarkTheme) Color(0xFF1E2A38) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    var playingSound by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
    }

    fun playPreview(soundName: String) {
        //arrêter le son en cours
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }

        if (playingSound == soundName) {
            playingSound = null
            mediaPlayer = null
            return
        }

        val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
        if (resId != 0) {
            mediaPlayer = MediaPlayer().apply {
                val afd = context.resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { playingSound = null }
                prepare()
                start()
            }
            playingSound = soundName
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Choisir un son", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(availableSounds) { sound ->
                    val isSelected = sound == currentSound
                    val isPlaying = sound == playingSound

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isSelected -> DarkBlue.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(sound) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSelected) {
                                    Text("✓ ", color = DarkBlue, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = soundDisplayName(sound),
                                    color = if (isSelected) DarkBlue else textColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            IconButton(
                                onClick = { playPreview(sound) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Arrêter" else "Écouter",
                                    tint = DarkBlue
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = if (isDarkTheme) Color(0xFF90CAFF) else DarkBlue)
            }
        }
    )
}
@Composable
fun ContactsPickerDialog(
    isDarkTheme: Boolean,
    contactsList: List<Contact>, // Reçoit la liste des vrais contacts
    selectedList: List<String>,  // Reçoit les UIDs sélectionnés
    onDismiss: () -> Unit,
    onContactToggled: (String) -> Unit // Renvoie un UID
) {
    val dialogBg = if (isDarkTheme) Color(0xFF1E2A38) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Inviter des contacts", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
        },
        text = {
            if (contactsList.isEmpty()) {
                Text(
                    text = "Aucun contact disponible",
                    color = textColor,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(contactsList) { contact ->
                        // Vérifie si l'UID de ce contact est dans la liste des sélectionnés
                        val isSelected = selectedList.contains(contact.uid)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactToggled(contact.uid) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = contact.name, // Affichage du nom
                                color = textColor,
                                fontSize = 16.sp
                            )
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onContactToggled(contact.uid) },
                                colors = CheckboxDefaults.colors(checkedColor = DarkBlue)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Terminer", color = if (isDarkTheme) Color(0xFF90CAFF) else DarkBlue)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AlarmPageContentPreview() {
    FilRougeTheme {
        AlarmPageContent(
            alarms = listOf(
                Alarm(id = "1", hour = 7, minute = 30, enabled = true, name = "Réveil matin", days = listOf(1, 2, 3, 4, 5)),
                Alarm(id = "2", hour = 9, minute = 0, enabled = false, name = "Weekend", days = listOf(6, 7))
            ),
            isDarkTheme = false,
            onToggle = { _, _ -> },
            onDelete = {},
            onNewAlarmClick = {},
            onEditAlarm = {}
        )
    }
}
