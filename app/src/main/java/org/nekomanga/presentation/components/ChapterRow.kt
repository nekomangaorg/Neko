package org.nekomanga.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import me.saket.swipe.SwipeAction
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

/**
 * Data class holding all the necessary information for a chapter row. Using a data class simplifies
 * the function signature and improves maintainability.
 */
@Immutable
data class ChapterRowData(
    val title: String,
    val scanlator: String,
    val uploader: String,
    val language: String?,
    val chapterNumber: Double,
    val dateUploaded: Long,
    val lastPageRead: Int,
    val pagesLeft: Int,
    val read: Boolean,
    val bookmark: Boolean,
    val isMerged: Boolean,
    val isLocal: Boolean,
    val isUnavailable: Boolean,
    val downloadState: Download.State,
    val downloadProgress: Int,
)

@Composable
fun ChapterRow(
    themeColor: ThemeColorState,
    data: ChapterRowData,
    shouldHideChapterTitles: Boolean = false,
    onClick: () -> Unit,
    onBookmark: () -> Unit,
    onRead: () -> Unit,
    onWebView: () -> Unit,
    onComment: () -> Unit,
    onDownload: (MangaConstants.DownloadAction) -> Unit,
    blockScanlator: (MangaConstants.BlockType, String) -> Unit,
    markPrevious: (Boolean) -> Unit,
) {
    CompositionLocalProvider(LocalRippleConfiguration provides themeColor.rippleConfiguration) {
        val (readIcon, readTextRes) =
            if (data.read) Icons.Default.VisibilityOff to R.string.mark_as_unread
            else Icons.Default.Visibility to R.string.mark_as_read
        val (bookmarkIcon, bookmarkTextRes) =
            if (data.bookmark) Icons.Default.BookmarkRemove to R.string.remove_bookmark
            else Icons.Default.BookmarkAdd to R.string.add_bookmark

        val swipeActionBackgroundColor =
            MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(
                themeColor.primaryColor,
                Size.small,
            )

        val markReadSwipeAction =
            SwipeAction(
                icon = {
                    SwipeIcon(
                        icon = readIcon,
                        text = stringResource(readTextRes),
                        contentColor = themeColor.primaryColor,
                    )
                },
                background = swipeActionBackgroundColor,
                onSwipe = onRead,
            )

        val markBookmarkAction =
            SwipeAction(
                icon = {
                    SwipeIcon(
                        icon = bookmarkIcon,
                        text = stringResource(bookmarkTextRes),
                        contentColor = themeColor.primaryColor,
                    )
                },
                background = swipeActionBackgroundColor,
                onSwipe = onBookmark,
            )

        ChapterSwipe(
            startSwipeActions = listOf(markBookmarkAction),
            endSwipeActions = listOf(markReadSwipeAction),
        ) {
            ChapterRowContent(
                themeColorState = themeColor,
                shouldHideChapterTitles = shouldHideChapterTitles,
                data = data,
                onClick = onClick,
                onWebView = onWebView,
                onComment = onComment,
                onDownload = onDownload,
                markPrevious = markPrevious,
                blockScanlator = blockScanlator,
            )
        }
    }
}

