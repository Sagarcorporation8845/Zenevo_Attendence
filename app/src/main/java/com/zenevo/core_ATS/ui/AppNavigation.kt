package com.zenevo.core_ATS.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("forgot-password") { ForgotPasswordScreen(navController) }
        composable("dashboard") { DashboardScreen(navController) }
        composable("history") { HistoryScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("success") { AttendanceSuccessScreen(navController) }
        composable("security-audit") { SecurityAuditScreen(navController) }
    }
}
