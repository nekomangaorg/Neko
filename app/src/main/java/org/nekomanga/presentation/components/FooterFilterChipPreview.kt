package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.nekomanga.presentation.theme.Size
import org.nekomanga.ui.theme.ThemeConfig
import org.nekomanga.ui.theme.ThemeConfigProvider
import org.nekomanga.ui.theme.ThemedPreviews

@Preview
@Composable
private fun FooterFilterChipPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) {
        Row(horizontalArrangement = Arrangement.spacedBy(Size.small)) {
            FooterFilterChip(selected = false, onClick = {}, name = "Unselected")
            FooterFilterChip(selected = true, onClick = {}, name = "Selected")
            FooterFilterChip(
                selected = false,
                onClick = {},
                name = "Icon",
                icon = Icons.Default.Check,
            )
            FooterFilterChip(
                selected = true,
                onClick = {},
                name = "Icon",
                icon = Icons.Default.Check,
            )
        }
    }
}
