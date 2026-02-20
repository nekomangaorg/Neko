package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun HeaderCard(headerText: @Composable () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(Shapes.coverRadius),
        modifier = Modifier.fillMaxWidth().padding(Size.small),
        colors =
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondary),
    ) {
        headerText()
    }
}

@Composable
fun DefaultHeaderText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    )
}
