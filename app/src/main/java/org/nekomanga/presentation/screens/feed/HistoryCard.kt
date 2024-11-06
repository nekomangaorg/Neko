package org.nekomanga.presentation.screens.feed

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.recents.FeedManga
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.dialog.DeleteHistoryDialog
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun HistoryCard(
    feedManga: FeedManga,
    themeColorState: ThemeColorState,
    outlineCovers: Boolean,
    hideChapterTitles: Boolean,
    groupedBySeries: Boolean,
    downloadClick: (ChapterItem, MangaConstants.DownloadAction) -> Unit,
    mangaClick: () -> Unit,
    chapterClick: (Long) -> Unit,
    deleteAllHistoryClick: () -> Unit,
    deleteHistoryClick: (SimpleChapter) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val canExpand by remember { mutableStateOf(feedManga.chapters.size > 1) }
    val cardColor: Color by
        animateColorAsState(
            if (expanded) MaterialTheme.colorScheme.surfaceColorAtElevation(Size.small)
            else MaterialTheme.colorScheme.primaryContainer,
            label = "historyCardExpansion",
        )
    val lowContrastColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    var showRemoveHistoryDialog by remember { mutableIntStateOf(-1) }
    var showRemoveAllHistoryDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = mangaClick,
        modifier = Modifier.fillMaxWidth().padding(Size.small).animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        val titleColor =
            getReadTextColor(
                isRead = feedManga.chapters.all { it.chapter.read },
                themeColorState.buttonColor,
            )

        Text(
            text = feedManga.mangaTitle,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = titleColor,
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = Size.small, vertical = Size.tiny),
        )

        HistoryRow(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable { chapterClick(feedManga.chapters.first().chapter.id) }
                    .padding(start = Size.small),
            artwork = feedManga.artwork,
            chapterItem = feedManga.chapters.first(),
            themeColorState = themeColorState,
            outlineCovers = outlineCovers,
            hideChapterTitles = hideChapterTitles,
            canExpand = canExpand,
            isExpanded = expanded,
            mangaClick = mangaClick,
            expandClick = { expanded = !expanded },
            deleteAllClick = { showRemoveAllHistoryDialog = true },
            deleteClick = { showRemoveHistoryDialog = 0 },
            downloadClick = { action -> downloadClick(feedManga.chapters.first(), action) },
        )
        if (showRemoveHistoryDialog >= 0) {
            DeleteHistoryDialog(
                themeColorState = themeColorState,
                onDismiss = { showRemoveHistoryDialog = -1 },
                name = feedManga.chapters[showRemoveHistoryDialog].chapter.name,
                title = R.string.remove_history_question,
                description = R.string.this_will_remove_the_read_date,
                onConfirm = {
                    deleteHistoryClick(feedManga.chapters[showRemoveHistoryDialog].chapter)
                },
            )
        }
        if (showRemoveAllHistoryDialog) {
            DeleteHistoryDialog(
                themeColorState = themeColorState,
                onDismiss = { showRemoveAllHistoryDialog = false },
                name = feedManga.mangaTitle,
                title = R.string.remove_all_history_question,
                description = R.string.this_will_remove_the_read_date_for_all,
                onConfirm = deleteAllHistoryClick,
            )
        }

        if (expanded) {
            if (groupedBySeries) {
                Text(
                    modifier =
                        Modifier.padding(start = Size.small, top = Size.medium).fillMaxWidth(),
                    text = stringResource(id = R.string.showing_x_most_recent),
                    textAlign = TextAlign.Center,
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            color =
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = NekoColors.mediumAlphaLowContrast
                                )
                        ),
                )
            }
            feedManga.chapters.forEachIndexed { index, chapterItem ->
                if (index == 0) {
                    Gap(Size.smedium)
                } else {
                    Divider(Modifier.padding(horizontal = Size.small))
                    Column(
                        modifier =
                            Modifier.fillMaxWidth().clickable {
                                chapterClick(chapterItem.chapter.id)
                            }
                    ) {
                        Gap(Size.smedium)
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium)) {
                            Column(
                                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(.75f)
                            ) {
                                FeedChapterTitleLine(
                                    hideChapterTitles = hideChapterTitles,
                                    isBookmarked = chapterItem.chapter.bookmark,
                                    chapterNumber = chapterItem.chapter.chapterNumber,
                                    title = chapterItem.chapter.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textColor = getReadTextColor(isRead = chapterItem.chapter.read),
                                )

                                LastReadLine(
                                    lastRead = chapterItem.chapter.lastRead,
                                    scanlator = chapterItem.chapter.scanlator,
                                    language = chapterItem.chapter.language,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textColor =
                                        getReadTextColor(
                                            isRead = chapterItem.chapter.read,
                                            lowContrastColor,
                                        ),
                                )
                                if (chapterItem.chapter.pagesLeft > 0) {
                                    PagesLeftLine(
                                        pagesLeft = chapterItem.chapter.pagesLeft,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textColor =
                                            getReadTextColor(
                                                isRead = chapterItem.chapter.read,
                                                lowContrastColor,
                                            ),
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier.align(Alignment.CenterEnd),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row() {
                                    Buttons(
                                        themeColorState = themeColorState,
                                        chapterItem = chapterItem,
                                        deleteClick = { showRemoveHistoryDialog = index },
                                        downloadClick = { action ->
                                            downloadClick(chapterItem, action)
                                        },
                                    )
                                }
                            }
                        }
                        Gap(Size.smedium)
                    }
                }
            }
            Gap(Size.medium)
        }
    }
}

