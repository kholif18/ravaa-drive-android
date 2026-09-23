package com.ravaa.drive.presentation.drive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ravaa.drive.data.api.DriveFolder

/** Pilih folder tujuan ala Google Drive (root = My Drive). */
@Composable
fun MoveDialog(
    folders: List<DriveFolder>,
    onNavigate: (String?) -> Unit,
    onDismiss: () -> Unit,
    onMove: (targetId: String?) -> Unit
) {
    var trail by remember { mutableStateOf(listOf<Pair<String?, String>>(null to "My Drive")) }
    val current = trail.last()

    LaunchedEffect(trail) { onNavigate(current.first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pindah ke...", color = Color.White) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trail.size > 1) {
                        IconButton(onClick = { trail = trail.dropLast(1) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.ArrowBack, "Kembali", tint = Color.White)
                        }
                    }
                    Text(
                        trail.map { it.second }.joinToString(" / "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.75f),
                        maxLines = 2
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (folders.isEmpty()) {
                    Text("Tidak ada subfolder", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                } else {
                    LazyColumn {
                        items(folders) { f ->
                            ListItem(
                                headlineContent = { Text(f.name, color = Color.White, maxLines = 1) },
                                leadingContent = { Icon(Icons.Filled.Folder, null, tint = Color(0xFF60A5FA)) },
                                modifier = Modifier.clickable { trail = trail + (f.id to f.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onMove(current.first) }) { Text("Pindahkan ke sini") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
