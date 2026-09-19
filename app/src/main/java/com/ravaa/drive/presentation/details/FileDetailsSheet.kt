package com.ravaa.drive.presentation.details

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailsSheet(fileName: String, onDismiss: ()->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(fileName, style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Info • Activity • Share LINK (password/expiry/maxViews)", style=MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Button(onClick=onDismiss, modifier=Modifier.fillMaxWidth()){ Text("Close") }
        }
    }
}
