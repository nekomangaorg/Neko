package org.nekomanga.presentation.screens.feed.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.ui.feed.FeedManga
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.screens.feed.FeedChapterTitleLine
import org.nekomanga.presentation.screens.feed.FeedCover
import org.nekomanga.presentation.screens.feed.getReadTextColor
import org.nekomanga.presentation.theme.Size

@Composable
fun HistoryCard(
    modifier: Modifier = Modifier,
    feedManga: FeedManga,
    outlineCard: Boolean,
    outlineCover: Boolean,
    groupedBySeries: Boolean,
    mangaClick: () -> Unit,
    chapterClick: (Long) -> Unit,
    deleteAllHistoryClick: () -> Unit,
    deleteHistoryClick: (SimpleChapter) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val canExpand by remember { mutableStateOf(feedManga.chapters.size > 1) }
    val allChaptersRead = feedManga.chapters.all { it.chapter.read }
    val cardColor: Color by
        animateColorAsState(
            if (expanded) MaterialTheme.colorScheme.surfaceColorAtElevation(Size.small)
            else MaterialTheme.colorScheme.surfaceColorAtElevation(Size.extraTiny),
            label = "historyCardExpansion",
        )
    val lowContrastColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = Size.small).animateContentSize(),
        outlineCard = outlineCard,
        cardColor = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        val titleColor =
            getReadTextColor(isRead = allChaptersRead, MaterialTheme.colorScheme.onSurfaceVariant)

        Row {
            Column(modifier = Modifier.weight(2f)) {
                Gap(Size.tiny)
                Text(
                    text = feedManga.mangaTitle,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = titleColor,
                    modifier = Modifier.fillMaxWidth().padding(start = Size.small),
                )
                Gap(Size.tiny)
                HistoryRow(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { chapterClick(feedManga.chapters.first().chapter.id) }
                            .padding(start = Size.small),
                    artwork = feedManga.artwork,
                    chapterItem = feedManga.chapters.first(),
                    outlineCovers = outlineCover,
                    mangaClick = mangaClick,
                )
            }
            DeleteExpandColumn(
                canExpand = canExpand,
                isExpanded = expanded,
                expandClick = { expanded = !expanded },
                chapterName = feedManga.chapters.first().chapter.name,
                deleteHistoryClick = { deleteHistoryClick(feedManga.chapters.first().chapter) },
                deleteAllHistoryClick = deleteAllHistoryClick,
            )
        }

        if (expanded) {
            if (groupedBySeries) {
                Gap(Size.small)
                Text(
                    modifier = Modifier.padding(start = Size.small).fillMaxWidth(),
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
                Gap(Size.small)
                Divider(Modifier.padding(horizontal = Size.small))
            }
            feedManga.chapters.forEachIndexed { index, chapterItem ->
                if (index != 0) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth().padding(vertical = Size.small).clickable {
                                chapterClick(chapterItem.chapter.id)
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier =
                                    Modifier.weight(1f).fillMaxWidth().padding(start = Size.small)
                            ) {
                                val textColor =
                                    getReadTextColor(
                                        isRead = chapterItem.chapter.read,
                                        lowContrastColor,
                                    )
                                FeedChapterTitleLine(
                                    isBookmarked = chapterItem.chapter.bookmark,
                                    language = chapterItem.chapter.language,
                                    title = chapterItem.chapter.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textColor = textColor,
                                )

                                ScanlatorLine(
                                    scanlator = chapterItem.chapter.scanlator,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textColor = textColor,
                                )

                                LastReadLine(
                                    lastRead = chapterItem.chapter.lastRead,
                                    hasPagesLeft =
                                        !chapterItem.chapter.read &&
                                            chapterItem.chapter.pagesLeft > 0,
                                    pagesLeft = chapterItem.chapter.pagesLeft,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textColor = textColor,
                                )
                            }

                            Buttons(
                                chapterName = chapterItem.chapter.name,
                                deleteHistoryClick = { deleteHistoryClick(chapterItem.chapter) },
                                deleteAllHistoryClick = deleteAllHistoryClick,
                            )
                        }
                    }
                }
            }
        } else {
            Gap(Size.small)
        }
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    outlineCard: Boolean,
    cardColor: CardColors,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (outlineCard) {
        true -> {
            OutlinedCard(modifier = modifier, colors = cardColor, content = content)
        }
        false -> {
            ElevatedCard(modifier = modifier, colors = cardColor, content = content)
        }
    }
}

