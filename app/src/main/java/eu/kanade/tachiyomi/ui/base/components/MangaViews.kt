package eu.kanade.tachiyomi.ui.base.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import coil.transform.RoundedCornersTransformation
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.zedlabs.pastelplaceholder.Pastel
import eu.kanade.tachiyomi.data.database.models.Manga

@Composable
fun MangaCover(manga: Manga, shouldOutlineCover: Boolean, modifier: Modifier = Modifier) {
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
        Image(
            asset = CommunityMaterial.Icon2.cmd_heart,
            colorFilter = ColorFilter.tint(MaterialTheme.colors.secondary),
            modifier = Modifier
                .size(24.dp)
                .align(alignment = Alignment.TopStart)
                .offset(x = (-4).dp, y = (-4).dp)
        )
    }
}

@Preview
@Composable
private fun MangaCoverPreview() {
    MangaCover(manga = Manga.create("test",
        "Title",
        1L).apply { favorite = true }, true)
}