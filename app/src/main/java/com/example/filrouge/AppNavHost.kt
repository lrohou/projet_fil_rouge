package com.example.filrouge

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onNavigateToHome = { navController.navigate("home") }
            )
        }
        composable("home") {
            HomeScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}