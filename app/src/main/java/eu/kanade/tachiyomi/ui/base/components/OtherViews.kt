package eu.kanade.tachiyomi.ui.base.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.base.components.theme.Shapes
import eu.kanade.tachiyomi.ui.base.components.theme.Typefaces

@Composable
fun HeaderCard(text: String) {
    Card(shape = RoundedCornerShape(Shapes.coverRadius),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 8.dp,
        backgroundColor = MaterialTheme.colorScheme.secondary) {
        Text(
            text = text,
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            fontFamily = Typefaces.montserrat,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(all = 12.dp)
        )
    }
}

@Preview
@Composable
private fun HeaderCardPreview() {
    HeaderCard(text = "My Test header")
}

@Composable
fun Loading(isLoading: Boolean, modifier: Modifier = Modifier) {
    if (isLoading) {
        Box(modifier = modifier
            .size(40.dp)
            .background(color = MaterialTheme.colorScheme.secondary, shape = CircleShape)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(28.dp)
                    .padding(2.dp)
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSecondary,
                strokeWidth = 2.dp
            )
        }
    }
}

@Preview
@Composable
private fun loading() {
    Loading(isLoading = true)
}
