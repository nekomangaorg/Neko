package org.nekomanga.presentation.components.theme

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.nekomanga.presentation.theme.Themes

@Preview
@Composable
private fun PreviewThemeItem() {
    Surface() { ThemeItem(theme = Themes.Pink, isDarkTheme = false, selected = false) {} }
}
