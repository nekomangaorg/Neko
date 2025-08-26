package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Divider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
        modifier = modifier,
    )
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = Modifier.fillMaxHeight().width(1.dp).then(modifier),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    )
}
