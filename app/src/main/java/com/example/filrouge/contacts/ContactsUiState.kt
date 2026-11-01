package com.example.filrouge.contacts

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