@Composable
private fun ChapterRowContent(
    themeColorState: ThemeColorState,
    shouldHideChapterTitles: Boolean,
    data: ChapterRowData,
    onClick: () -> Unit,
    onWebView: () -> Unit,
    onComment: () -> Unit,
    onDownload: (MangaConstants.DownloadAction) -> Unit,
    markPrevious: (Boolean) -> Unit,
    blockScanlator: (MangaConstants.BlockType, String) -> Unit,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val textColor =
        remember(data.read) {
            if (data.read) onSurfaceColor.copy(alpha = NekoColors.disabledAlphaLowContrast)
            else onSurfaceColor
        }
    val secondaryTextColor =
        remember(data.read) {
            if (data.read) onSurfaceColor.copy(alpha = NekoColors.disabledAlphaLowContrast)
            else onSurfaceColor.copy(alpha = NekoColors.mediumAlphaLowContrast)
        }

    val dropdownItems =
        remember(data.isLocal, data.isMerged, data.scanlator, data.uploader) {
            buildChapterDropdownItems(
                isLocal = data.isLocal,
                isMerged = data.isMerged,
                scanlator = data.scanlator,
                uploader = data.uploader,
                onWebView = onWebView,
                onComment = onComment,
                markPrevious = markPrevious,
                blockScanlator = blockScanlator,
            )
        }

    SimpleDropdownMenu(
        expanded = isDropdownExpanded,
        themeColorState = themeColorState,
        onDismiss = { isDropdownExpanded = false },
        dropDownItems = dropdownItems,
    )

    themeColorState.rippleConfiguration

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    if (isDropdownExpanded) themeColorState.rippleColor.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isDropdownExpanded = true
                    },
                )
                .padding(start = Size.small, top = Size.small, bottom = Size.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            ChapterTitle(
                shouldHideChapterTitles = shouldHideChapterTitles,
                title = data.title,
                chapterNumber = data.chapterNumber,
                isBookmarked = data.bookmark,
                textColor = textColor,
                themeColor = themeColorState.primaryColor,
            )

            val subtitleText =
                remember(
                    data.dateUploaded,
                    data.read,
                    data.lastPageRead,
                    data.pagesLeft,
                    data.scanlator,
                    data.uploader,
                    data.isMerged,
                ) {
                    val statuses = mutableListOf<String>()
                    ChapterUtil.relativeDate(data.dateUploaded)?.let { statuses.add(it) }
                    if (!data.read && data.lastPageRead > 0) {
                        if (data.pagesLeft > 0) {
                            statuses.add(
                                context.resources.getQuantityString(
                                    R.plurals.pages_left,
                                    data.pagesLeft,
                                    data.pagesLeft,
                                )
                            )
                        } else {
                            statuses.add(context.getString(R.string.page_, data.lastPageRead + 1))
                        }
                    }
                    if (data.scanlator.isNotBlank()) {
                        if (data.scanlator == Constants.NO_GROUP) statuses.add(data.uploader)
                        statuses.add(data.scanlator)
                        if (data.isMerged && data.uploader.isNotBlank()) statuses.add(data.uploader)
                    }
                    statuses.joinToString(Constants.SEPARATOR)
                }

            ChapterSubtitle(
                subtitleText = subtitleText,
                language = data.language,
                textColor = secondaryTextColor,
            )
        }
        ChapterDownloadIndicator(
            isUnavailable = data.isUnavailable,
            scanlator = data.scanlator,
            downloadState = data.downloadState,
            downloadProgress = data.downloadProgress,
            onDownload = onDownload,
            themeColorState = themeColorState,
        )
    }
}

