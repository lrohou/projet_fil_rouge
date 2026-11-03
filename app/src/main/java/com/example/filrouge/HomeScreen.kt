package com.example.filrouge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.filrouge.alarms.Alarm
import com.example.filrouge.alarms.AlarmEditDialog
import com.example.filrouge.alarms.AlarmPageContent
import com.example.filrouge.ui.theme.FilRougeTheme
import com.example.filrouge.alarms.AlarmViewModel
import com.example.filrouge.alarms.AppNotification
import com.example.filrouge.alarms.DarkBlue
import com.example.filrouge.alarms.NavBarBlue
import com.example.filrouge.alarms.NotificationType
import com.example.filrouge.contacts.Contact
import com.example.filrouge.contacts.ContactsPageContent
import kotlinx.coroutines.launch
import com.example.filrouge.contacts.ContactsViewModel
import com.example.filrouge.settings.SettingsPageContent

/**
 * Écran principal de l'application (Conteneur principal).
 * Gère la navigation par balayage (HorizontalPager) entre les 3 onglets principaux :
 * 0: Contacts, 1: Alarmes, 2: Paramètres.
 * Intègre également la barre supérieure (profil, recherche, notifications) et la barre inférieure.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    onBack: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    alarmViewModel: AlarmViewModel = viewModel()
) {
    val context = LocalContext.current

    // État du pager : 3 pages (Contacts=0, Alarmes=1, Paramètres=2)
    val pagerState = rememberPagerState(
        initialPage = 1, // On démarre sur la page Alarmes
        pageCount = { 3 }
    )
    val scope = rememberCoroutineScope()
    val contactsViewModel: ContactsViewModel = viewModel()
    val contactsState by contactsViewModel.uiState.collectAsState()

    // État pour afficher/masquer les notifications
    var showNotifications by remember { mutableStateOf(false) }

    // État pour afficher la popup de nouvelle alarme
    var showNewAlarmDialog by remember { mutableStateOf(false) }

    // État pour l'alarme en cours d'édition (null = pas d'édition)
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    // État pour la barre de recherche dans les paramètres
    var searchQuery by remember { mutableStateOf("") }

    val alarms by alarmViewModel.alarms.collectAsState()

    val realContacts = contactsState.users
        .filter { it.uid in contactsState.friendIds }
        .map { Contact(uid = it.uid, name = it.displayName, subtitle = it.email) }

    // Dialog de création d'une nouvelle alarme
    if (showNewAlarmDialog) {
        AlarmEditDialog(
            isDarkTheme = isDarkTheme,
            existingAlarm = null,
            contactsList = realContacts, // 2. Ajout de la liste de contacts ici
            onDismiss = { showNewAlarmDialog = false },
            onConfirm = { hour, minute, name, days, soundName, vibrate, contacts, discordWebhookUrl ->
                alarmViewModel.createAlarm(
                    context,
                    hour,
                    minute,
                    name,
                    days,
                    soundName,
                    vibrate,
                    contacts,
                    discordWebhookUrl
                )
                showNewAlarmDialog = false
            }
        )
    }

    // Dialog d'édition d'une alarme existante
    editingAlarm?.let { alarm ->
        AlarmEditDialog(
            isDarkTheme = isDarkTheme,
            existingAlarm = alarm,
            contactsList = realContacts, // 3. Ajout de la liste de contacts ici
            onDismiss = { editingAlarm = null },
            onConfirm = { hour, minute, name, days, soundName, vibrate, contacts, discordWebhookUrl ->
                alarmViewModel.updateAlarm(
                    context,
                    alarm,
                    hour,
                    minute,
                    name,
                    days,
                    soundName,
                    vibrate,
                    contacts,
                    discordWebhookUrl
                )
                editingAlarm = null
            }
        )
    }

    // Liste des notifications (demandes d'ami + invitations d'alarme)
    val alarmInvitations by alarmViewModel.alarmInvitations.collectAsState()

    val friendNotifications = contactsState.incomingRequests.map { req ->
        AppNotification(
            id = req.friendshipId,
            title = req.from.displayName,
            message = "souhaite devenir ami avec vous",
            type = NotificationType.FRIEND_REQUEST
        )
    }

    val alarmNotifications = alarmInvitations.map { inv ->
        AppNotification(
            id = inv.id,
            title = inv.senderName,
            message = "vous invite à l'alarme '${inv.displayName}' (${inv.formattedTime})",
            type = NotificationType.ALARM_INVITATION,
            alarmInvitation = inv
        )
    }

    val notifications = friendNotifications + alarmNotifications

    BackHandler(enabled = showNotifications) {
        showNotifications = false
    }

    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color.White
    val navBarColor = if (isDarkTheme) Color(0xFF1E2A38) else NavBarBlue

    Scaffold(
        containerColor = backgroundColor,
        // Barre de navigation en bas
        bottomBar = {
            BottomNavBar(
                currentPage = pagerState.currentPage,
                navBarColor = navBarColor,
                onPageSelected = { page ->
                    scope.launch { pagerState.animateScrollToPage(page) }
                }
            )
        }
    ) { innerPadding ->
        // Conteneur principal avec la barre du haut
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // === Barre du haut (profil + notifications) ===
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icône profil (clic -> page profil)
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .size(48.dp)
                            .background(DarkBlue, CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_person),
                            contentDescription = "Profil",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Icône notifications (clic -> afficher/masquer)
                    IconButton(
                        onClick = { showNotifications = !showNotifications },
                        modifier = Modifier
                            .size(48.dp)
                            .background(DarkBlue, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_bell),
                            contentDescription = "Notifications",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // === Pager horizontal (swipe entre les 3 pages) ===
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> ContactsPageContent(
                            isDarkTheme = isDarkTheme,
                            contactsViewModel = contactsViewModel
                        )
                        1 -> AlarmPageContent(
                            alarms = alarms,
                            isDarkTheme = isDarkTheme,
                            onToggle = { alarm, checked ->
                                alarmViewModel.setEnabled(
                                    context,
                                    alarm,
                                    checked
                                )
                            },
                            onDelete = { alarm -> alarmViewModel.delete(context, alarm) },
                            onNewAlarmClick = { showNewAlarmDialog = true },
                            onEditAlarm = { alarm -> editingAlarm = alarm }
                        )
                        2 -> SettingsPageContent(
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = onToggleDarkTheme
                        )
                    }
                }
            }

            // === Popup de notifications (affichée par-dessus) ===
            if (showNotifications) {
                NotificationDropdown(
                    notifications = notifications,
                    onAccept = { notif ->
                        when (notif.type) {
                            NotificationType.FRIEND_REQUEST -> contactsViewModel.acceptRequest(notif.id)
                            NotificationType.ALARM_INVITATION -> notif.alarmInvitation?.let { inv ->
                                alarmViewModel.acceptInvitation(inv)
                            }
                        }
                    },
                    onRefuse = { notif ->
                        when (notif.type) {
                            NotificationType.FRIEND_REQUEST -> contactsViewModel.refuseRequest(notif.id)
                            NotificationType.ALARM_INVITATION -> alarmViewModel.declineInvitation(notif.id)
                        }
                    },
                    onDismiss = { showNotifications = false }
                )
            }
        }
    }
}

/**
 * Panneau déroulant de notifications affichant la liste fusionnée des demandes d'amitié
 * et des invitations aux alarmes partagées avec options d'acceptation ou de refus.
 */
