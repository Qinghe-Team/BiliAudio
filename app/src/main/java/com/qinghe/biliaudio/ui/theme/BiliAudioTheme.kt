package com.qinghe.biliaudio.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography

private val BiliAudioColors = Colors(
    primary = androidx.compose.ui.graphics.Color(0xFF6EC6FF),
    primaryVariant = androidx.compose.ui.graphics.Color(0xFF2B7AA8),
    secondary = androidx.compose.ui.graphics.Color(0xFFFFA8D8),
    secondaryVariant = androidx.compose.ui.graphics.Color(0xFFC76B9F),
    error = androidx.compose.ui.graphics.Color(0xFFFF6E6E),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF0D1B2A),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF241021),
    onError = androidx.compose.ui.graphics.Color.White,
    background = androidx.compose.ui.graphics.Color(0xFF050B12),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF3F6FA),
    surface = androidx.compose.ui.graphics.Color(0xFF111927),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF3F6FA)
)

@Composable
fun BiliAudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = BiliAudioColors,
        typography = Typography(),
        content = content
    )
}
