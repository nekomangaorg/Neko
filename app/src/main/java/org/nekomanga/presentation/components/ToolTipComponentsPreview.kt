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
import org.nekomanga.ui.theme.ThemedPreviews

private data class ToolTipButtonData(
    val label: String,
    val isEnabled: Boolean,
    val icon: ImageVector,
)

private class ToolTipButtonProvider : PreviewParameterProvider<ToolTipButtonData> {
    override val values =
        sequenceOf(
            ToolTipButtonData("Enabled Action", true, Icons.Default.Favorite),
            ToolTipButtonData("Disabled Action", false, Icons.Default.Delete),
            ToolTipButtonData("Long Label Action", true, Icons.Default.Share),
        )
}

@Preview(name = "ToolTipButton Variants", showBackground = true, widthDp = 720)
@Composable
private fun ToolTipButtonPreview(
    @PreviewParameter(ToolTipButtonProvider::class) data: ToolTipButtonData
) {
    ThemedPreviews {
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
