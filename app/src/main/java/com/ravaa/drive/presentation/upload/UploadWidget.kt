package com.ravaa.drive.presentation.upload

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ravaa.drive.ui.theme.glassCard

@Composable
fun UploadWidget(vm: UploadViewModel = hiltViewModel()) {
    val uploads by vm.uploads.collectAsState()
    if (uploads.isEmpty()) return
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).glassCard()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Uploading ${uploads.size} files", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Row {
                    IconButton(onClick = { uploads.filter{it.status=="uploading"}.forEach{ vm.pause(it.id)} }) { Icon(Icons.Filled.Pause, null, tint = Color.White) }
                    IconButton(onClick = { uploads.filter{it.status=="paused"}.forEach{ vm.resume(it.id)} }) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White) }
                    IconButton(onClick = { vm.cancelAll() }) { Icon(Icons.Filled.Close, null, tint = Color.White) }
                }
            }
            LinearProgressIndicator(progress = { uploads.map{it.progress}.average().toFloat()/100f }, modifier = Modifier.fillMaxWidth().padding(vertical=8.dp))
            LazyColumn(modifier = Modifier.heightIn(max=200.dp)) {
                items(uploads) { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical=4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.InsertDriveFile, null, modifier=Modifier.size(24.dp), tint = Color.White.copy(0.9f))
                        Column(Modifier.weight(1f).padding(horizontal=8.dp)) {
                            Text(item.name, style=MaterialTheme.typography.bodySmall, maxLines=1, color = Color.White)
                            if (item.status == "error" && item.error != null) {
                                Text(item.error, style=MaterialTheme.typography.labelSmall, color=Color(0xFFF87171), maxLines=2)
                            } else {
                                LinearProgressIndicator(progress={item.progress/100f}, modifier=Modifier.fillMaxWidth().height(4.dp), trackColor = Color.White.copy(0.15f))
                            }
                        }
                        Text(if (item.status == "success") "OK" else "${item.progress}%", style=MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f))
                        if(item.status=="uploading") IconButton(onClick={vm.pause(item.id)}, modifier=Modifier.size(24.dp)){ Icon(Icons.Filled.Pause, null, modifier=Modifier.size(16.dp), tint = Color.White) }
                        if(item.status=="paused") IconButton(onClick={vm.resume(item.id)}, modifier=Modifier.size(24.dp)){ Icon(Icons.Filled.PlayArrow, null, modifier=Modifier.size(16.dp), tint = Color.White) }
                    }
                }
            }
        }
    }
}
