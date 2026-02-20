package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun HeaderCardPreview() {
    Box(modifier = Modifier.statusBarsPadding()) {
        HeaderCard { DefaultHeaderText(text = "My Test Header") }
    }
}
