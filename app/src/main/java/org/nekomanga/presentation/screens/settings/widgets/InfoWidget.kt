package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.NekoTheme
import org.nekomanga.presentation.theme.Size

@Composable
internal fun InfoWidget(text: String) {
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(Size.medium).alpha(NekoColors.mediumAlphaHighContrast),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
        Gap(Size.small)
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@PreviewLightDark
@Composable
private fun InfoWidgetPreview() {
    NekoTheme { Surface { InfoWidget(text = stringResource(R.string.download_ahead_info)) } }
}
