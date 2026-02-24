package org.nekomanga.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.nekomanga.ui.theme.ThemeConfig
import org.nekomanga.ui.theme.ThemeConfigProvider
import org.nekomanga.ui.theme.ThemedPreviews

@Composable
fun LoadingScreen(showLoadingIcon: Boolean = true) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        if (showLoadingIcon) {
            ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview
@Composable
private fun LoadingScreenPreview(
    @PreviewParameter(ThemeConfigProvider::class) themeConfig: ThemeConfig
) {
    ThemedPreviews(themeConfig) { LoadingScreen() }
}
