package com.ravaa.drive.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ravaa.drive.presentation.auth.LoginScreen
import com.ravaa.drive.presentation.components.DriveDrawer
import com.ravaa.drive.presentation.drive.DriveScreen
import com.ravaa.drive.presentation.files.FilesScreen
import com.ravaa.drive.presentation.search.SearchScreen
import com.ravaa.drive.presentation.server.ServerScreen
import com.ravaa.drive.presentation.viewer.ViewerScreen
import com.ravaa.drive.presentation.session.SessionViewModel
import com.ravaa.drive.presentation.settings.SettingsScreen
import com.ravaa.drive.presentation.shared.SharedScreen
import com.ravaa.drive.presentation.starred.StarredScreen
import com.ravaa.drive.ui.theme.GlassBackground
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("drive", "My Drive", Icons.Filled.Home),
    Tab("shared", "Shared", Icons.Filled.People),
    Tab("starred", "Starred", Icons.Filled.Star),
    Tab("files", "Files", Icons.Filled.Folder)
)

private fun androidx.navigation.NavController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Server → Login → shell ala Google Drive: side panel + bottom nav 4 tab. */
@Composable
fun NavGraph(
    session: SessionViewModel = hiltViewModel(),
    start: String? = null
) {
    val nav = rememberNavController()
    var resolved by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(start) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (resolved == null) {
            // Token tersimpan = langsung masuk (ala Google Drive).
            // Token basi/dicabut → DriveScreen terima 401 → auto-logout ke login.
            resolved = when {
                session.getServer().isNullOrBlank() -> "server"
                !session.getToken().isNullOrBlank() -> "main"
                else -> "login"
            }
        }
    }
    if (resolved == null) {
        // Splash singkat selagi baca server tersimpan
        GlassBackground {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        }
        return
    }
    NavHost(navController = nav, startDestination = resolved!!) {
        composable("server") { ServerScreen(onConnected = { nav.navigate("login") { popUpTo("server") { inclusive = true } } }) }
        composable("login") { LoginScreen(onSuccess = { nav.navigate("main") { popUpTo("login") { inclusive = true } } }) }
        composable("main") { MainScaffold(nav) }
    }
}

@Composable
private fun MainScaffold(rootNav: androidx.navigation.NavController) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route?.substringBefore("?") ?: "drive"
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val session: SessionViewModel = hiltViewModel()
    val email by session.email.collectAsState()

    fun openMenu() = scope.launch { drawerState.open() }
    fun closeMenu() = scope.launch { drawerState.close() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(drawerContainerColor = Color(0xFF141414)) {
            DriveDrawer(
                current = current,
                email = email,
                onSelect = {
                    closeMenu()
                    if (it == "settings") nav.navigate("settings") else nav.goTab(it)
                },
                onRecentTrash = { isTrash ->
                    closeMenu()
                    nav.navigate("files?tab=${if (isTrash) 1 else 0}") {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onLogout = {
                    closeMenu()
                    session.logout { rootNav.navigate("login") { popUpTo("main") { inclusive = true } } }
                }
            )
        }
    }
    ) {
        GlassBackground {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF141414).copy(alpha = 0.92f)) {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = current == t.route,
                                onClick = { nav.goTab(t.route) },
                                icon = { Icon(t.icon, null) },
                                label = { Text(t.label) }
                            )
                        }
                    }
                }
            ) { pad ->
                NavHost(navController = nav, startDestination = "drive", modifier = Modifier.padding(pad)) {
                    composable("drive") {
                        DriveScreen(
                            nav = nav,
                            onMenu = { openMenu() },
                            onSessionExpired = { rootNav.navigate("login") { popUpTo("main") { inclusive = true } } }
                        )
                    }
                    composable("shared") { SharedScreen(nav = nav, onMenu = { openMenu() }) }
                    composable("starred") { StarredScreen(nav = nav, onMenu = { openMenu() }) }
                    composable(
                        "files?tab={tab}",
                        arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
                    ) { backStack ->
                        FilesScreen(initialTab = backStack.arguments?.getInt("tab") ?: 0, onMenu = { openMenu() }, nav = nav)
                    }
                    composable("search") { SearchScreen(nav = nav, onMenu = { openMenu() }) }
                    composable(
                        "viewer?fileId={fileId}&name={name}&mime={mime}&gallery={gallery}&index={index}",
                        arguments = listOf(
                            navArgument("fileId") { type = NavType.StringType; defaultValue = "" },
                            navArgument("name") { type = NavType.StringType; defaultValue = "" },
                            navArgument("mime") { type = NavType.StringType; defaultValue = "" },
                            navArgument("gallery") { type = NavType.StringType; defaultValue = "" },
                            navArgument("index") { type = NavType.IntType; defaultValue = 0 }
                        )
                    ) { backStack ->
                        ViewerScreen(
                            fileId = backStack.arguments?.getString("fileId").orEmpty(),
                            name = backStack.arguments?.getString("name").orEmpty(),
                            mime = backStack.arguments?.getString("mime").orEmpty(),
                            galleryIds = backStack.arguments?.getString("gallery").orEmpty().split(",").filter { it.isNotBlank() },
                            startIndex = backStack.arguments?.getInt("index") ?: 0,
                            onBack = { nav.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onMenu = { openMenu() },
                            onLoggedOut = { rootNav.navigate("login") { popUpTo("main") { inclusive = true } } }
                        )
                    }
                }
            }
        }
    }
}
