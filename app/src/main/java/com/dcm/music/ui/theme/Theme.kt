package com.dcm.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DcmBlack = Color(0xFF05070D)
val DcmSurface = Color(0xFF0D1220)
val DcmCard = Color(0xFF141B2E)
val DcmNeon = Color(0xFF00B4FF)
val DcmElectric = Color(0xFF2979FF)
val DcmGray = Color(0xFF9AA4B8)

private val DcmColors = darkColorScheme(
    primary = DcmNeon,
    onPrimary = Color.Black,
    secondary = DcmElectric,
    background = DcmBlack,
    onBackground = Color.White,
    surface = DcmSurface,
    onSurface = Color.White,
    surfaceVariant = DcmCard,
    onSurfaceVariant = DcmGray
)

@Composable
fun DcmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DcmColors, content = content)
}
