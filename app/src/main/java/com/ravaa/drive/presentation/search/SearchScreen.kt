package com.ravaa.drive.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ravaa.drive.presentation.drive.DriveItemsList
import com.ravaa.drive.presentation.drive.DriveViewModel

@Composable
fun SearchScreen(
    vm: DriveViewModel = hiltViewModel(),
    onMenu: () -> Unit = {},
    nav: androidx.navigation.NavController? = null
) {
    var q by remember { mutableStateOf("") }
    val files by vm.files.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Menu, "Menu", tint = Color.White)
            }
            Text("Search", style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
        OutlinedTextField(
            value = q,
            onValueChange = { q = it; vm.search(it) },
            label = { Text("Search My Drive", color = Color.White.copy(0.7f)) },
            placeholder = { Text("min. 2 huruf...", color = Color.White.copy(0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(0.06f),
                unfocusedContainerColor = Color.White.copy(0.04f),
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color.White.copy(0.14f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF3B82F6)
            )
        )
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("Docs", color = Color.White.copy(0.85f)) },
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = Color.White.copy(0.25f))
            )
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("Images", color = Color.White.copy(0.85f)) },
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = Color.White.copy(0.25f))
            )
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("Audio", color = Color.White.copy(0.85f)) },
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = false, borderColor = Color.White.copy(0.25f))
            )
        }
        if (q.trim().length >= 2) {
            Text("Results for \"$q\"", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.75f))
            DriveItemsList(
                files,
                "Tidak ketemu",
                onItemClick = {
                    val file = it as? com.ravaa.drive.data.api.DriveFile ?: return@DriveItemsList
                    nav?.navigate(com.ravaa.drive.presentation.drive.viewerRoute(file, files))
                },
                imageLoader = vm.imageLoader,
                thumbUrl = vm::thumbUrl
            )
        } else {
            Text("Ketik minimal 2 huruf untuk mencari", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.75f))
        }
    }
}
