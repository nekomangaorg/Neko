package org.nekomanga.presentation.screens.mangadetails

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gite
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.system.isInNightMode
import jp.wasabeef.gap.Gap

@Composable
fun ButtonBlock(manga: Manga, modifier: Modifier = Modifier) {

    val primaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val context = LocalContext.current
    val buttonColor = MaterialTheme.colorScheme.secondary //remember { getColor(primaryColor, surfaceColor, context) }

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        mainAxisAlignment = MainAxisAlignment.SpaceBetween, crossAxisSpacing = 8.dp,
        crossAxisAlignment = FlowCrossAxisAlignment.Center,
        mainAxisSize = SizeMode.Expand,

        ) {
        val (favIcon, favDescription) = when (manga.favorite) {
            true -> Icons.Filled.Favorite to R.string.remove_from_library
            false -> Icons.Outlined.Favorite to R.string.add_to_library
        }
        OutlineButton(favIcon, favDescription, color = buttonColor, text = "In Library")

        //Icons.Filled.AutoRenew for not tracked
        OutlineButton(favIcon = Icons.Filled.Check, favDescription = R.string.tracking, color = buttonColor, text = "3 tracked")

        OutlineButton(favIcon = Icons.Filled.AccountTree, favDescription = R.string.similar, color = buttonColor, text = "Similar")

        OutlineButton(favIcon = Icons.Filled.Gite, favDescription = R.string.merged, color = buttonColor, text = "Merge")

        OutlineButton(favIcon = Icons.Filled.OpenInBrowser, favDescription = R.string.open_external, color = buttonColor, text = "Links")

        OutlineButton(favIcon = Icons.Filled.Share, favDescription = R.string.share, color = buttonColor, text = stringResource(id = R.string.share))

    }
}

@Composable
private fun OutlineButton(
    favIcon: ImageVector,
    @StringRes favDescription: Int,
    color: Color = MaterialTheme.colorScheme.primary,
    shape: CornerBasedShape = RoundedCornerShape(50),
    text: String,
) {
    Row(
        modifier = Modifier
            .clip(shape)
            .border(width = 2.dp, color = color.copy(alpha = .5f), shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = favIcon, contentDescription = null, modifier = Modifier.size(24.dp), tint = color)
        if (text.isNotEmpty()) {
            Gap(width = 4.dp)
            Text(text = text, style = MaterialTheme.typography.bodyLarge.copy(color = color.copy(alpha = .8f), fontWeight = FontWeight.Medium))
        }
    }
}

private fun getColor(buttonColor: Color, surfaceColor: Color, context: Context): Color {
    val dominant = ColorUtils.blendARGB(buttonColor.toArgb(), surfaceColor.toArgb(), 0.5f)
    val domLum = ColorUtils.calculateLuminance(dominant)
    val lumWrongForTheme =
        (if (context.isInNightMode()) domLum > 0.8 else domLum <= 0.2)
    return Color(
        ColorUtils.blendARGB(
            dominant,
            surfaceColor.toArgb(),
            if (lumWrongForTheme) 0.9f else 0.7f,
        ),
    )
}
