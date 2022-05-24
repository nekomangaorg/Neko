package eu.kanade.tachiyomi.ui.manga.header

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crazylegend.string.isNotNullOrEmpty
import com.elvishew.xlog.XLog
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import com.google.android.material.composethemeadapter3.Mdc3Theme
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SManga
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.theme.Typefaces
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InformationHeader(
    manga: Manga,
    isExpanded: Boolean = true,
    titleLongClick: (String) -> Unit = {},
    creatorLongClicked: (String) -> Unit = {},

    ) {
    val style = TextStyle(
        fontFamily = Typefaces.montserrat,
        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
    )

    val noRippleInteraction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 8.dp),
    ) {
        if (manga.title.isNotNullOrEmpty()) {
            Text(
                text = manga.title,
                modifier = Modifier
                    .indication(indication = null, interactionSource = noRippleInteraction)
                    .combinedClickable(
                        interactionSource = noRippleInteraction,
                        indication = null,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            titleLongClick(manga.title)
                        },
                        onClick = {},
                    ),
                maxLines = if (isExpanded) Integer.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                style = style.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .9f),
                ),
            )
        }

        if (manga.author.isNotNullOrEmpty() || manga.artist.isNotNullOrEmpty()) {
            val creator =
                if (manga.author == manga.artist) {
                    manga.author ?: "".trim()
                } else {
                    listOfNotNull(manga.author?.trim(), manga.artist?.trim())
                        .joinToString(", ")
                }

            Gap(4.dp)
            Text(
                text = creator,
                modifier = Modifier
                    .animateContentSize()
                    .combinedClickable(
                        interactionSource = noRippleInteraction,
                        indication = null,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            creatorLongClicked(creator)
                        },
                        onClick = {},
                    ),
                maxLines = if (isExpanded) Integer.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                style = style,
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

            Text(
                text = stringResource(id = status),
                style = style,
            )
        }
        if (manga.rating != null || manga.users != null || manga.lang_flag != null) {
            Gap(8.dp)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            mainAxisAlignment = FlowMainAxisAlignment.Start,
        ) {
            if (manga.lang_flag != null) {
                val flag = when (manga.lang_flag?.lowercase(Locale.US)) {
                    "zh-hk" -> R.drawable.ic_flag_china
                    "zh" -> R.drawable.ic_flag_china
                    "ko" -> R.drawable.ic_flag_korea
                    "ja" -> R.drawable.ic_flag_japan
                    else -> null
                }
                if (flag != null) {
                    val drawable = AppCompatResources.getDrawable(LocalContext.current, flag)
                    androidx.compose.foundation.Image(
                        painter = rememberDrawablePainter(drawable = drawable),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentDescription = "flag",
                    )
                }
            }

            if (manga.rating != null) {
                Row {
                    Image(
                        asset = MaterialDesignDx.Icon.gmf_bar_chart,
                        colorFilter = ColorFilter.tint(style.color.copy(.65f)),
                    )
                    Text(
                        text = manga.rating!!,
                        style = style,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
            if (manga.users != null) {
                val users = kotlin.runCatching {
                    NumberFormat.getNumberInstance(Locale.US).format(manga.users?.toInt() ?: 0)
                }.getOrElse {
                    XLog.e("number couldnt be formatted for ${manga.url}")
                    0
                }.toString()
                Row {
                    Image(
                        asset = MaterialDesignDx.Icon.gmf_person,
                        colorFilter = ColorFilter.tint(style.color.copy(.65f)),
                    )
                    Text(text = users, style = style)
                }
            }
        }
        if (manga.missing_chapters != null) {
            Text(
                text = stringResource(R.string.missing_chapters, manga.missing_chapters!!),
                style = style,
            )
        }
    }
}

@Preview
@Composable
private fun InformationHeader() {
    val manga = Manga.create(1L).apply {
        title = "One Piece"
        author = "Eiichiro Oda"
        status = SManga.ONGOING
    }
    Mdc3Theme {
        Surface {
            InformationHeader(manga = manga)
        }
    }
}
