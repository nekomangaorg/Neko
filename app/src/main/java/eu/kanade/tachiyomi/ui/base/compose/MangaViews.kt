package eu.kanade.tachiyomi.ui.base.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.database.models.Manga

@Composable
fun MangaCover(manga: Manga, modifier: Modifier, shouldOutlineCover: Boolean) {
    Box {

        val outlineModifier = when (shouldOutlineCover) {
            true -> Modifier.border(.75.dp, NekoColors.outline,
                RoundedCornerShape(12.dp))
            else -> Modifier
        }

        Image(painter = rememberImagePainter(
            data = manga,
            builder = {
                placeholder(Pastel.getColorLight())
                transformations(RoundedCornersTransformation(0f))

            }),
            contentDescription = null,
            modifier = modifier
                .size(64.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(outlineModifier)

        )
        if (manga.favorite) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(shape = CircleShape)
                    .background(color = MaterialTheme.colors.secondary)
                    .then(outlineModifier)
                    .align(alignment = Alignment.TopStart)
                    .padding(4.dp),
            )
        }
    }
}