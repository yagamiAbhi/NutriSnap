package com.nutrisnap.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF176B52),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD8EFE5),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF123D30),
    secondary = androidx.compose.ui.graphics.Color(0xFFB85C38),
    tertiary = androidx.compose.ui.graphics.Color(0xFF315E73),
    background = androidx.compose.ui.graphics.Color(0xFFF4F7F4),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFEFC),
    onSurface = androidx.compose.ui.graphics.Color(0xFF17221D),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF59665F),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFDCE4DE)
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF8AD0B2),
    secondary = androidx.compose.ui.graphics.Color(0xFFFFB095),
    tertiary = androidx.compose.ui.graphics.Color(0xFF9DCEE2)
)

@Composable
fun NutriSnapTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
