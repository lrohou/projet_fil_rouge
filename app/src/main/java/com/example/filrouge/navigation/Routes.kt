package com.example.filrouge.navigation

/**
 * Définition des routes et arguments de navigation de l'application.
 */
object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val PROFILE = "profile"

    const val USER_PROFILE = "profile/{userId}"

    /** Génère le chemin de navigation vers le profil d'un utilisateur spécifique */
    fun userProfilePath(userId: String): String {
        return "profile/$userId"
    }
}
