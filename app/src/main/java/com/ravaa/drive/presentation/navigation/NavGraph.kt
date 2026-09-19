package com.ravaa.drive.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ravaa.drive.presentation.auth.LoginScreen
import com.ravaa.drive.presentation.drive.DriveScreen

@Composable
fun NavGraph(start: String = "login") {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = start) {
        composable("login") { LoginScreen(onSuccess = { nav.navigate("drive") { popUpTo("login"){inclusive=true} } }) }
        composable("drive") { DriveScreen() }
    }
}
