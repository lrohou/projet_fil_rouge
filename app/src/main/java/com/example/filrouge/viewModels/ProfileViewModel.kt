package com.example.filrouge.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filrouge.data.AlarmRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.TimeZone

import com.google.firebase.firestore.ListenerRegistration

data class ProfileUiState(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val timeZone: String = "",
    val activeAlarmsCount: Int = 0,
    val isCurrentUser: Boolean = true,
    val isLoading: Boolean = false
)

class ProfileViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val alarmRepo: AlarmRepository = AlarmRepository()
) : ViewModel() {

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
                timeZone = "Non renseigné",
                activeAlarmsCount = 0,
                isCurrentUser = false,
                isLoading = true
            )
            listenToOtherUserProfile(targetUserId)
            fetchOtherUserAlarms(targetUserId)
        }
    }

    private fun loadInitialState(): ProfileUiState {
        val user = auth.currentUser
        val initialName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")
            ?: ""
        val initialEmail = user?.email ?: ""
        return ProfileUiState(
            userId = user?.uid ?: "",
            displayName = initialName,
            email = initialEmail,
            timeZone = getSystemTimeZoneDisplay(),
            activeAlarmsCount = 0,
            isCurrentUser = true,
            isLoading = false
        )
    }

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

                    _uiState.value = current.copy(
                        displayName = finalName,
                        email = finalEmail,
                        isLoading = false
                    )
                }
            }
    }

    private fun listenToOtherUserProfile(targetUid: String) {
        profileListener = db.collection("users").document(targetUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val name = snapshot.getString("displayName")?.takeIf { it.isNotBlank() }
                        ?: snapshot.getString("email")?.substringBefore("@")
                        ?: "Utilisateur"
                    val email = snapshot.getString("email") ?: ""
                    val tz = snapshot.getString("timeZone")?.takeIf { it.isNotBlank() } ?: "Non renseigné"

                    _uiState.value = _uiState.value.copy(
                        displayName = name,
                        email = email,
                        timeZone = tz,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
    }

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

    private fun fetchOtherUserAlarms(targetUid: String) {
        db.collection("alarms")
            .whereArrayContains("participantIds", targetUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.documents.count { it.getBoolean("enabled") == true }
                _uiState.value = _uiState.value.copy(activeAlarmsCount = count)
            }
            .addOnFailureListener {
                // En cas d'erreur de permissions éventuelle, laisser 0
            }
    }

    companion object {
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
