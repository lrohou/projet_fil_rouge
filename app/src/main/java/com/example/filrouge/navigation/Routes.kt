package com.example.filrouge.navigation

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val PROFILE = "profile"

    const val USER_PROFILE = "profile/{userId}"

    fun userProfilePath(userId: String): String {
        return "profile/$userId"
    }
}