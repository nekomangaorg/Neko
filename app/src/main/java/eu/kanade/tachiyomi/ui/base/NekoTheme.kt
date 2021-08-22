package eu.kanade.tachiyomi.ui.base

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun NekoTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val myColors = sapphireDusk
    MaterialTheme(colors = myColors,
        content = content)
}

val sapphireDusk = darkColors(
    primary = Color(62, 108, 129),
    secondary = Color(88, 154, 184),
    secondaryVariant = Color(121, 174, 198)
)
