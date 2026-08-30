package org.nekomanga.presentation.screens.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.components.sheets.BaseSheet
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ReaderChaptersSheet(
    chapters: List<ReaderChapterItem>,
    isChaptersEnabled: Boolean,
    isCommentsEnabled: Boolean,
    isWebViewEnabled: Boolean,
    isReadingModeEnabled: Boolean,
    isRotationEnabled: Boolean,
    isCropBordersEnabled: Boolean,
    isGrayscaleEnabled: Boolean,
    isDoublePageEnabled: Boolean,
    isShiftPageEnabled: Boolean,
    cropBorders: Boolean,
    grayscale: Boolean,
    readingModeIconRes: Int,
    rotationIconRes: Int,
    doublePageIconRes: Int,
    shiftPageIconRes: Int,
    onChapterClick: (ReaderChapterItem, Int) -> Unit,
    onBookmarkClick: (ReaderChapterItem) -> Unit,
    onCommentsClick: () -> Unit,
    onWebviewClick: () -> Unit,
    onReadingModeClick: () -> Unit,
    onRotationClick: () -> Unit,
    onCropBordersClick: () -> Unit,
    onGrayscaleClick: () -> Unit,
    onDoublePageClick: () -> Unit,
    onShiftPageClick: () -> Unit,
    onDisplayOptionsClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeColorState = defaultThemeColorState()
    val listState = rememberLazyListState()
    val maxLazyHeight = (LocalConfiguration.current.screenHeightDp * 0.6f).dp
    var loadingIndex by remember { mutableStateOf<Int?>(null) }
    val mangaDetailsPreferences = remember { Injekt.get<MangaDetailsPreferences>() }
    val hideChapterTitles =
        remember(chapters, mangaDetailsPreferences) {
            chapters.firstOrNull()?.manga?.hideChapterTitle(mangaDetailsPreferences) ?: false
        }

    // Scroll to the current reading chapter
    LaunchedEffect(chapters) {
        val currentIndex = chapters.indexOfFirst { it.isCurrent }
        if (currentIndex >= 0) {
            val scrollIndex = maxOf(0, currentIndex - 2)
            listState.scrollToItem(scrollIndex)
        }
    }

    BaseSheet(
        themeColor = themeColorState,
        maxSheetHeightPercentage = 0.9f,
        bottomPaddingAroundContent = Size.none,
    ) {
        Column(modifier = modifier.fillMaxWidth().padding(vertical = Size.small)) {
            // Drag handle pill is drawn by BaseSheet, so we build the header shortcuts row
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier =
                        Modifier.widthIn(min = maxWidth)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = Size.medium, vertical = Size.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (isChaptersEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(R.string.chapters),
                            painter = painterResource(R.drawable.ic_format_list_numbered_24dp),
                            enabledTint = MaterialTheme.colorScheme.primary,
                            onClick = onDismiss,
                        )
                    }

                    if (isCommentsEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(R.string.comments),
                            painter = painterResource(R.drawable.ic_view_comments_24p),
                            enabledTint = MaterialTheme.colorScheme.primary,
                            onClick = onCommentsClick,
                        )
                    }

                    if (isWebViewEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(R.string.open_in_webview),
                            painter = painterResource(R.drawable.ic_open_in_webview_24dp),
                            enabledTint = MaterialTheme.colorScheme.primary,
                            onClick = onWebviewClick,
                        )
                    }

                    if (isReadingModeEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(R.string.reading_mode),
                            painter = painterResource(readingModeIconRes),
                            enabledTint = MaterialTheme.colorScheme.primary,
                            onClick = onReadingModeClick,
                        )
                    }

                    if (isRotationEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(R.string.rotation),
                            painter = painterResource(rotationIconRes),
                            enabledTint = MaterialTheme.colorScheme.primary,
                            onClick = onRotationClick,
                        )
                    }

                    if (isCropBordersEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(R.string.crop_borders),
                            icon = if (cropBorders) Icons.Default.CropFree else Icons.Default.Crop,
                            enabledTint =
                                if (cropBorders) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                            onClick = onCropBordersClick,
                        )
                    }

                    if (isGrayscaleEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(R.string.grayscale_toggle),
                            painter = painterResource(R.drawable.ic_palette),
                            enabledTint =
                                if (grayscale) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                            onClick = onGrayscaleClick,
                        )
                    }

                    if (isDoublePageEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(R.string.double_pages),
                            painter = painterResource(doublePageIconRes),
                            enabledTint = MaterialTheme.colorScheme.primary,
                            onClick = onDoublePageClick,
                        )
                    }

                    if (isShiftPageEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(R.string.shift_one_page_over),
                            painter = painterResource(shiftPageIconRes),
                            enabledTint = MaterialTheme.colorScheme.primary,
                            onClick = onShiftPageClick,
                        )
                    }

                    ToolTipButton(
                        toolTipLabel = stringResource(R.string.display_options),
                        painter = painterResource(R.drawable.ic_tune_24dp),
                        enabledTint = MaterialTheme.colorScheme.primary,
                        onClick = onDisplayOptionsClick,
                    )
                }
            }

            HorizontalDivider()

            Box(modifier = Modifier.fillMaxWidth().heightIn(max = maxLazyHeight)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(
                        items = chapters,
                        key = { _, item -> item.chapter.id ?: 0L },
                    ) { index, item ->
                        val isLoading = loadingIndex == index
                        ChapterListItem(
                            item = item,
                            isLoading = isLoading,
                            hideChapterTitles = hideChapterTitles,
                            onClick = {
                                loadingIndex = index
                                onChapterClick(item, index)
                            },
                            onBookmarkClick = { onBookmarkClick(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterListItem(
    item: ReaderChapterItem,
    isLoading: Boolean,
    hideChapterTitles: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
) {
    val context = LocalContext.current
    val chapterColor =
        remember(item.chapter, item.isCurrent) {
            Color(ChapterUtil.chapterColor(context, item.chapter))
        }
    val bookmarkColor =
        remember(item.chapter) { Color(ChapterUtil.bookmarkColor(context, item.chapter)) }

    val fontStyle = if (item.isCurrent) FontStyle.Italic else FontStyle.Normal
    val fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Normal

    val titleText =
        remember(item.chapter, hideChapterTitles) {
            if (hideChapterTitles) {
                val decimalFormat =
                    DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })
                val number = decimalFormat.format(item.chapter_number.toDouble())
                context.getString(R.string.chapter_, number)
            } else {
                item.name
            }
        }

    val subtitleText =
        remember(item.chapter) {
            val statuses = mutableListOf<String>()
            ChapterUtil.relativeDate(item)?.let { statuses.add(it) }
            item.scanlator?.takeIf { it.isNotBlank() }?.let { statuses.add(it) }
            statuses.joinToString(Constants.SEPARATOR)
        }

    val hasLanguage =
        remember(item.chapter.language) {
            !item.chapter.language.isNullOrBlank() &&
                !item.chapter.language.equals("english", true) &&
                !item.chapter.language.equals("en", true)
        }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(enabled = !isLoading, onClick = onClick)
                .padding(horizontal = Size.medium, vertical = Size.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleText,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = fontWeight,
                        fontStyle = fontStyle,
                        color = chapterColor,
                    ),
            )
            Spacer(modifier = Modifier.height(Size.extraTiny))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasLanguage) {
                    Text(
                        text = item.chapter.language ?: "",
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontWeight = fontWeight,
                                fontStyle = fontStyle,
                                color = chapterColor,
                            ),
                        modifier = Modifier.padding(end = Size.small),
                    )
                }
                Text(
                    text = subtitleText,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontWeight = fontWeight,
                            fontStyle = fontStyle,
                            color = chapterColor,
                        ),
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Size.large),
                strokeWidth = Size.extraTiny,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    painter =
                        painterResource(
                            if (item.chapter.bookmark) R.drawable.ic_bookmark_24dp
                            else R.drawable.ic_bookmark_border_24dp
                        ),
                    contentDescription =
                        stringResource(
                            if (item.chapter.bookmark) R.string.bookmarked
                            else R.string.not_bookmarked
                        ),
                    tint = bookmarkColor,
                )
            }
        }
    }
}