@Composable
fun BoxScope.NotificationDropdown(
    notifications: List<AppNotification>,
    onAccept: (AppNotification) -> Unit,
    onRefuse: (AppNotification) -> Unit,
    onDismiss: () -> Unit
) {
    // Fond semi-transparent pour fermer en cliquant à côté
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    )

    // Carte de notifications positionnée en haut à droite
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBlue),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 72.dp, end = 16.dp)
            .width(280.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            if (notifications.isEmpty()) {
                Text(
                    text = "Aucune notification",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                notifications.forEachIndexed { index, notif ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Titre en gras
                            Text(
                                text = notif.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            // Message
                            Text(
                                text = notif.message,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Boutons Accepter et Refuser
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bouton Accepter
                            IconButton(
                                onClick = { onAccept(notif) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "Accepter",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Bouton Refuser
                            IconButton(
                                onClick = { onRefuse(notif) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Refuser",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (index < notifications.size - 1) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.4f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Barre de navigation inférieure permettant de basculer entre les 3 onglets.
 */
@Composable
fun BottomNavBar(
    currentPage: Int = 1,
    navBarColor: Color = NavBarBlue,
    onPageSelected: (Int) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = navBarColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icône Contacts
                IconButton(onClick = { onPageSelected(0) }) {
                    Image(
                        painter = painterResource(id = R.drawable.group),
                        contentDescription = "Contacts",
                        modifier = Modifier.size(if (currentPage == 0) 40.dp else 28.dp)
                    )
                }
                // Icône Alarmes
                IconButton(onClick = { onPageSelected(1) }) {
                    Image(
                        painter = painterResource(id = R.drawable.clock),
                        contentDescription = "Alarmes",
                        modifier = Modifier.size(if (currentPage == 1) 40.dp else 28.dp)
                    )
                }
                // Icône Paramètres
                IconButton(onClick = { onPageSelected(2) }) {
                    Image(
                        painter = painterResource(id = R.drawable.settings),
                        contentDescription = "Paramètres",
                        modifier = Modifier.size(if (currentPage == 2) 40.dp else 28.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PagePreview() {
    FilRougeTheme {
        HomeScreen()
    }
}