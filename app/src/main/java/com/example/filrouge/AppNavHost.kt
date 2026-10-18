package com.example.filrouge

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost(
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        // Écran de connexion
        composable("login") {
            LoginScreen(
                onNavigateToHome = {
                    // Navigation vers la page d'accueil en supprimant "login" de la pile
                    // Ainsi, le bouton retour ne redirige pas vers la connexion : "home" devient la racine
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        // Écran principal (avec les 3 pages en swipe)
        composable("home") {
            HomeScreen(
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }
        // Écran profil
        composable("profile") {
            ProfileScreen(
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() }
            )
        }
    }
}