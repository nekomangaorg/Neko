package org.nekomanga.presentation.screens.feed.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.ui.feed.FeedManga
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.R
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.screens.feed.FeedChapterTitleLine
import org.nekomanga.presentation.screens.feed.FeedCover
import org.nekomanga.presentation.screens.feed.getReadTextColor
import org.nekomanga.presentation.screens.feed.history.LastReadLine
import org.nekomanga.presentation.theme.Size

@Composable
fun ContinueReadingCard(
    modifier: Modifier = Modifier,
    feedManga: FeedManga,
    outlineCover: Boolean,
    mangaClick: () -> Unit,
    chapterClick: (Long) -> Unit,
    deleteAllHistoryClick: () -> Unit,
    deleteHistoryClick: (SimpleChapter) -> Unit,
) {
    val mediumAlphaColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)

    val chapterItem = feedManga.chapters.first()

    val titleColor = getReadTextColor(isRead = chapterItem.chapter.read)
    val updatedColor = getReadTextColor(isRead = chapterItem.chapter.read, mediumAlphaColor)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { chapterClick(chapterItem.chapter.id) }
                .padding(vertical = Size.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Gap(Size.small)
        FeedCover(
            artwork = feedManga.artwork,
            outlined = outlineCover,
            coverSize = Size.extraHuge,
            shoulderOverlayCover = chapterItem.chapter.read,
            onClick = mangaClick,
        )
        Column(modifier = Modifier.padding(horizontal = Size.small).weight(3f)) {
            Text(
                text = feedManga.mangaTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FeedChapterTitleLine(
                isBookmarked = chapterItem.chapter.bookmark,
                language = chapterItem.chapter.language,
                title = chapterItem.chapter.name,
                style = MaterialTheme.typography.bodyMedium,
                textColor = updatedColor,
            )
            LastReadLine(
                lastRead = chapterItem.chapter.lastRead,
                hasPagesLeft = !chapterItem.chapter.read && chapterItem.chapter.pagesLeft > 0,
                pagesLeft = chapterItem.chapter.pagesLeft,
                lastReadPreviousChapter = feedManga.lastReadChapter,
                style = MaterialTheme.typography.bodyMedium,
                textColor = updatedColor,
            )
        }
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
                                                UiText.String(chapterItem.chapter.name),
                                                onClick = {
                                                    deleteHistoryClick(chapterItem.chapter)
                                                },
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
    }
}
