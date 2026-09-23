package com.ravaa.drive.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ravaa.drive.R
import com.ravaa.drive.ui.theme.GlassBackground
import com.ravaa.drive.ui.theme.glassCard

@Composable
fun LoginScreen(onSuccess: () -> Unit, vm: LoginViewModel = hiltViewModel()) {
    var id by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val st by vm.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val loading = st is LoginState.Loading
    fun submit() { if (!loading && id.isNotBlank() && pass.isNotEmpty()) vm.login(id, pass) }
    LaunchedEffect(st) { if (st is LoginState.Success) onSuccess() }

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
                    Text("Ravaa Drive", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    Text(
                        "Your personal cloud",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        label = { Text("Email / Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = fieldColors,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPass) "Hide password" else "Show password"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); submit() })
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { submit() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !loading,
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Text(if (loading) "Loading..." else "Login")
                    }
                    if (st is LoginState.Error) {
                        Spacer(Modifier.height(12.dp))
                        Text((st as LoginState.Error).msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
