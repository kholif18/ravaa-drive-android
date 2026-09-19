package com.ravaa.drive.presentation.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ravaa.drive.presentation.drive.DriveViewModel

@Composable
fun SearchScreen(vm: DriveViewModel = hiltViewModel()) {
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(value=q, onValueChange={q=it; vm.load(null)}, label={Text("Search My Drive")}, modifier=Modifier.fillMaxWidth())
        Row(Modifier.padding(vertical=8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=false, onClick={}, label={Text("Docs")})
            FilterChip(selected=false, onClick={}, label={Text("Images")})
            FilterChip(selected=false, onClick={}, label={Text("Audio")})
        }
        Text("Results for \"$q\"", style=MaterialTheme.typography.bodySmall)
    }
}