@Composable
private fun HistoryRow(
    modifier: Modifier = Modifier,
    artwork: Artwork,
    chapterItem: ChapterItem,
    themeColorState: ThemeColorState,
    outlineCovers: Boolean,
    hideChapterTitles: Boolean,
    canExpand: Boolean,
    isExpanded: Boolean,
    expandClick: () -> Unit,
    mangaClick: () -> Unit,
    deleteAllClick: () -> Unit,
    deleteClick: () -> Unit,
    downloadClick: (MangaConstants.DownloadAction) -> Unit,
) {
    val mediumAlphaColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Size.tiny)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FeedCover(
                artwork = artwork,
                outlined = outlineCovers,
                coverSize = Size.squareCover,
                onClick = mangaClick,
            )
            ChapterInfo(
                modifier =
                    Modifier.padding(vertical = Size.small, horizontal = Size.small)
                        .align(Alignment.Top)
                        .height(Size.squareCover)
                        .weight(3f),
                chapterItem = chapterItem,
                hideChapterTitles = hideChapterTitles,
                updatedColor = mediumAlphaColor,
                themeColorState = themeColorState,
                canExpand = canExpand,
                isExpanded = isExpanded,
                expandClick = expandClick,
                deleteAllClick = deleteAllClick,
                deleteClick = deleteClick,
                downloadClick = downloadClick,
            )
        }
    }
}

@Composable
private fun ChapterInfo(
    modifier: Modifier = Modifier,
    chapterItem: ChapterItem,
    hideChapterTitles: Boolean,
    updatedColor: Color,
    themeColorState: ThemeColorState,
    canExpand: Boolean,
    isExpanded: Boolean,
    expandClick: () -> Unit,
    deleteAllClick: () -> Unit,
    deleteClick: () -> Unit,
    downloadClick: (MangaConstants.DownloadAction) -> Unit,
) {
    Column(modifier = modifier) {
        val textColor = getReadTextColor(isRead = chapterItem.chapter.read)

        FeedChapterTitleLine(
            hideChapterTitles = hideChapterTitles,
            isBookmarked = chapterItem.chapter.bookmark,
            chapterNumber = chapterItem.chapter.chapterNumber,
            title = chapterItem.chapter.name,
            style = MaterialTheme.typography.bodyLarge,
            textColor = textColor,
        )

        val readColor = getReadTextColor(isRead = chapterItem.chapter.read, updatedColor)
        LastReadLine(
            lastRead = chapterItem.chapter.lastRead,
            scanlator = chapterItem.chapter.scanlator,
            language = chapterItem.chapter.language,
            style = MaterialTheme.typography.bodyMedium,
            textColor = readColor,
        )

        Spacer(modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!chapterItem.chapter.read && chapterItem.chapter.pagesLeft > 0) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.pages_left,
                            chapterItem.chapter.pagesLeft,
                            chapterItem.chapter.pagesLeft,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = readColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (canExpand) {
                Spacer(modifier.weight(1f))
                TextButton(onClick = expandClick) {
                    Icon(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        imageVector =
                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                    )
                }
            }
            Spacer(modifier.weight(1f))
            Buttons(
                themeColorState = themeColorState,
                chapterItem = chapterItem,
                deleteAll = true,
                deleteAllClick = deleteAllClick,
                deleteClick = deleteClick,
                downloadClick = downloadClick,
            )
        }
    }
}

@Composable
private fun RowScope.Buttons(
    themeColorState: ThemeColorState,
    chapterItem: ChapterItem,
    deleteAll: Boolean = false,
    deleteClick: () -> Unit,
    deleteAllClick: () -> Unit = {},
    downloadClick: (MangaConstants.DownloadAction) -> Unit,
) {
    if (deleteAll) {
        IconButton(onClick = deleteAllClick) {
            Icon(
                imageVector = Icons.Outlined.DeleteSweep,
                contentDescription = null,
                tint = themeColorState.buttonColor.copy(alpha = .8f),
            )
        }
    }
    IconButton(onClick = deleteClick) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            tint = themeColorState.buttonColor.copy(alpha = .8f),
        )
    }
    DownloadButton(
        themeColorState = themeColorState,
        downloadState = chapterItem.downloadState,
        downloadProgress = chapterItem.downloadProgress,
        onDownload = downloadClick,
    )
}

@Composable
private fun LastReadLine(
    lastRead: Long,
    scanlator: String,
    language: String,
    style: TextStyle,
    textColor: Color,
) {
    val statuses = mutableListOf<String>()

    statuses.add("Read ${lastRead.timeSpanFromNow}")

    if (scanlator.isNotBlank()) {
        statuses.add(scanlator)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (language.isNotEmpty() && !language.equals("en", true)) {
            val iconRes = MdLang.fromIsoCode(language)?.iconResId

            when (iconRes == null) {
                true -> {
                    TimberKt.e { "Missing flag for $language" }
                    Text(
                        text = "$language${Constants.SEPARATOR}",
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = textColor,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-.6).sp,
                            ),
                    )
                }
                false -> {
                    val painter =
                        rememberDrawablePainter(
                            drawable = AppCompatResources.getDrawable(LocalContext.current, iconRes)
                        )
                    Image(
                        painter = painter,
                        modifier = Modifier.height(16.dp).clip(RoundedCornerShape(4.dp)),
                        contentDescription = "flag",
                    )
                    Gap(4.dp)
                }
            }
        }
        Text(
            text = statuses.joinToString(Constants.SEPARATOR),
            style =
                style.copy(
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-.6).sp,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PagesLeftLine(pagesLeft: Int, style: TextStyle, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = pluralStringResource(id = R.plurals.pages_left, count = pagesLeft),
            style =
                style.copy(
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-.6).sp,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
