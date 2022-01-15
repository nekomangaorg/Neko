package eu.kanade.tachiyomi.ui.base.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
        backgroundColor = MaterialTheme.colors.secondary) {
        Text(
            text = text,
            fontSize = MaterialTheme.typography.h6.fontSize,
            fontFamily = Typefaces.montserrat,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSecondary,
            modifier = Modifier.padding(all = 4.dp)
        )
    }
}

@Preview
@Composable
fun HeaderCardPreview() {
    HeaderCard(text = "My Test header")
}