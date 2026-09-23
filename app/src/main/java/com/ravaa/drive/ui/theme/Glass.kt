package com.ravaa.drive.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Mac-style frosted glass card: translucent gradient + hairline border. */
fun Modifier.glassCard(corner: Dp = 20.dp): Modifier = this
    .clip(RoundedCornerShape(corner))
    .background(
        Brush.linearGradient(
            listOf(Color.White.copy(0.10f), Color.White.copy(0.04f))
        )
    )
    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(corner))

/**
 * Dark base + soft color glows (radial gradients, no API-31 blur needed).
 * Menyediakan LocalContentColor PUTIH — Box telanjang tidak memberi warna
 * konten (default hitam), jadi semua Text/Icon tanpa warna eksplisit di
 * dalamnya tetap terang di dark mode.
 */
@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        Box(
            Modifier
                .size(320.dp)
                .offset((-90).dp, (-70).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF3B82F6).copy(0.35f), Color.Transparent),
                        radius = 320f
                    )
                )
        )
        Box(
            Modifier
                .size(300.dp)
                .offset(180.dp, 520.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF8B5CF6).copy(0.25f), Color.Transparent),
                        radius = 300f
                    )
                )
        )
        content()
    }
    }
}
