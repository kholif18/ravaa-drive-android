package com.ravaa.drive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ravaa.drive.presentation.navigation.NavGraph
import com.ravaa.drive.ui.theme.RavaaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RavaaTheme { NavGraph() } }
    }
}
