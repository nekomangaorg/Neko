package org.nekomanga.presentation.screens.mangadetails

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.HotelClass
import androidx.compose.material.icons.outlined._18UpRating
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crazylegend.common.ifTrue
import com.crazylegend.string.isNotNullOrEmpty
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SManga
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.NoRippleText
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InformationBlock(
    manga: Manga,
    title: String,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    isMerged: Boolean = true,
    titleLongClick: (String) -> Unit = {},
    creatorLongClicked: (String) -> Unit = {},
) {

    val highAlpha = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.highAlphaLowContrast)
    val mediumAlpha = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
    ) {
        if (title.isNotNullOrEmpty()) {
            NoRippleText(
                text = title,
                maxLines = if (isExpanded) Integer.MAX_VALUE else 4,
                onLongClick = titleLongClick,
                style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = (-.5).sp, fontWeight = FontWeight.Medium),
                color = highAlpha,
            )
        }

        if (manga.author.isNotNullOrEmpty() || manga.artist.isNotNullOrEmpty()) {
            val creator =
                when (manga.author == manga.artist) {
                    true -> manga.author ?: "".trim()
                    false -> {
                        listOfNotNull(manga.author?.trim(), manga.artist?.trim())
                            .joinToString(", ")
                    }
                }

            Gap(4.dp)
            NoRippleText(
                text = creator,
                onLongClick = creatorLongClicked,
                maxLines = if (isExpanded) Integer.MAX_VALUE else 2,
                style = MaterialTheme.typography.bodyLarge,
                color = mediumAlpha,
            )
        }
        if (manga.status != 0) {
            Gap(4.dp)
            val status = when (manga.status) {
                SManga.ONGOING -> R.string.ongoing
                SManga.COMPLETED -> R.string.completed
                SManga.LICENSED -> R.string.licensed
                SManga.PUBLICATION_COMPLETE -> R.string.publication_complete
                SManga.HIATUS -> R.string.hiatus
                SManga.CANCELLED -> R.string.cancelled
                else -> R.string.unknown
            }

            NoRippleText(
                text = stringResource(id = status),
                style = MaterialTheme.typography.bodyLarge,
                color = mediumAlpha,
            )
        }
        if (manga.rating != null || manga.users != null || manga.lang_flag != null) {
            Gap(8.dp)
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth(),
            mainAxisAlignment = FlowMainAxisAlignment.Start,
            crossAxisAlignment = FlowCrossAxisAlignment.Center,

            ) {
            if (manga.lang_flag != null) {
                val flag = when (manga.lang_flag?.lowercase(Locale.US)) {
                    "zh-hk" -> R.drawable.ic_flag_hk
                    "zh" -> R.drawable.ic_flag_cn
                    "ko" -> R.drawable.ic_flag_kr
                    "ja" -> R.drawable.ic_flag_jp
                    "en" -> R.drawable.ic_flag_us
                    else -> null
                }
                if (flag != null) {
                    val drawable = AppCompatResources.getDrawable(LocalContext.current, flag)
                    Image(
                        painter = rememberDrawablePainter(drawable = drawable),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp)),
                        contentDescription = "flag",
                    )
                }
            }

            manga.genre?.contains("pornographic", true)?.ifTrue {
                Row {
                    Gap(8.dp)
                    Image(imageVector = Icons.Outlined._18UpRating, modifier = Modifier.size(32.dp), contentDescription = null, colorFilter = ColorFilter.tint(Color.Red))
                }
            }

            manga.rating?.let { rating ->
                val formattedRating = ((rating.toDouble() * 100).roundToInt() / 100.0).toString()

                Row {
                    Gap(8.dp)
                    Image(imageVector = Icons.Filled.HotelClass, contentDescription = null, colorFilter = ColorFilter.tint(mediumAlpha))
                    Gap(4.dp)
                    NoRippleText(
                        text = formattedRating,
                        style = MaterialTheme.typography.bodyLarge,
                        color = mediumAlpha,
                    )
                }
            }

            manga.users?.let { unformattedNumberOfUsers ->
                val numberOfUsers = runCatching {
                    NumberFormat.getNumberInstance(Locale.US).format(unformattedNumberOfUsers.toInt())
                }.getOrDefault(0).toString()

                Row {
                    Gap(8.dp)
                    Image(imageVector = Icons.Filled.Bookmarks, contentDescription = null, colorFilter = ColorFilter.tint(mediumAlpha))
                    Gap(4.dp)
                    NoRippleText(
                        text = numberOfUsers,
                        style = MaterialTheme.typography.bodyLarge,
                        color = mediumAlpha,
                    )
                }
            }

            if (isMerged) {
                Row {
                    Gap(8.dp)
                    com.mikepenz.iconics.compose.Image(asset = CommunityMaterial.Icon.cmd_check_decagram, colorFilter = ColorFilter.tint(mediumAlpha))
                }
            }

        }

        manga.missing_chapters?.let { numberOfMissingChapters ->
            Gap(4.dp)
            NoRippleText(
                text = stringResource(id = R.string.missing_chapters, numberOfMissingChapters), style = MaterialTheme.typography.bodyLarge,
                color = mediumAlpha,
            )
        }

    }
}

