package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Typefaces

@Composable
fun HeaderCard(text: String) {
    Card(
        shape = RoundedCornerShape(Shapes.coverRadius),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 8.dp,
        backgroundColor = MaterialTheme.colorScheme.secondary,
    ) {
        Text(
            text = text,
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            fontFamily = Typefaces.montserrat,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(all = 12.dp),
        )
    }
}

@Preview
@Composable
private fun HeaderCardPreview() {
    HeaderCard(text = "My Test header")
}
