package eu.kanade.tachiyomi.ui.base.components

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.ui.base.components.theme.Typefaces

@Composable
internal fun MangaTitle(
    title: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    fontSize: TextUnit = MaterialTheme.typography.bodyMedium.fontSize,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = title,
        style = TextStyle(fontFamily = Typefaces.montserrat,
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = fontWeight),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
internal fun DisplayText(
    displayText: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = MaterialTheme.typography.bodyMedium.fontSize,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = displayText,
        style = TextStyle(fontFamily = Typefaces.montserrat,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = MaterialTheme.colorScheme.onSurface.copy(.6f)),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
internal fun Favorited(offset: Dp) {
    Image(
        asset = CommunityMaterial.Icon2.cmd_heart,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
        modifier = Modifier
            .size(24.dp)
            .offset(x = offset, y = offset)
    )
}
