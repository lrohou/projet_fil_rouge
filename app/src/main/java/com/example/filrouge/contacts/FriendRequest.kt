package com.example.filrouge.contacts

/**
 * Représente une demande d'ami entrante en attente d'approbation.
 */
data class FriendRequest(
    val friendshipId: String,
    val from: AppUser
)
