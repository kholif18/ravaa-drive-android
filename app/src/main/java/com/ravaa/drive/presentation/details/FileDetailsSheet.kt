package com.ravaa.drive.presentation.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ravaa.drive.data.api.DriveFile
import com.ravaa.drive.data.api.DriveFolder
import com.ravaa.drive.util.formatDate
import com.ravaa.drive.util.formatFileSize

/** Bottom sheet detail ala Google Drive: info + aksi lengkap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsSheet(
    item: Any,
    onDismiss: () -> Unit,
    trashMode: Boolean = false,
    onToggleStar: (DriveFile) -> Unit = {},
    onRename: (String, String) -> Unit = { _, _ -> },
    onTrash: (String) -> Unit = {},
    onRestore: (String) -> Unit = {},
    onDeletePermanent: (String) -> Unit = {},
    onDownload: (String) -> Unit = {},
    onMove: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onCopy: (String) -> Unit = {},
    onVersions: (String) -> Unit = {}
) {
    val name = (item as? DriveFile)?.name ?: (item as? DriveFolder)?.name ?: "Unknown"
    val isFolder = item is DriveFolder
    val isStarred = (item as? DriveFile)?.isStarred == true
    val id = (item as? DriveFile)?.id ?: (item as? DriveFolder)?.id ?: ""

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414).copy(alpha = 0.95f)
    ) {
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isFolder) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
                    null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isFolder) Color(0xFF60A5FA) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, color = Color.White)
                    Text(
                        if (isFolder) "Folder" else "${formatFileSize((item as DriveFile).fileSize)} • ${formatDate(item.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.7f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(0.08f))
            if (trashMode) {
                SheetAction(Icons.Filled.Restore, "Restore", tint = Color(0xFF34D399)) { onRestore(id); onDismiss() }
                SheetAction(Icons.Filled.Delete, "Delete permanently", tint = Color(0xFFF87171)) { onDeletePermanent(id); onDismiss() }
            } else {
                if (!isFolder) {
                    SheetAction(Icons.Filled.Download, "Download") { onDownload(id); onDismiss() }
                    SheetAction(Icons.Filled.Star, if (isStarred) "Unstar" else "Star", tint = if (isStarred) Color(0xFFFBBF24) else null) {
                        onToggleStar(item as DriveFile); onDismiss()
                    }
                }
                SheetAction(Icons.Filled.DriveFileMove, "Move") { onMove(id); onDismiss() }
                SheetAction(Icons.Filled.Share, "Share link") { onShare(id); onDismiss() }
                SheetAction(Icons.Filled.ContentCopy, "Make a copy") { onCopy(id); onDismiss() }
                if (!isFolder) {
                    SheetAction(Icons.Filled.History, "Version history") { onVersions(id); onDismiss() }
                }
                SheetAction(Icons.Filled.Edit, "Rename") { onRename(id, name); onDismiss() }
                SheetAction(Icons.Filled.Delete, "Delete", tint = Color(0xFFF87171)) { onTrash(id); onDismiss() }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint ?: Color.White.copy(0.9f))
            Spacer(Modifier.width(16.dp))
            Text(label, color = tint ?: Color.White.copy(0.9f))
        }
    }
}
