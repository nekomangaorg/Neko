package org.nekomanga.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.nekomanga.presentation.theme.NekoTheme
import org.nekomanga.presentation.theme.Size
import org.nekomanga.presentation.theme.Themes
import org.nekomanga.presentation.theme.colorSchemeFromTheme

@Composable
private fun NekoThemePreview(theme: Themes, isDark: Boolean, content: @Composable () -> Unit) {
    NekoTheme(
        colorScheme =
            colorSchemeFromTheme(LocalContext.current, theme = theme, isSystemInDarkTheme = isDark)
    ) {
        Surface(modifier = Modifier.padding(Size.tiny)) { content() }
    }
}

@Composable
fun ThemedPreviews(themeConfig: ThemeConfig, content: @Composable (theme: Themes) -> Unit) {
    NekoThemePreview(themeConfig.theme, themeConfig.isDark) {
        Column {
            // Renders the theme name at the top of every preview canvas
            Text(
                text = themeConfig.displayName, // e.g., "Monet Dark"
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Size.small),
            )

            // Renders your actual component below it
            content(themeConfig.theme)
        }
    }
}

data class ThemeConfig(val theme: Themes, val isDark: Boolean) {
    val displayName: String
        get() = "${theme.name} ${if (isDark) "Dark" else "Light"}"

    override fun toString(): String = ""
}

data class Themed<T>(val value: T, val themeConfig: ThemeConfig) {
    val displayName: String
        get() = "${themeConfig.theme.name} ${if (themeConfig.isDark) "Dark" else "Light"}"

    override fun toString(): String = ""
}

class ThemeConfigProvider : PreviewParameterProvider<ThemeConfig> {
    override val values: Sequence<ThemeConfig> =
        Themes.entries.asSequence().flatMap { theme ->
            sequenceOf(ThemeConfig(theme, false), ThemeConfig(theme, true))
        }
}

fun <T> Sequence<T>.withThemes(): Sequence<Themed<T>> = flatMap { item ->
    ThemeConfigProvider().values.map { config -> Themed(item, config) }
}
