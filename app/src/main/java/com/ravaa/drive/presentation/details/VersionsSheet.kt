package com.ravaa.drive.presentation.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ravaa.drive.presentation.drive.DriveViewModel
import com.ravaa.drive.util.formatDate
import com.ravaa.drive.util.formatFileSize

/** Riwayat versi file + kembalikan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionsSheet(
    vm: DriveViewModel,
    fileId: String,
    fileName: String,
    onDismiss: () -> Unit
) {
    val versions by vm.versions.collectAsState()
    LaunchedEffect(fileId) { vm.loadVersions(fileId) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414).copy(alpha = 0.95f)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Text("Version history", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(fileName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f), maxLines = 1)
            Spacer(Modifier.height(12.dp))
            if (versions.isEmpty()) {
                Text("Belum ada versi lama", color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(versions) { v ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(formatDate(v.createdAt).ifBlank { v.id.take(8) }, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                Text(formatFileSize(v.fileSize), color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { vm.restoreVersion(fileId, v.id) }) { Text("Kembalikan") }
                        }
                        HorizontalDivider(color = Color.White.copy(0.08f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
