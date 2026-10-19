package com.example.filrouge.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filrouge.data.AlarmRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val timeZone: String = "",
    val activeAlarmsCount: Int = 0
)

class ProfileViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val alarmRepo: AlarmRepository = AlarmRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        listenToUserProfile()
        listenToAlarms()
    }

    private fun loadInitialState(): ProfileUiState {
        val user = auth.currentUser
        val initialName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")
            ?: ""
        val initialEmail = user?.email ?: ""
        return ProfileUiState(
            displayName = initialName,
            email = initialEmail,
            timeZone = getSystemTimeZoneDisplay(),
            activeAlarmsCount = 0
        )
    }

    private fun listenToUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
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
                        email = finalEmail
                    )
                }
            }
    }

    private fun listenToAlarms() {
        viewModelScope.launch {
            alarmRepo.alarmsFlow().collect { alarms ->
                val activeCount = alarms.count { it.enabled }
                _uiState.value = _uiState.value.copy(activeAlarmsCount = activeCount)
            }
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
