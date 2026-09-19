package com.ravaa.drive.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class DrawerItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)

val drawerItems = listOf(
    DrawerItem("My Drive", Icons.Filled.Folder, "drive"),
    DrawerItem("Shared with me", Icons.Filled.People, "shared"),
    DrawerItem("Recent", Icons.Filled.Schedule, "recent"),
    DrawerItem("Starred", Icons.Filled.Star, "starred"),
    DrawerItem("Trash", Icons.Filled.Delete, "trash"),
)

@Composable
fun AppDrawer(selected: String, onSelect: (String)->Unit, onClose: ()->Unit, storageUsed: String = "13 MB / 10 GB") {
    ModalDrawerSheet {
        Spacer(Modifier.height(12.dp))
        Text("Ravaa Drive", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        Text(storageUsed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
        LinearProgressIndicator(progress = 0.02f, modifier = Modifier.padding(16.dp).fillMaxWidth())
        HorizontalDivider()
        drawerItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                icon = { Icon(item.icon, null) },
                selected = selected == item.route,
                onClick = { onSelect(item.route); onClose() },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(label={Text("Settings")}, icon={Icon(Icons.Filled.Settings,null)}, selected=false, onClick={ onSelect("settings"); onClose() })
        NavigationDrawerItem(label={Text("Help")}, icon={Icon(Icons.Filled.Help,null)}, selected=false, onClick={ onSelect("help"); onClose() })
    }
}
