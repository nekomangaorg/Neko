package org.nekomanga.presentation.screens.feed

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crazylegend.string.isNotNullOrEmpty
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.recents.FeedScreenType
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.decimalFormat
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedRow(
    modifier: Modifier = Modifier,
    artwork: Artwork,
    firstChapter: SimpleChapter,
    buttonColor: Color,
    outlineCovers: Boolean,
    feedScreenType: FeedScreenType,
    hideChapterTitles: Boolean,
    canExpand: Boolean,
    isExpanded: Boolean,
    mangaClick: () -> Unit,
) {
    val updatedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Size.tiny)) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Cover(artwork = artwork, outlined = outlineCovers, modifier = Modifier.align(Alignment.CenterVertically), onClick = mangaClick)
            Gap(Size.small)
            ChapterInfo(
                modifier = Modifier
                    .fillMaxWidth(.8f)
                    .padding(Size.tiny),
                feedScreenType = feedScreenType,
                firstChapter = firstChapter,
                hideChapterTitles = hideChapterTitles,
                updatedColor = updatedColor,
                canExpand = canExpand,
                isExpanded = isExpanded,
            )
            Gap(Size.small)
            Buttons(modifier = Modifier.weight(1f), buttonColor = buttonColor)
        }
    }
}

@Composable
private fun Cover(artwork: Artwork, outlined: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Shapes.coverRadius))
            .background(Color.Magenta)
            .clickable { onClick() },
    ) {
        MangaCover.Square.invoke(
            artwork = artwork,
            shouldOutlineCover = outlined,
            modifier = Modifier
                .size(Size.squareCover),
        )
    }
}

@Composable
private fun ChapterInfo(
    modifier: Modifier = Modifier,
    feedScreenType: FeedScreenType,
    firstChapter: SimpleChapter,
    hideChapterTitles: Boolean,
    updatedColor: Color,
    canExpand: Boolean,
    isExpanded: Boolean,
) {
    Column(modifier.height(Size.squareCover)) {
        val textColor = getTextColor(isRead = firstChapter.read)

        ChapterTitleLine(
            hideChapterTitles = hideChapterTitles,
            isBookmarked = firstChapter.bookmark,
            chapterNumber = firstChapter.chapterNumber,
            title = firstChapter.name,
            style = MaterialTheme.typography.bodyLarge,
            textColor = textColor,
        )

        Row {
            when (feedScreenType) {
                FeedScreenType.History -> {
                    Column {
                        val readColor = getTextColor(isRead = firstChapter.read, updatedColor)
                        LastReadLine(
                            lastRead = firstChapter.lastRead,
                            scanlator = firstChapter.scanlator,
                            language = firstChapter.language,
                            style = MaterialTheme.typography.bodyMedium,
                            textColor = readColor,
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (!firstChapter.read && firstChapter.pagesLeft > 0) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    text = pluralStringResource(id = R.plurals.pages_left, count = 1, firstChapter.pagesLeft),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = readColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                FeedScreenType.Updates -> {
                    Text(
                        text = "Updated ${firstChapter.dateUpload.timeSpanFromNow}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = updatedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (canExpand) {
            Spacer(modifier.weight(1f))
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
            )
        }

    }
}

@Composable
private fun Buttons(modifier: Modifier = Modifier, buttonColor: Color) {
    Column(modifier) {

        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = buttonColor.copy(alpha = .8f),
            )
        }
        DownloadButton(
            buttonColor,
            Download.State.NOT_DOWNLOADED,
            0f,
            Modifier
                .combinedClickable(
                    onClick = {
                        when (Download.State.NOT_DOWNLOADED) {
                            Download.State.NOT_DOWNLOADED -> Unit //onDownload(MangaConstants.DownloadAction.Download)
                            else -> Unit // chapterDropdown = true
                        }
                    },
                    onLongClick = {},
                ),
        )
    }
}

@Composable
private fun ChapterTitleLine(hideChapterTitles: Boolean, isBookmarked: Boolean, chapterNumber: Float, title: String, style: TextStyle, textColor: Color) {
    val titleText = when (hideChapterTitles) {
        true -> stringResource(id = R.string.chapter_, decimalFormat.format(chapterNumber))
        false -> title
    }
    Row {
        if (isBookmarked) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier
                    .size(Size.medium)
                    .align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.primary,
            )
            Gap(4.dp)
        }
        Text(
            text = titleText,
            style = style.copy(color = textColor, fontWeight = FontWeight.Medium, letterSpacing = (-.6).sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun getTextColor(isRead: Boolean, defaultColor: Color = LocalContentColor.current): Color {
    return when (isRead) {
        true -> LocalContentColor.current.copy(alpha = NekoColors.disabledAlphaLowContrast)
        false -> defaultColor
    }
}

@Composable
private fun LastReadLine(lastRead: Long, scanlator: String, language: String, style: TextStyle, textColor: Color) {
    val statuses = mutableListOf<String>()

    statuses.add("Read ${lastRead.timeSpanFromNow}")

    if (scanlator.isNotBlank()) {
        statuses.add(scanlator)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (language.isNotNullOrEmpty() && !language.equals("en", true)) {
            val iconRes = MdLang.fromIsoCode(language)?.iconResId

            when (iconRes == null) {
                true -> {
                    TimberKt.e { "Missing flag for $language" }
                    Text(
                        text = "$language • ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-.6).sp,
                        ),
                    )
                }

                false -> {
                    val painter = rememberDrawablePainter(drawable = AppCompatResources.getDrawable(LocalContext.current, iconRes))
                    Image(
                        painter = painter,
                        modifier = Modifier
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentDescription = "flag",
                    )
                    Gap(4.dp)
                }
            }
        }
        Text(
            text = statuses.joinToString(" • "),
            style = style.copy(
                color = textColor,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-.6).sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
