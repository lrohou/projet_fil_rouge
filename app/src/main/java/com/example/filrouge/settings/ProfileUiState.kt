package com.example.filrouge.settings

/**
 * État de l'interface utilisateur pour la consultation de profil (compte personnel ou ami).
 */
data class ProfileUiState(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val timeZone: String = "",
    val activeAlarmsCount: Int = 0,
    val isCurrentUser: Boolean = true,
    val isLoading: Boolean = false
)
