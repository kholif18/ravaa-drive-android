package com.ravaa.drive.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ravaa.drive.R

private data class DrawerEntry(val route: String, val label: String, val icon: ImageVector)

private val mainEntries = listOf(
    DrawerEntry("drive", "My Drive", Icons.Filled.Home),
    DrawerEntry("shared", "Shared", Icons.Filled.People),
    DrawerEntry("starred", "Starred", Icons.Filled.Star),
    DrawerEntry("files", "Files", Icons.Filled.Folder)
)

private val secondEntries = listOf(
    DrawerEntry("recent", "Recent", Icons.Filled.History),
    DrawerEntry("trash", "Trash", Icons.Filled.Delete)
)

/** Side panel ala Google Drive: header logo, menu, email + logout. */
@Composable
fun DriveDrawer(
    current: String,
    email: String?,
    onSelect: (String) -> Unit,
    onRecentTrash: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.logo),
                null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Ravaa Drive", style = MaterialTheme.typography.titleMedium)
                Text("Personal cloud", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
            }
        }
        HorizontalDivider(color = Color.White.copy(0.08f))
        Spacer(Modifier.height(8.dp))
        mainEntries.forEach { e ->
            NavigationDrawerItem(
                label = { Text(e.label) },
                selected = current == e.route,
                onClick = { onSelect(e.route) },
                icon = { Icon(e.icon, null) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = Color.White.copy(0.08f), modifier = Modifier.padding(vertical = 8.dp))
        secondEntries.forEach { e ->
            NavigationDrawerItem(
                label = { Text(e.label) },
                selected = false,
                onClick = { onRecentTrash(e.route == "trash") },
                icon = { Icon(e.icon, null) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        NavigationDrawerItem(
            label = { Text("Settings") },
            selected = current == "settings",
            onClick = { onSelect("settings") },
            icon = { Icon(Icons.Filled.Settings, null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = Color.White.copy(0.08f))
        if (!email.isNullOrBlank()) {
            Text(
                email,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.8f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        NavigationDrawerItem(
            label = { Text("Logout") },
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.Filled.Logout, null) },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(12.dp))
    }
}
