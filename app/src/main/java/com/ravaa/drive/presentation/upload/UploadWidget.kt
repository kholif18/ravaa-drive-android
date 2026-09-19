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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun UploadWidget(vm: UploadViewModel = hiltViewModel()) {
    val uploads by vm.uploads.collectAsState()
    if (uploads.isEmpty()) return
    Card(Modifier.fillMaxWidth().padding(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Uploading ${uploads.size} files", style = MaterialTheme.typography.titleSmall)
                Row {
                    IconButton(onClick = { uploads.filter{it.status=="uploading"}.forEach{ vm.pause(it.id)} }) { Icon(Icons.Filled.Pause, null) }
                    IconButton(onClick = { uploads.filter{it.status=="paused"}.forEach{ vm.resume(it.id)} }) { Icon(Icons.Filled.PlayArrow, null) }
                    IconButton(onClick = { vm.cancelAll() }) { Icon(Icons.Filled.Close, null) }
                }
            }
            LinearProgressIndicator(progress = { uploads.map{it.progress}.average().toFloat()/100f }, modifier = Modifier.fillMaxWidth().padding(vertical=8.dp))
            LazyColumn(modifier = Modifier.heightIn(max=200.dp)) {
                items(uploads) { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical=4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.InsertDriveFile, null, modifier=Modifier.size(24.dp))
                        Column(Modifier.weight(1f).padding(horizontal=8.dp)) {
                            Text(item.name, style=MaterialTheme.typography.bodySmall, maxLines=1)
                            LinearProgressIndicator(progress={item.progress/100f}, modifier=Modifier.fillMaxWidth().height(4.dp))
                        }
                        Text("${item.progress}%", style=MaterialTheme.typography.labelSmall)
                        if(item.status=="uploading") IconButton(onClick={vm.pause(item.id)}, modifier=Modifier.size(24.dp)){ Icon(Icons.Filled.Pause, null, modifier=Modifier.size(16.dp)) }
                        if(item.status=="paused") IconButton(onClick={vm.resume(item.id)}, modifier=Modifier.size(24.dp)){ Icon(Icons.Filled.PlayArrow, null, modifier=Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}
