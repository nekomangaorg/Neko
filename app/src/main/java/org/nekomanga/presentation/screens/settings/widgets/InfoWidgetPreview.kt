package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.nekomanga.R
import org.nekomanga.presentation.theme.NekoTheme

@PreviewLightDark
@Composable
private fun InfoWidgetPreview() {
    NekoTheme { Surface { InfoWidget(text = stringResource(R.string.download_ahead_info)) } }
}
