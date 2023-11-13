package org.nekomanga.presentation.screens.feed

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crazylegend.string.isNotNullOrEmpty
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.recents.FeedManga
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.theme.Size

@Composable
fun HistoryCard(feedManga: FeedManga, buttonColor: Color, outlineCovers: Boolean, hideChapterTitles: Boolean, downloadClick: (Long) -> Unit, mangaClick: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val cardColor: Color by animateColorAsState(if (expanded) MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp) else MaterialTheme.colorScheme.surface)
    val lowContrastColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    val canExpand = feedManga.chapters.size > 1

    ElevatedCard(
        enabled = canExpand,
        onClick = {
            if (canExpand) {
                expanded = !expanded
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(Size.small)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        val titleColor = getReadTextColor(isRead = feedManga.chapters.all { it.read }, buttonColor)

        Text(
            text = feedManga.mangaTitle,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = titleColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Size.small, vertical = Size.tiny),
        )

        HistoryRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Size.small),
            artwork = feedManga.artwork,
            firstChapter = feedManga.chapters.first(),
            buttonColor = buttonColor,
            outlineCovers = outlineCovers,
            hideChapterTitles = hideChapterTitles,
            canExpand = canExpand,
            isExpanded = expanded,
            mangaClick = mangaClick,
        )


        if (expanded) {
            feedManga.chapters.forEachIndexed { index, simpleChapter ->
                if (index > 0) {
                    Divider(Modifier.padding(Size.small))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Size.small),
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(.8f),
                        ) {

                            FeedChapterTitleLine(
                                hideChapterTitles = hideChapterTitles,
                                isBookmarked = simpleChapter.bookmark,
                                chapterNumber = simpleChapter.chapterNumber,
                                title = simpleChapter.name,
                                style = MaterialTheme.typography.bodyMedium,
                                textColor = getReadTextColor(isRead = simpleChapter.read),
                            )

                            LastReadLine(
                                lastRead = simpleChapter.lastRead,
                                scanlator = simpleChapter.scanlator,
                                language = simpleChapter.language,
                                style = MaterialTheme.typography.bodyMedium,
                                textColor = getReadTextColor(isRead = simpleChapter.read, lowContrastColor),
                            )
                        }
                        Box(modifier = Modifier.align(Alignment.CenterEnd), contentAlignment = Alignment.Center) {
                            DownloadButton(
                                buttonColor,
                                Download.State.NOT_DOWNLOADED,
                                0f,
                                Modifier
                                    .combinedClickable(
                                        onClick = {
                                            when (Download.State.NOT_DOWNLOADED) {
                                                Download.State.NOT_DOWNLOADED -> Unit //onDownload(MangaConstants.DownloadAction.Download)
                                                else -> downloadClick(simpleChapter.id)
                                            }
                                        },
                                        onLongClick = {},
                                    ),
                            )
                        }

                    }
                }
            }
            Gap(Size.small)
        }
    }
}

@Composable
private fun HistoryRow(
    modifier: Modifier = Modifier,
    artwork: Artwork,
    firstChapter: SimpleChapter,
    buttonColor: Color,
    outlineCovers: Boolean,
    hideChapterTitles: Boolean,
    canExpand: Boolean,
    isExpanded: Boolean,
    mangaClick: () -> Unit,
) {
    val mediumAlphaColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Size.tiny)) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            FeedCover(artwork = artwork, outlined = outlineCovers, coverSize = Size.squareCover, onClick = mangaClick)
            Gap(Size.small)
            ChapterInfo(
                modifier = Modifier
                    .padding(horizontal = Size.small, vertical = Size.small)
                    .align(Alignment.Top)
                    .weight(3f),
                firstChapter = firstChapter,
                hideChapterTitles = hideChapterTitles,
                updatedColor = mediumAlphaColor,
                canExpand = canExpand,
                isExpanded = isExpanded,
            )
            Gap(Size.small)
            Buttons(buttonColor = buttonColor)
        }

    }
}

@Composable
private fun ChapterInfo(
    modifier: Modifier = Modifier,
    firstChapter: SimpleChapter,
    hideChapterTitles: Boolean,
    updatedColor: Color,
    canExpand: Boolean,
    isExpanded: Boolean,
) {
    Column(modifier = modifier) {
        val textColor = getReadTextColor(isRead = firstChapter.read)

        FeedChapterTitleLine(
            hideChapterTitles = hideChapterTitles,
            isBookmarked = firstChapter.bookmark,
            chapterNumber = firstChapter.chapterNumber,
            title = firstChapter.name,
            style = MaterialTheme.typography.bodyLarge,
            textColor = textColor,
        )

        val readColor = getReadTextColor(isRead = firstChapter.read, updatedColor)
        LastReadLine(
            lastRead = firstChapter.lastRead,
            scanlator = firstChapter.scanlator,
            language = firstChapter.language,
            style = MaterialTheme.typography.bodyMedium,
            textColor = readColor,
        )
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