@Composable
private fun HistoryRow(
    modifier: Modifier = Modifier,
    artwork: Artwork,
    chapterItem: ChapterItem,
    outlineCovers: Boolean,
    mangaClick: () -> Unit,
) {
    val mediumAlphaColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        FeedCover(
            artwork = artwork,
            outlined = outlineCovers,
            coverSize = Size.squareHistoryCover,
            onClick = mangaClick,
        )
        ChapterInfo(
            modifier = Modifier.padding(horizontal = Size.small).align(Alignment.Top),
            chapterItem = chapterItem,
            updatedColor = mediumAlphaColor,
        )
    }
}

@Composable
fun DeleteExpandColumn(
    modifier: Modifier = Modifier,
    canExpand: Boolean,
    isExpanded: Boolean,
    chapterName: String,
    deleteHistoryClick: () -> Unit,
    deleteAllHistoryClick: () -> Unit,
    expandClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Buttons(
            canExpand = canExpand,
            isExpanded = isExpanded,
            expandClick = expandClick,
            chapterName = chapterName,
            deleteHistoryClick = deleteHistoryClick,
            deleteAllHistoryClick = deleteAllHistoryClick,
        )
    }
}

@Composable
private fun ChapterInfo(
    modifier: Modifier = Modifier,
    chapterItem: ChapterItem,
    updatedColor: Color,
) {
    Column(modifier = modifier) {
        val textColor = getReadTextColor(isRead = chapterItem.chapter.read)

        FeedChapterTitleLine(
            language = chapterItem.chapter.language,
            isBookmarked = chapterItem.chapter.bookmark,
            title = chapterItem.chapter.name,
            style = MaterialTheme.typography.bodyLarge,
            textColor = textColor,
        )
        val readColor = getReadTextColor(isRead = chapterItem.chapter.read, updatedColor)
        ScanlatorLine(
            scanlator = chapterItem.chapter.scanlator,
            style = MaterialTheme.typography.bodyMedium,
            textColor = readColor,
        )

        LastReadLine(
            lastRead = chapterItem.chapter.lastRead,
            hasPagesLeft = !chapterItem.chapter.read && chapterItem.chapter.pagesLeft > 0,
            pagesLeft = chapterItem.chapter.pagesLeft,
            style = MaterialTheme.typography.bodyMedium,
            textColor = readColor,
        )
    }
}

@Composable
private fun Buttons(
    canExpand: Boolean = false,
    isExpanded: Boolean = false,
    chapterName: String,
    expandClick: () -> Unit = {},
    deleteHistoryClick: () -> Unit,
    deleteAllHistoryClick: () -> Unit,
) {
    var dropdown by remember { mutableStateOf(false) }

    IconButton(onClick = { dropdown = !dropdown }) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = .8f),
        )
        if (dropdown) {
            SimpleDropdownMenu(
                expanded = dropdown,
                themeColorState = defaultThemeColorState(),
                onDismiss = { dropdown = false },
                dropDownItems =
                    listOf(
                            SimpleDropDownItem.Parent(
                                UiText.StringResource(R.string.remove_history),
                                children =
                                    persistentListOf(
                                        SimpleDropDownItem.Action(
                                            UiText.String(chapterName),
                                            onClick = deleteHistoryClick,
                                        ),
                                        SimpleDropDownItem.Action(
                                            UiText.StringResource(R.string.entire_series),
                                            onClick = deleteAllHistoryClick,
                                        ),
                                    ),
                            )
                        )
                        .toPersistentList(),
            )
        }
    }
    if (canExpand) {
        IconButton(onClick = expandClick) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = .8f),
            )
        }
    }
}

@Composable
private fun ScanlatorLine(scanlator: String, textColor: Color, style: TextStyle) {
    if (scanlator.isNotBlank()) {
        Text(
            text = scanlator,
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
private fun LastReadLine(
    lastRead: Long,
    hasPagesLeft: Boolean,
    pagesLeft: Int,
    style: TextStyle,
    textColor: Color,
) {
    val statuses = mutableListOf<String>()

    statuses.add("Read ${lastRead.timeSpanFromNow}")

    if (hasPagesLeft) {
        statuses.add(pluralStringResource(R.plurals.pages_left, pagesLeft, pagesLeft))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
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
