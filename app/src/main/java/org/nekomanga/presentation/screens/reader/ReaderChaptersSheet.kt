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
import org.nekomanga.presentation.components.sheets.BaseSheet
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

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
    isWebtoon: Boolean,
    isPager: Boolean,
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
        bottomPaddingAroundContent = 0.dp,
    ) {
        Column(modifier = modifier.fillMaxWidth().padding(vertical = Size.small)) {
            // Drag handle pill is drawn by BaseSheet, so we build the header shortcuts row
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Size.medium, vertical = Size.small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                if (isChaptersEnabled) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_format_list_numbered_24dp),
                            contentDescription = stringResource(R.string.chapters),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (isCommentsEnabled) {
                    IconButton(onClick = onCommentsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_view_comments_24p),
                            contentDescription = stringResource(R.string.comments),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (isWebViewEnabled) {
                    IconButton(onClick = onWebviewClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_open_in_webview_24dp),
                            contentDescription = stringResource(R.string.open_in_webview),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (isReadingModeEnabled) {
                    IconButton(onClick = onReadingModeClick) {
                        Icon(
                            painter = painterResource(readingModeIconRes),
                            contentDescription = stringResource(R.string.reading_mode),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (isPager && isRotationEnabled) {
                    IconButton(onClick = onRotationClick) {
                        Icon(
                            painter = painterResource(rotationIconRes),
                            contentDescription = stringResource(R.string.rotation),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if ((isPager || isWebtoon) && isCropBordersEnabled) {
                    IconButton(onClick = onCropBordersClick) {
                        Icon(
                            imageVector =
                                if (cropBorders) Icons.Default.CropFree else Icons.Default.Crop,
                            contentDescription = stringResource(R.string.crop_borders),
                            tint =
                                if (cropBorders) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                if (isGrayscaleEnabled) {
                    IconButton(onClick = onGrayscaleClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_palette),
                            contentDescription = stringResource(R.string.grayscale_toggle),
                            tint =
                                if (grayscale) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                if (isPager && isDoublePageEnabled) {
                    IconButton(onClick = onDoublePageClick) {
                        Icon(
                            painter = painterResource(doublePageIconRes),
                            contentDescription = stringResource(R.string.double_pages),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (isShiftPageEnabled) {
                    IconButton(onClick = onShiftPageClick) {
                        Icon(
                            painter = painterResource(shiftPageIconRes),
                            contentDescription = stringResource(R.string.shift_one_page_over),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                IconButton(onClick = onDisplayOptionsClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tune_24dp),
                        contentDescription = stringResource(R.string.display_options),
                        tint = MaterialTheme.colorScheme.primary,
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

    val manga = item.manga
    val titleText =
        remember(item.chapter) {
            if (manga.hideChapterTitle(item.mangaDetailsPreferences)) {
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
            Spacer(modifier = Modifier.height(2.dp))
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
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
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
                    contentDescription = stringResource(R.string.bookmarked),
                    tint = bookmarkColor,
                )
            }
        }
    }
}
