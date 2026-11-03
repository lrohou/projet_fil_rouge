package com.example.filrouge.contacts

/**
 * État de l'interface utilisateur pour la gestion des contacts et demandes d'amitié.
 */
data class ContactsUiState(
    val users: List<AppUser> = emptyList(),
    val filteredUsers: List<AppUser> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val friendRequestsSent: Set<String> = emptySet(),
    val friendIds: Set<String> = emptySet(),
    val incomingRequests: List<FriendRequest> = emptyList(),
    val error: String? = null
)
