package com.ravaa.drive.presentation.server

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ravaa.drive.R
import com.ravaa.drive.ui.theme.GlassBackground
import com.ravaa.drive.ui.theme.glassCard

/** Layar pertama ala Nextcloud: pilih server dulu, baru login. */
@Composable
fun ServerScreen(onConnected: () -> Unit, vm: ServerViewModel = hiltViewModel()) {
    val url by vm.url.collectAsState()
    val state by vm.state.collectAsState()
    val checking = state is ServerState.Checking
    fun submit() { if (!checking) vm.connect { onConnected() } }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White.copy(0.06f),
        unfocusedContainerColor = Color.White.copy(0.04f),
        focusedBorderColor = Color(0xFF3B82F6),
        unfocusedBorderColor = Color.White.copy(0.14f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        cursorColor = Color(0xFF3B82F6)
    )

    GlassBackground {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.glassCard(32.dp).padding(28.dp, 32.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painterResource(R.drawable.logo),
                        contentDescription = "Ravaa Drive",
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Connect to server", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(
                        "Server Ravaa Drive kamu",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { vm.setUrl(it) },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://192.168.x.x:2713", color = Color.White.copy(0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { submit() })
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { submit() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !checking,
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text(if (checking) "Menghubungkan..." else "Connect")
                    }
                    when (val s = state) {
                        is ServerState.Ok -> {
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Terhubung${if (s.version.isNotBlank()) " • v${s.version}" else ""}",
                                    color = Color(0xFF34D399),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is ServerState.Error -> {
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Error, null, tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.msg, color = Color(0xFFF87171), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
