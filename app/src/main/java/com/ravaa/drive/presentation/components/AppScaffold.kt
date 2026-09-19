package com.ravaa.drive.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ravaa.drive.presentation.drive.DriveScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val nav = rememberNavController()
    var current by remember { mutableStateOf("drive") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { AppDrawer(selected = current, onSelect = { r -> current = r; nav.navigate(r) { launchSingleTop = true } }, onClose = { scope.launch { drawerState.close() } }) }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Ravaa Drive") },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Filled.Menu, null) } },
                    actions = { IconButton(onClick = { nav.navigate("search") }) { Icon(Icons.Filled.Search, null) } }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = current=="drive", onClick = { current="drive"; nav.navigate("drive") }, icon = { Icon(Icons.Filled.Folder, null) }, label = { Text("My Drive") })
                    NavigationBarItem(selected = current=="shared", onClick = { current="shared"; nav.navigate("shared") }, icon = { Icon(Icons.Filled.People, null) }, label = { Text("Shared") })
                    NavigationBarItem(selected = current=="starred", onClick = { current=="starred"; nav.navigate("starred") }, icon = { Icon(Icons.Filled.Star, null) }, label = { Text("Starred") })
                    NavigationBarItem(selected = current=="trash", onClick = { current="trash"; nav.navigate("trash") }, icon = { Icon(Icons.Filled.Delete, null) }, label = { Text("Files") })
                }
            }
        ) { pad ->
            NavHost(navController = nav, startDestination = "drive", modifier = Modifier.padding(pad)) {
                composable("drive") { DriveScreen() }
                composable("shared") { DriveScreen() }
                composable("starred") { DriveScreen() }
                composable("trash") { DriveScreen() }
                composable("recent") { DriveScreen() }
                composable("search") { Text("Search — global ?q=") }
            }
        }
    }
}
