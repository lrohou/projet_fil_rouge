package com.example.filrouge.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.filrouge.ui.UserAvatar
import coil.request.ImageRequest
import com.example.filrouge.R
import com.example.filrouge.alarms.CardBlue
import com.example.filrouge.alarms.DarkBlue
import com.example.filrouge.alarms.SnoozeManager
import com.example.filrouge.ui.theme.FilRougeTheme

/**
 * Écran de consultation de profil (compte personnel ou ami).
 * Permet d'afficher les informations, le fuseau horaire, d'importer/modifier/supprimer
 * sa photo de profil et de se déconnecter.
 */
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
    val context = LocalContext.current

    //sauvegarder le nom dès qu'il est chargé depuis firestore
    LaunchedEffect(uiState.displayName) {
        if (uiState.displayName.isNotBlank()) {
            SnoozeManager.saveUserName(context, uiState.displayName)
        }
    }

    //affichage des erreurs éventuelles 
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    ProfileContent(
        uiState = uiState,
        isDarkTheme = isDarkTheme,
        onBack = onBack,
        onLogout = onLogout,
        onUploadPhoto = viewModel::uploadProfilePicture,
        onDeletePhoto = viewModel::deleteProfilePicture
    )
}

/** Composant UI affichant le contenu détaillé du profil */
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    isDarkTheme: Boolean = false,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onUploadPhoto: (Uri) -> Unit = {},
    onDeletePhoto: () -> Unit = {}
) {
    val bgColor = if (isDarkTheme) Color(0xFF121212) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val cardColor = if (isDarkTheme) Color(0xFF1E2A38) else CardBlue
    val subTextColor = if (isDarkTheme) Color.LightGray else Color.Gray

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    //sélectionneur d'image Android
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadPhoto(uri)
        }
    }

    //Dialog de confirmation pour la suppression de la photo
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.profile_delete_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.profile_delete_confirm_msg))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeletePhoto()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.profile_delete_photo),
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = stringResource(R.string.btn_cancel))
                }
            }
        )
    }

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
                        text = stringResource(R.string.btn_back),
                        color = if (isDarkTheme) Color(0xFF90CAFF) else DarkBlue,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar avec photo / initiale du pseudo
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(DarkBlue)
                    .then(
                        if (uiState.isCurrentUser && !uiState.isUploadingPhoto) {
                            Modifier.clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(
                    photoUrl = uiState.photoUrl,
                    displayName = uiState.displayName,
                    initialFontSize = 46.sp,
                    iconSize = 56.dp
                )

                // Overlay de chargement pendant l'envoi ou la suppression
                if (uiState.isUploadingPhoto) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            // Boutons d'action pour la photo (uniquement pour l'utilisateur connecté)
            if (uiState.isCurrentUser) {
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.photoUrl.isBlank()) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !uiState.isUploadingPhoto,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.profile_add_photo),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !uiState.isUploadingPhoto,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_change_photo),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            enabled = !uiState.isUploadingPhoto,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFD32F2F)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_delete_photo),
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nom et adresse e-mail
            Text(
                text = uiState.displayName.ifBlank { stringResource(R.string.default_user_name) },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            // Email du user
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
                        text = stringResource(R.string.profile_info_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Ligne nom
                    ProfileInfoRow(
                        label = stringResource(R.string.profile_label_name),
                        value = uiState.displayName.ifBlank { stringResource(R.string.not_specified) },
                        textColor = textColor
                    )
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Ligne email
                    ProfileInfoRow(
                        label = stringResource(R.string.profile_label_email),
                        value = uiState.email.ifBlank { stringResource(R.string.not_specified) },
                        textColor = textColor
                    )
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Ligne fuseau horaire
                    ProfileInfoRow(
                        label = stringResource(R.string.profile_label_timezone),
                        value = uiState.timeZone.ifBlank { stringResource(R.string.not_specified) },
                        textColor = textColor
                    )
                    HorizontalDivider(
                        color = if (isDarkTheme) Color.Gray.copy(alpha = 0.5f) else Color.White,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Ligne nombre d'alarmes actives
                    ProfileInfoRow(
                        label = stringResource(R.string.profile_label_active_alarms),
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
                        text = stringResource(R.string.btn_logout),
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

/** Ligne d'affichage clé / valeur dans la carte d'informations */
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
