package org.nekomanga.ui.theme

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import org.nekomanga.presentation.theme.NekoTheme
import org.nekomanga.presentation.theme.Themes
import org.nekomanga.presentation.theme.colorSchemeFromTheme

@Composable
private fun NekoThemePreview(theme: Themes, isDark: Boolean, content: @Composable () -> Unit) {
    NekoTheme(
        colorScheme =
            colorSchemeFromTheme(LocalContext.current, theme = theme, isSystemInDarkTheme = isDark)
    ) {
        Surface { content() }
    }
}

@Preview(name = "All Themes", showBackground = true, widthDp = 720) annotation class ThemePreviews

@Composable
fun ThemedPreviews(themeConfig: ThemeConfig, content: @Composable (theme: Themes) -> Unit) {
    NekoThemePreview(themeConfig.theme, themeConfig.isDark) { content(themeConfig.theme) }
}

data class ThemeConfig(val theme: Themes, val isDark: Boolean) {
    override fun toString(): String = "${theme.name} ${if (isDark) "Dark" else "Light"}"
}

data class Themed<T>(val value: T, val themeConfig: ThemeConfig) {
    override fun toString(): String = "$themeConfig - $value"
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