@Composable
private fun ChapterTitle(
    shouldHideChapterTitles: Boolean,
    title: String,
    chapterNumber: Double,
    isBookmarked: Boolean,
    textColor: Color,
    themeColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isBookmarked) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = themeColor,
            )
            Gap(Size.tiny)
        }
        Text(
            text =
                if (shouldHideChapterTitles)
                    stringResource(id = R.string.chapter_, decimalFormat.format(chapterNumber))
                else title,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChapterSubtitle(subtitleText: String, language: String?, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        language?.let { LanguageIcon(language = it, textColor = textColor) }
        Text(
            text = subtitleText,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LanguageIcon(language: String, textColor: Color) {
    if (language.equals(MdLang.ENGLISH.lang, true)) return

    val iconRes = remember(language) { MdLang.fromIsoCode(language)?.iconResId }

    if (iconRes != null) {
        Image(
            painter = painterResource(id = iconRes),
            modifier = Modifier.height(Size.medium).clip(RoundedCornerShape(Size.tiny)),
            contentDescription = "Language flag",
        )
        Spacer(modifier = Modifier.size(Size.tiny))
    } else {
        Text(
            text = "$language?",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                ),
        )
        Spacer(modifier = Modifier.size(Size.tiny))
    }
}

@Composable
private fun ChapterDownloadIndicator(
    isUnavailable: Boolean,
    scanlator: String,
    downloadState: Download.State,
    downloadProgress: Int,
    onDownload: (MangaConstants.DownloadAction) -> Unit,
    themeColorState: ThemeColorState,
) {
    val isLocked = isUnavailable || MdConstants.UnsupportedOfficialGroupList.contains(scanlator)
    val isDownloaded = downloadState == Download.State.DOWNLOADED

    when {
        isLocked && !isDownloaded -> {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = stringResource(id = R.string.unavailable),
                modifier = Modifier.padding(Size.smedium).size(Size.large),
                tint = themeColorState.primaryColor,
            )
        }
        isDownloaded -> {
            var showRemoveDropdown by remember { mutableStateOf(false) }
            DownloadButton(
                themeColorState = themeColorState,
                modifier = Modifier,
                downloadState = downloadState,
                downloadProgress = downloadProgress,
                onClick = { showRemoveDropdown = true },
            )
            SimpleDropdownMenu(
                themeColorState = themeColorState,
                expanded = showRemoveDropdown,
                onDismiss = { showRemoveDropdown = false },
                dropDownItems =
                    persistentListOf(
                        SimpleDropDownItem.Action(
                            text = UiText.StringResource(R.string.remove),
                            onClick = { onDownload(MangaConstants.DownloadAction.Remove) },
                        )
                    ),
            )
        }
        else -> {
            DownloadButton(
                themeColorState = themeColorState,
                modifier = Modifier,
                downloadState = downloadState,
                downloadProgress = downloadProgress,
                onClick = onDownload,
            )
        }
    }
}

private fun buildChapterDropdownItems(
    isLocal: Boolean,
    isMerged: Boolean,
    scanlator: String,
    uploader: String,
    onWebView: () -> Unit,
    onComment: () -> Unit,
    markPrevious: (Boolean) -> Unit,
    blockScanlator: (MangaConstants.BlockType, String) -> Unit,
): PersistentList<SimpleDropDownItem> {
    return buildList {
            if (!isLocal) {
                add(
                    SimpleDropDownItem.Action(
                        text = UiText.StringResource(R.string.open_in_webview),
                        onClick = onWebView,
                    )
                )
            }

            add(
                SimpleDropDownItem.Parent(
                    text = UiText.StringResource(R.string.mark_previous_as),
                    children =
                        listOf(
                            SimpleDropDownItem.Action(
                                text = UiText.StringResource(R.string.read),
                                onClick = { markPrevious(true) },
                            ),
                            SimpleDropDownItem.Action(
                                text = UiText.StringResource(R.string.unread),
                                onClick = { markPrevious(false) },
                            ),
                        ),
                )
            )

            val scanlatorItems =
                ChapterUtil.getScanlators(scanlator)
                    .mapNotNull { name ->
                        if (name == Constants.NO_GROUP) uploader.takeIf { it.isNotBlank() }
                        else name
                    }
                    .map { name ->
                        SimpleDropDownItem.Action(
                            text = UiText.String(name),
                            onClick = {
                                val type =
                                    if (name == uploader) MangaConstants.BlockType.Uploader
                                    else MangaConstants.BlockType.Group
                                blockScanlator(type, name)
                            },
                        )
                    }

            if (scanlatorItems.isNotEmpty() && !isLocal) {
                add(
                    SimpleDropDownItem.Parent(
                        text = UiText.StringResource(R.string.block_scanlator),
                        children = scanlatorItems,
                    )
                )
            }

            if (!isMerged && !isLocal) {
                add(
                    SimpleDropDownItem.Action(
                        text = UiText.StringResource(R.string.comments),
                        onClick = onComment,
                    )
                )
            }
        }
        .toPersistentList()
}

@Composable
private fun SwipeIcon(icon: ImageVector, text: String, contentColor: Color) {
    Box(Modifier.padding(horizontal = Size.medium)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Text(text = text, textAlign = TextAlign.Center, color = contentColor)
        }
    }
}

private val decimalFormat =
    DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })
