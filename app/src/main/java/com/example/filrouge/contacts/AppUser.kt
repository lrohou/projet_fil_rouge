package com.example.filrouge.contacts

/**
 * Représentation d'un utilisateur de l'application enregistré dans la base de données Firestore.
 */
data class AppUser(
    val uid: String = "",
    val displayName: String = "",
    val email: String = ""
)
