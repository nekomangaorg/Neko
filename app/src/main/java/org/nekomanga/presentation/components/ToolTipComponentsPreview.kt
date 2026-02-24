package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.nekomanga.ui.theme.Themed
import org.nekomanga.ui.theme.ThemedPreviews
import org.nekomanga.ui.theme.withThemes

private data class ToolTipButtonData(
    val label: String,
    val isEnabled: Boolean,
    val icon: ImageVector,
)

private class ToolTipButtonProvider : PreviewParameterProvider<Themed<ToolTipButtonData>> {
    override val values =
        sequenceOf(
                ToolTipButtonData("Enabled Action", true, Icons.Default.Favorite),
                ToolTipButtonData("Disabled Action", false, Icons.Default.Delete),
                ToolTipButtonData("Long Label Action", true, Icons.Default.Share),
            )
            .withThemes()
}

@Preview(name = "ToolTipButton Variants", showBackground = true)
@Composable
private fun ToolTipButtonPreview(
    @PreviewParameter(ToolTipButtonProvider::class) themedData: Themed<ToolTipButtonData>
) {
    ThemedPreviews(themedData.themeConfig) {
        val data = themedData.value
        Box(modifier = Modifier.padding(16.dp)) {
            ToolTipButton(
                toolTipLabel = data.label,
                isEnabled = data.isEnabled,
                icon = data.icon,
                onClick = {},
            )
        }
    }
}
