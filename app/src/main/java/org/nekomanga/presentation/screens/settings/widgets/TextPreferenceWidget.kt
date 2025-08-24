package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.settings.BasePreferenceWidget
import org.nekomanga.presentation.theme.NekoTheme
import org.nekomanga.presentation.theme.Size

@Composable
fun TextPreferenceWidget(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    footer: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    widget: @Composable (() -> Unit)? = null,
    onPreferenceClick: (() -> Unit)? = null,
) {
    BasePreferenceWidget(
        modifier = modifier,
        title = title,
        subcomponent =
            if (!subtitle.isNullOrBlank() || !footer.isNullOrBlank()) {
                {
                    Column(Modifier.padding(horizontal = Size.medium)) {
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                modifier = Modifier.alpha(NekoColors.mediumAlphaLowContrast),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 10,
                            )
                        }
                        if (!footer.isNullOrBlank()) {
                            Text(
                                text = footer,
                                modifier = Modifier.alpha(NekoColors.disabledAlphaLowContrast),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                }
            } else {
                null
            },
        icon =
            if (icon != null) {
                { Icon(imageVector = icon, tint = iconTint, contentDescription = null) }
            } else {
                null
            },
        onClick = onPreferenceClick,
        widget = widget,
    )
}

@PreviewLightDark
@Composable
private fun TextPreferenceWidgetPreview() {
    NekoTheme {
        Surface {
            Column {
                TextPreferenceWidget(
                    title = "Text preference with icon",
                    subtitle = "Text preference summary",
                    icon = Icons.Filled.Preview,
                    onPreferenceClick = {},
                )
                TextPreferenceWidget(
                    title = "Text preference",
                    subtitle = "Text preference summary",
                    onPreferenceClick = {},
                )
            }
        }
    }
}
