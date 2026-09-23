package com.ravaa.drive.presentation.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ravaa.drive.presentation.drive.DriveItemsList
import com.ravaa.drive.presentation.drive.DriveViewModel

@Composable
fun SharedScreen(
    vm: DriveViewModel = hiltViewModel(),
    onMenu: () -> Unit = {},
    nav: androidx.navigation.NavController? = null
) {
    val files by vm.files.collectAsState()
    LaunchedEffect(Unit) { vm.loadShared() }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Menu, "Menu", tint = Color.White)
            }
            Text("Shared with me", style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
        DriveItemsList(
            files,
            "Tidak ada file shared",
            onItemClick = {
                val file = it as? com.ravaa.drive.data.api.DriveFile ?: return@DriveItemsList
                nav?.navigate(com.ravaa.drive.presentation.drive.viewerRoute(file, files))
            },
            imageLoader = vm.imageLoader,
            thumbUrl = vm::thumbUrl
        )
    }
}
