package com.ravaa.drive.presentation.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ravaa.drive.presentation.drive.DriveViewModel
import kotlinx.coroutines.launch

/** Buat link publik + salin. */
@Composable
fun ShareDialog(
    vm: DriveViewModel,
    itemId: String,
    itemName: String,
    isFolder: Boolean,
    onDismiss: () -> Unit
) {
    var link by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(itemId) {
        loading = true
        error = null
        try {
            link = vm.makeShareLink(itemId, isFolder)
        } catch (e: Exception) {
            error = e.message ?: "Gagal membuat link"
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share link", color = Color.White) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(itemName, color = Color.White.copy(0.75f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Spacer(Modifier.height(12.dp))
                when {
                    loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Membuat link...", color = Color.White.copy(0.75f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    link != null -> {
                        OutlinedTextField(
                            value = link!!,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(link!!))
                                scope.launch { }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Salin link")
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Siapa pun dengan link bisa melihat", color = Color.White.copy(0.6f), style = MaterialTheme.typography.labelSmall)
                    }
                    else -> Text(error ?: "Gagal", color = Color(0xFFF87171), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } }
    )
}
