package com.example.filrouge

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filrouge.viewModels.AppUser
import com.example.filrouge.viewModels.ContactsViewModel

//modèle de données pour un contact
data class Contact(val uid: String, val name: String, val subtitle: String)

enum class FriendStatus { NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIEND }

//contenu de la page contacts
@Composable
fun ContactsPageContent(
    isDarkTheme: Boolean = false,
    onNavigateToProfile: (String) -> Unit = {},
    contactsViewModel: ContactsViewModel = viewModel()
) {
    var showNewContactDialog by remember { mutableStateOf(false) }

    // Liste fictive de contacts existants
    val uiState by contactsViewModel.uiState.collectAsState()

    val contacts = uiState.users
        .filter { it.uid in uiState.friendIds }
        .map { Contact(uid = it.uid, name = it.displayName, subtitle = it.email) }

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else CardBlue

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        //titre de la page
        Text(
            text = "Contacts",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        if (contacts.isEmpty()) {
            Text(
                text = "Aucun contact pour l'instant",
                color = textColor,
                fontSize = 14.sp
            )
        }

        //liste des contacts
        contacts.forEach { contact ->
            ContactCard(contact = contact, isDarkTheme = isDarkTheme)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        //bouton "nouveau contact"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.clickable {
                    contactsViewModel.fetchUsers()
                    showNewContactDialog = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Nouveau\ncontact",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    //icône + dans un cercle bleu foncé
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(DarkBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Pop up de recherche des utilisateurs pour nouveau contact
    if (showNewContactDialog) {
        NewContactDialog(
            isDarkTheme = isDarkTheme,
            viewModel = contactsViewModel,
            onDismiss = { showNewContactDialog = false },
            onNavigateToProfile = { userId ->
                showNewContactDialog = false
                onNavigateToProfile(userId)
            }
        )
    }
}

// Pop up pour parcourir et rechercher les utilisateurs de l'application
@Composable
fun NewContactDialog(
    isDarkTheme: Boolean,
    viewModel: ContactsViewModel,
    onDismiss: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val dialogBg = if (isDarkTheme) Color(0xFF1E2A38) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subTextColor = if (isDarkTheme) Color.LightGray else Color.Gray
    val inputBg = if (isDarkTheme) Color(0xFF243447) else Color(0xFFF1F5F9)
    val itemBg = if (isDarkTheme) Color(0xFF243447) else Color(0xFFF8FAFC)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // En-tête avec titre et bouton fermer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nouveau contact",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barre de recherche pour parcourir les utilisateurs
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            text = "Rechercher un utilisateur...",
                            fontSize = 14.sp,
                            color = subTextColor
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "🔍",
                            fontSize = 14.sp
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Text(
                                    text = "✕",
                                    fontSize = 14.sp,
                                    color = subTextColor
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Contenu : chargement, liste vide ou liste des utilisateurs
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = DarkBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    uiState.error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Impossible de charger les utilisateurs",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.error ?: "",
                                color = subTextColor,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.fetchUsers() },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Réessayer", color = Color.White)
                            }
                        }
                    }

                    uiState.filteredUsers.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isBlank()) {
                                    "Aucun autre utilisateur inscrit"
                                } else {
                                    "Aucun utilisateur trouvé pour \"${uiState.searchQuery}\""
                                },
                                color = subTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.filteredUsers, key = { it.uid }) { user ->
                                val incoming = uiState.incomingRequests.firstOrNull { it.from.uid == user.uid }
                                val status = when {
                                    user.uid in uiState.friendIds -> FriendStatus.FRIEND
                                    user.uid in uiState.friendRequestsSent -> FriendStatus.REQUEST_SENT
                                    incoming != null -> FriendStatus.REQUEST_RECEIVED
                                    else -> FriendStatus.NONE
                                }
                                UserSearchItem(
                                    user = user,
                                    isDarkTheme = isDarkTheme,
                                    itemBg = itemBg,
                                    textColor = textColor,
                                    subTextColor = subTextColor,
                                    status = status,
                                    onUserClick = { onNavigateToProfile(user.uid) },
                                    onFriendRequestClick = {
                                        viewModel.sendFriendRequest(user.uid) { success ->
                                            Toast.makeText(
                                                context,
                                                if (success) "Demande d'ami envoyée à ${user.displayName}"
                                                else "Impossible d'envoyer la demande",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onAcceptClick = { incoming?.let { viewModel.acceptRequest(it.friendshipId) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Élément d'utilisateur dans la liste de recherche
@Composable
fun UserSearchItem(
    user: AppUser,
    isDarkTheme: Boolean,
    itemBg: Color,
    textColor: Color,
    subTextColor: Color,
    status: FriendStatus,
    onUserClick: () -> Unit,
    onFriendRequestClick: () -> Unit,
    onAcceptClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = itemBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar avec initiale cliquable pour aller sur le profil
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(DarkBlue, CircleShape)
                    .clickable(onClick = onUserClick),
                contentAlignment = Alignment.Center
            ) {
                val initial = user.displayName.trim().take(1).uppercase()
                if (initial.isNotEmpty()) {
                    Text(
                        text = initial,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = "Avatar",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Nom et email de l'utilisateur (cliquable -> profil)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onUserClick)
            ) {
                Text(
                    text = user.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.email.isNotBlank()) {
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bouton "Demande d'ami"
            when (status) {
                FriendStatus.NONE -> Button(
                    onClick = onFriendRequestClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Demande d'ami", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                FriendStatus.REQUEST_RECEIVED -> Button(
                    onClick = onAcceptClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Accepter", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }

                FriendStatus.REQUEST_SENT, FriendStatus.FRIEND -> OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)
                    )
                ) {
                    Text(
                        text = if (status == FriendStatus.FRIEND) "Ami ✓" else "Envoyée ✓",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Carte individuelle d'un contact existant
@Composable
fun ContactCard(
    contact: Contact,
    isDarkTheme: Boolean = false
) {
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else CardBlue
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subtitleColor = if (isDarkTheme) Color(0xFFCCCCCC) else Color.Black
    val avatarBg = if (isDarkTheme) Color(0xFF2C3E50) else Color(0xFFE0E0E0)
    val avatarIcon = if (isDarkTheme) R.drawable.ic_person else R.drawable.ic_person_black

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //icône de profil dans un cercle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = avatarIcon),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            //nom et nombre de réveils actifs
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "${contact.subtitle}",
                    fontSize = 13.sp,
                    color = subtitleColor
                )
            }

            //icône horloge +
            Image(
                painter = painterResource(id = R.drawable.plus),
                contentDescription = "Ajouter alarme",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
