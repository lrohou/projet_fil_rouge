package com.example.filrouge.settings

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.filrouge.R
import com.example.filrouge.alarms.AlarmRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.TimeZone

/**
 * ViewModel gérant le chargement des informations du profil utilisateur
 * (pseudo, email, fuseau horaire système, photo de profil et décompte des alarmes actives).
 * Prend en charge l'affichage du compte propre ou de celui d'un tiers,
 * ainsi que l'import, la modification et la suppression de la photo de profil (stockée directement dans Firestore en Base64).
 */
class ProfileViewModel @JvmOverloads constructor(
    application: Application,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val alarmRepo: AlarmRepository = AlarmRepository()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var profileListener: ListenerRegistration? = null

    init {
        loadProfile(null)
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
    }

    /** Charge le profil cible (ou celui de l'utilisateur connecté si targetUserId est null) */
    fun loadProfile(targetUserId: String?) {
        profileListener?.remove()
        val currentUid = auth.currentUser?.uid

        if (targetUserId == null || targetUserId == currentUid) {
            // Profil de l'utilisateur connecté
            _uiState.value = loadInitialState()
            listenToUserProfile(currentUid)
            listenToCurrentAlarms()
        } else {
            // Profil d'un autre utilisateur
            _uiState.value = ProfileUiState(
                userId = targetUserId,
                displayName = "",
                email = "",
                timeZone = getApplication<Application>().getString(R.string.not_specified),
                activeAlarmsCount = 0,
                photoUrl = "",
                isCurrentUser = false,
                isLoading = true
            )
            listenToOtherUserProfile(targetUserId)
            fetchOtherUserAlarms(targetUserId)
        }
    }

    /** Prépare l'état initial à partir de la session Firebase Auth */
    private fun loadInitialState(): ProfileUiState {
        val user = auth.currentUser
        val initialName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")
            ?: ""
        val initialEmail = user?.email ?: ""
        val initialPhoto = user?.photoUrl?.toString() ?: ""
        return ProfileUiState(
            userId = user?.uid ?: "",
            displayName = initialName,
            email = initialEmail,
            timeZone = getSystemTimeZoneDisplay(),
            activeAlarmsCount = 0,
            photoUrl = initialPhoto,
            isCurrentUser = true,
            isLoading = false
        )
    }

    /** Écoute Firestore pour mettre à jour en temps réel les informations personnelles */
    private fun listenToUserProfile(uid: String?) {
        if (uid == null) return
        profileListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProfileVM", "Erreur écoute profil: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val firestoreName = snapshot.getString("displayName")
                    val firestoreEmail = snapshot.getString("email")
                    val firestorePhoto = snapshot.getString("photoUrl")

                    val current = _uiState.value
                    val finalName = firestoreName?.takeIf { it.isNotBlank() }
                        ?: current.displayName.takeIf { it.isNotBlank() }
                        ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                        ?: auth.currentUser?.email?.substringBefore("@")
                        ?: ""
                    val finalEmail = firestoreEmail?.takeIf { it.isNotBlank() }
                        ?: current.email.takeIf { it.isNotBlank() }
                        ?: auth.currentUser?.email
                        ?: ""
                    val finalPhoto = firestorePhoto?.takeIf { it.isNotBlank() }
                        ?: auth.currentUser?.photoUrl?.toString()
                        ?: ""

                    _uiState.value = current.copy(
                        displayName = finalName,
                        email = finalEmail,
                        photoUrl = finalPhoto,
                        isLoading = false
                    )
                }
            }
    }

    /** Écoute le profil d'un tiers dans Firestore */
    private fun listenToOtherUserProfile(targetUid: String) {
        val app = getApplication<Application>()
        profileListener = db.collection("users").document(targetUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("displayName")?.takeIf { it.isNotBlank() }
                        ?: snapshot.getString("email")?.substringBefore("@")
                        ?: app.getString(R.string.default_user_name)
                    val email = snapshot.getString("email") ?: ""
                    val tz = snapshot.getString("timeZone")?.takeIf { it.isNotBlank() } ?: app.getString(R.string.not_specified)
                    val photo = snapshot.getString("photoUrl") ?: ""

                    _uiState.value = _uiState.value.copy(
                        displayName = name,
                        email = email,
                        timeZone = tz,
                        photoUrl = photo,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
    }

    /** Calcule le nombre d'alarmes actuellement activées pour l'utilisateur */
    private fun listenToCurrentAlarms() {
        viewModelScope.launch {
            alarmRepo.alarmsFlow()
                .catch { e ->
                    Log.e("ProfileVM", "Erreur écoute alarmes: ${e.message}", e)
                }
                .collect { alarms ->
                    val activeCount = alarms.count { it.enabled }
                    _uiState.value = _uiState.value.copy(activeAlarmsCount = activeCount)
                }
        }
    }

    /** Récupère le nombre d'alarmes actives d'un autre utilisateur */
    private fun fetchOtherUserAlarms(targetUid: String) {
        db.collection("alarms")
            .whereArrayContains("participantIds", targetUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.documents.count { it.getBoolean("enabled") == true }
                _uiState.value = _uiState.value.copy(activeAlarmsCount = count)
            }
            .addOnFailureListener {
            }
    }

    /**
     * Importe ou modifie la photo de profil :
     * compresse l'image en JPEG léger (256x256, 75%) et l'encode en Base64,
     * puis met à jour directement le champ `photoUrl` dans Firestore (100% gratuit, aucun forfait requis).
     */
    fun uploadProfilePicture(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(isUploadingPhoto = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val base64String = withContext(Dispatchers.IO) {
                    val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Impossible de lire l'image sélectionnée")
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (originalBitmap == null) throw IllegalStateException("Format d'image invalide")

                    //redimmensionnement pour alléger l'image dans firebase
                    val maxDim = 256
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height, 1.0f)

                    val scaledBitmap = if (ratio < 1.0f) {
                        Bitmap.createScaledBitmap(
                            originalBitmap,
                            (width * ratio).toInt(),
                            (height * ratio).toInt(),
                            true
                        )
                    } else {
                        originalBitmap
                    }

                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                    val bytes = outputStream.toByteArray()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }

                // mise a jour dans firestore
                db.collection("users").document(uid).update("photoUrl", base64String).await()

                _uiState.value = _uiState.value.copy(
                    photoUrl = base64String,
                    isUploadingPhoto = false
                )
            } catch (e: Exception) {
                Log.e("ProfileVM", "Erreur enregistrement photo: ${e.message}", e)
                val app = getApplication<Application>()
                _uiState.value = _uiState.value.copy(
                    isUploadingPhoto = false,
                    errorMessage = e.localizedMessage ?: app.getString(R.string.profile_photo_upload_error)
                )
            }
        }
    }

    /**
     * Supprime la photo de profil :
     * retire le champ `photoUrl` dans le document Firestore.
     */
    fun deleteProfilePicture() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(isUploadingPhoto = true, errorMessage = null)

        viewModelScope.launch {
            try {
                // Suppression du champ photoUrl dans Firestore
                db.collection("users").document(uid).update("photoUrl", FieldValue.delete()).await()

                _uiState.value = _uiState.value.copy(
                    photoUrl = "",
                    isUploadingPhoto = false
                )
            } catch (e: Exception) {
                Log.e("ProfileVM", "Erreur suppression photo: ${e.message}", e)
                val app = getApplication<Application>()
                _uiState.value = _uiState.value.copy(
                    isUploadingPhoto = false,
                    errorMessage = e.localizedMessage ?: app.getString(R.string.profile_photo_delete_error)
                )
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    companion object {
        /** Formate l'affichage du fuseau horaire du système (ex: UTC+1 (Paris)) */
        fun getSystemTimeZoneDisplay(): String {
            val tz = TimeZone.getDefault()
            val now = System.currentTimeMillis()
            val offsetMillis = tz.getOffset(now)
            val totalMinutes = offsetMillis / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = Math.abs(totalMinutes % 60)
            val sign = if (hours >= 0) "+" else "-"
            val absHours = Math.abs(hours)
            val offsetStr = if (minutes == 0) {
                "UTC$sign$absHours"
            } else {
                "UTC$sign${absHours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
            }
            val city = tz.id.substringAfterLast("/").replace("_", " ")
            return "$offsetStr ($city)"
        }
    }
}
