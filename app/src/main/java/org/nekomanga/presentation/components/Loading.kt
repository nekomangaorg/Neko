package org.nekomanga.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.theme.Size

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(40.dp)
                .background(color = MaterialTheme.colorScheme.secondary, shape = CircleShape),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp).padding(Size.extraTiny).align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSecondary,
            strokeWidth = Size.extraTiny,
        )
    }
}

@Preview
@Composable
private fun LoadingPreview() {
    Loading()
}
