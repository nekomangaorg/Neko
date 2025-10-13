package org.nekomanga.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
fun ThemedPreviews(content: @Composable (theme: Themes) -> Unit) {
    val themes = Themes.values()

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        for (theme in themes) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    NekoThemePreview(theme = theme, isDark = false) {
                        Column {
                            Text(
                                text = "${theme.name} Light",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            content(theme)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    NekoThemePreview(theme = theme, isDark = true) {
                        Column {
                            Text(
                                text = "${theme.name} Dark",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            content(theme)
                        }
                    }
                }
            }
        }
    }
}
