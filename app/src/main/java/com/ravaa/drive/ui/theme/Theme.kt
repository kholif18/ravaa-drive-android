package com.ravaa.drive.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(primary=Color(0xFF3B82F6), background=Color(0xFF0A0A0A), surface=Color(0xFF141414))
@Composable fun RavaaTheme(content: @Composable ()->Unit){ MaterialTheme(colorScheme=DarkScheme, content=content) }
