package com.ravaa.drive.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(onSuccess: () -> Unit, vm: LoginViewModel = hiltViewModel()) {
    var id by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    val st by vm.state.collectAsState()
    LaunchedEffect(st) { if (st is LoginState.Success) onSuccess() }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Ravaa Drive — Login", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value=id, onValueChange={id=it}, label={Text("Email / Username")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value=pass, onValueChange={pass=it}, label={Text("Password")}, modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick={vm.login(id,pass)}, modifier=Modifier.fillMaxWidth(), enabled=st !is LoginState.Loading) {
            Text(if(st is LoginState.Loading) "Loading..." else "Login")
        }
        if(st is LoginState.Error) Text((st as LoginState.Error).msg, color=MaterialTheme.colorScheme.error)
    }
}
