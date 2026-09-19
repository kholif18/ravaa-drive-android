package com.ravaa.drive.presentation.recent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable fun RecentScreen(){ Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){ Text("Recent — sorted by updatedAt") } }
