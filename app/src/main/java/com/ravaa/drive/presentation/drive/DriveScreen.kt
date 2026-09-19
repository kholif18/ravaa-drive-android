package com.ravaa.drive.presentation.drive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(vm: DriveViewModel = hiltViewModel()) {
    val files by vm.files.collectAsState()
    var isGrid by remember { mutableStateOf(false) }
    var showFabSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showFabSheet = true }) { Icon(Icons.Filled.Add, null) }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // Breadcrumb sticky
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("My Drive", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { vm.load(null) })
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { isGrid = !isGrid }, modifier = Modifier.size(28.dp)) {
                    Icon(if (isGrid) Icons.Filled.ViewList else Icons.Filled.GridView, null, modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider()
            if (files.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Empty — drop files here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                if (isGrid) {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(files) { f -> DriveGridItem(f) }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(files) { f -> DriveListItem(f) }
                    }
                }
            }
        }
        if (showFabSheet) {
            ModalBottomSheet(onDismissRequest = { showFabSheet = false }) {
                ListItem(headlineContent = { Text("Upload file") }, leadingContent = { Icon(Icons.Filled.Upload, null) }, modifier = Modifier.clickable { showFabSheet = false })
                ListItem(headlineContent = { Text("Upload folder") }, leadingContent = { Icon(Icons.Filled.Folder, null) }, modifier = Modifier.clickable { showFabSheet = false })
                ListItem(headlineContent = { Text("New folder") }, leadingContent = { Icon(Icons.Filled.CreateNewFolder, null) }, modifier = Modifier.clickable { showFabSheet = false })
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DriveListItem(f: Any) {
    val name = (f as? com.ravaa.drive.data.api.DriveFile)?.name ?: (f as? com.ravaa.drive.data.api.DriveFolder)?.name ?: "Unknown"
    ListItem(
        headlineContent = { Text(name, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = { Text("Modified • owner", style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(Icons.Filled.InsertDriveFile, null) },
        trailingContent = { IconButton(onClick = {}) { Icon(Icons.Filled.MoreVert, null) } }
    )
    HorizontalDivider()
}

@Composable
private fun DriveGridItem(f: Any) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.InsertDriveFile, null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text((f as? com.ravaa.drive.data.api.DriveFile)?.name ?: (f as? com.ravaa.drive.data.api.DriveFolder)?.name ?: "Unknown", style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}
