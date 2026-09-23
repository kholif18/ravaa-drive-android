package com.ravaa.drive.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ravaa.drive.BuildConfig
import com.ravaa.drive.presentation.session.SessionViewModel
import com.ravaa.drive.ui.theme.GlassBackground
import com.ravaa.drive.ui.theme.glassCard
import com.ravaa.drive.util.formatFileSize

/** Settings: server URL, storage, cache, about, logout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMenu: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
    session: SessionViewModel = hiltViewModel()
) {
    val server by vm.server.collectAsState()
    val saved by vm.saved.collectAsState()
    val storage by vm.storage.collectAsState()
    val cacheSize by vm.cacheSize.collectAsState()

    GlassBackground {
        Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Menu, "Menu")
                }
                Text("Settings", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))

            // Server
            Box(Modifier.fillMaxWidth().glassCard(20.dp).padding(16.dp)) {
                Column {
                    Text("Server", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = server,
                        onValueChange = { vm.setServer(it) },
                        label = { Text("Server URL") },
                        placeholder = { Text(BuildConfig.DRIVE_BASE_URL) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    var loggingOut by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            loggingOut = true
                            vm.saveServerAndLogout { session.logout(onLoggedOut) }
                        }) { Text("Simpan") }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (loggingOut) "Menyimpan..." else if (saved) "Tersimpan" else "Ganti server = login ulang",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.6f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Storage
            Box(Modifier.fillMaxWidth().glassCard(20.dp).padding(16.dp)) {
                Column {
                    Text("Storage", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val st = storage
                    if (st == null) {
                        Text("Memuat...", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                    } else {
                        val pct = if (st.limit > 0) (st.used.toDouble() / st.limit).coerceIn(0.0, 1.0).toFloat() else 0f
                        LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth().height(6.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${formatFileSize(st.used)} of ${formatFileSize(st.limit)} • ${st.count} files",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.6f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Cache
            Box(Modifier.fillMaxWidth().glassCard(20.dp).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Cache", style = MaterialTheme.typography.titleSmall)
                        Text(
                            formatFileSize(cacheSize),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.6f)
                        )
                    }
                    OutlinedButton(onClick = { vm.clearCache() }) {
                        Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bersihkan")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // About
            Box(Modifier.fillMaxWidth().glassCard(20.dp).padding(16.dp)) {
                Column {
                    Text("About", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Ravaa Drive ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodySmall)
                    Text("Personal cloud • Drive • Notes • Tasks", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                }
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { session.logout(onLoggedOut) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
            ) { Text("Logout") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
