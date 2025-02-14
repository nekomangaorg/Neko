package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.settings.IconItem

@Composable
fun SettingsReaderScreen(modifier: Modifier = Modifier, contentPadding: PaddingValues) {
    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxWidth()) {
        item {
            IconItem(labelText = UiText.String("Reader"), icon = Icons.Default.Info, onClick = {})
        }
    }
}
