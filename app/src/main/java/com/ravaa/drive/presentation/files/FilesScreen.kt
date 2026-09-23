package com.ravaa.drive.presentation.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ravaa.drive.presentation.details.FileDetailsSheet
import com.ravaa.drive.presentation.drive.DriveItemsList
import com.ravaa.drive.presentation.drive.DriveViewModel
import com.ravaa.drive.ui.theme.GlassBackground

/** Tab Files ala Google Drive: Recent + Trash (restore/hapus permanen). */
@Composable
fun FilesScreen(
    vm: DriveViewModel = hiltViewModel(),
    initialTab: Int = 0,
    onMenu: () -> Unit = {},
    nav: androidx.navigation.NavController? = null
) {
    val files by vm.files.collectAsState()
    var tab by remember(initialTab) { mutableIntStateOf(initialTab) }

    LaunchedEffect(tab) { if (tab == 0) vm.loadRecent() else vm.loadTrash() }

    GlassBackground {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Menu, "Menu", tint = Color.White)
                }
                Text("Files", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tab]),
                        color = Color(0xFF3B82F6)
                    )
                }
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("Recent", color = if (tab == 0) Color.White else Color.White.copy(0.6f)) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("Trash", color = if (tab == 1) Color.White else Color.White.copy(0.6f)) }
                )
            }
            var trashItem by remember { mutableStateOf<Any?>(null) }
            fun view(f: Any) {
                val file = f as? com.ravaa.drive.data.api.DriveFile ?: return
                nav?.navigate(com.ravaa.drive.presentation.drive.viewerRoute(file, files))
            }
            if (tab == 0) {
                DriveItemsList(files, "Belum ada aktivitas", onItemClick = { view(it) }, imageLoader = vm.imageLoader, thumbUrl = vm::thumbUrl)
            } else {
                DriveItemsList(
                    files,
                    "Trash kosong",
                    onItemClick = { trashItem = it },
                    imageLoader = vm.imageLoader,
                    thumbUrl = vm::thumbUrl,
                    trailing = { f ->
                        // Folder di trash: aksi restore permanen belum didukung API (lihat saja)
                        val file = f as? com.ravaa.drive.data.api.DriveFile ?: return@DriveItemsList
                        val id = file.id
                        Row {
                            IconButton(onClick = { vm.restoreFile(id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Restore, "Restore", tint = Color(0xFF34D399))
                            }
                            IconButton(onClick = { vm.deletePermanent(id) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Delete, "Delete", tint = Color(0xFFF87171))
                            }
                        }
                    }
                )
            }
            // Spacer agar konten tidak tertutup nav bar: ditangani padding scaffold
            Text(
                if (tab == 0) "File yang baru diubah" else "Item di trash 30 hari lalu dihapus permanen",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.5f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            trashItem?.let { item ->
                FileDetailsSheet(
                    item = item,
                    trashMode = true,
                    onDismiss = { trashItem = null },
                    onRestore = { id -> vm.restoreFile(id); trashItem = null },
                    onDeletePermanent = { id -> vm.deletePermanent(id); trashItem = null }
                )
            }
        }
    }
}
