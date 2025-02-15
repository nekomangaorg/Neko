package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.theme.Size

@Composable
fun PreferenceGroupHeader(title: UiText) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.fillMaxWidth().padding(bottom = Size.small, top = Size.medium),
    ) {
        Text(
            text = title.asString(),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = Size.medium),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
