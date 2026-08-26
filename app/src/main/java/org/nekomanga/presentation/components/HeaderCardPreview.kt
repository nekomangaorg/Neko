package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun HeaderCardPreview() {
    Box(modifier = Modifier.statusBarsPadding()) {
        var isExpanded by remember { mutableStateOf(true) }
        HeaderCard {
            DefaultHeaderText(
                text = "My Test Header",
                isExpanded = isExpanded,
                onClick = { isExpanded = !isExpanded },
            )
        }
    }
}
