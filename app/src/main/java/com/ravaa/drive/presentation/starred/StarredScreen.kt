package com.ravaa.drive.presentation.starred

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ravaa.drive.presentation.drive.DriveViewModel

@Composable
fun StarredScreen(vm: DriveViewModel = hiltViewModel()) {
    LaunchedEffect(Unit){ vm.load(null) }
    Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){ Text("Starred — filter isStarred=true") }
}
