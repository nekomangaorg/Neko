package org.nekomanga.presentation.components

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissState
import androidx.compose.material.DismissValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.rememberDismissState
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crazylegend.string.isNotNullOrEmpty
import com.elvishew.xlog.XLog
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DownloadAction
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun ChapterRow(
    themeColor: ThemeColorState,
    title: String,
    scanlator: String,
    language: String?,
    chapterNumber: Double,
    dateUploaded: Long,
    lastPageRead: Int,
    pagesLeft: Int,
    read: Boolean,
    bookmark: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Float,
    shouldHideChapterTitles: Boolean = false,
    onClick: () -> Unit,
    onBookmark: () -> Unit,
    onWebView: () -> Unit,
    onRead: () -> Unit,
    blockScanlator: (String) -> Unit,
    markPrevious: (Boolean) -> Unit,
    onDownload: (DownloadAction) -> Unit,
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColor.rippleTheme) {
        val dismissState = rememberDismissState(initialValue = DismissValue.Default)
        NekoSwipeToDismiss(
            state = dismissState,
            modifier = Modifier
                .padding(vertical = Dp(1f)),
            background = {
                val color = when (dismissState.dismissDirection) {
                    null -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(themeColor.buttonColor, 8.dp)
                }

                when (dismissState.dismissDirection) {
                    DismissDirection.EndToStart -> {
                        val (icon, text) = when (read) {
                            true -> Icons.Default.VisibilityOff to R.string.mark_as_unread
                            false -> Icons.Default.Visibility to R.string.mark_as_read
                        }
                        Background(icon, Alignment.CenterEnd, color, stringResource(id = text), themeColor.buttonColor)
                    }
                    DismissDirection.StartToEnd -> {
                        val (icon, text) = when (bookmark) {
                            true -> Icons.Default.BookmarkRemove to R.string.remove_bookmark
                            false -> Icons.Default.BookmarkAdd to R.string.add_bookmark
                        }
                        Background(icon, Alignment.CenterStart, color, stringResource(id = text), themeColor.buttonColor)
                    }
                    else -> Unit
                }
            },
            dismissContent = {
                ChapterInfo(
                    themeColorState = themeColor,
                    shouldHideChapterTitles = shouldHideChapterTitles,
                    title = title,
                    scanlator = scanlator,
                    language = language,
                    chapterNumber = chapterNumber,
                    dateUploaded = dateUploaded,
                    lastPageRead = lastPageRead,
                    pagesLeft = pagesLeft,
                    read = read,
                    bookmark = bookmark,
                    downloadStateProvider = downloadStateProvider,
                    downloadProgressProvider = downloadProgressProvider,
                    onClick = onClick,
                    onWebView = onWebView,
                    onDownload = onDownload,
                    markPrevious = markPrevious,
                    blockScanlator = blockScanlator,
                )
            },
        )
        when {
            dismissState.isDismissed(DismissDirection.EndToStart) -> Reset(dismissState = dismissState, action = onRead)
            dismissState.isDismissed(DismissDirection.StartToEnd) -> Reset(dismissState = dismissState, action = onBookmark)
        }
    }
}

@Composable
private fun Reset(dismissState: DismissState, action: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(key1 = dismissState.dismissDirection) {
        scope.launch {
            dismissState.reset()
            action()
        }
    }
}

@Composable
private fun Background(icon: ImageVector, alignment: Alignment, color: Color, text: String, contentColor: Color) {
    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = Dp(20f)),
        contentAlignment = alignment,
    ) {
        Column(modifier = Modifier.align(alignment)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = text,
                textAlign = TextAlign.Center,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun ChapterInfo(
    themeColorState: ThemeColorState,
    shouldHideChapterTitles: Boolean,
    title: String,
    scanlator: String,
    language: String?,
    chapterNumber: Double,
    dateUploaded: Long,
    lastPageRead: Int,
    pagesLeft: Int,
    read: Boolean,
    bookmark: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Float,
    onClick: () -> Unit,
    onWebView: () -> Unit,
    onDownload: (DownloadAction) -> Unit,
    markPrevious: (Boolean) -> Unit,
    blockScanlator: (String) -> Unit,
) {
    var dropdown by remember { mutableStateOf(false) }
    var chapterDropdown by remember { mutableStateOf(false) }

    val splitScanlator = remember {
        ChapterUtil.getScanlators(scanlator).map {
            SimpleDropDownItem.Action(
                text = it,
                onClick = { blockScanlator(it) },
            )
        }.toImmutableList()
    }

    val haptic = LocalHapticFeedback.current

    val lowContrast = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast)
    val (textColor, secondaryTextColor) = when (read) {
        true -> lowContrast to lowContrast
        false -> MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    }

    val rowColor = when (dropdown) {
        true -> themeColorState.rippleTheme.defaultColor().copy(alpha = themeColorState.rippleTheme.rippleAlpha().focusedAlpha)
        false -> MaterialTheme.colorScheme.surface
    }

    SimpleDropdownMenu(
        expanded = dropdown,
        themeColorState = themeColorState,
        onDismiss = { dropdown = false },
        dropDownItems = getDropDownItems(scanlator.isNotBlank(), splitScanlator, onWebView, markPrevious),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = rowColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dropdown = !dropdown
                },
            )
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth(.8f),
        ) {
            val titleText = when (shouldHideChapterTitles) {
                true -> stringResource(id = R.string.chapter_, decimalFormat.format(chapterNumber))
                false -> title
            }

            Row {
                if (bookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.CenterVertically),
                        tint = themeColorState.buttonColor,
                    )
                    Gap(4.dp)
                }
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyLarge.copy(color = textColor, fontWeight = FontWeight.Medium, letterSpacing = (-.6).sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val statuses = mutableListOf<String>()

            ChapterUtil.relativeDate(dateUploaded)?.let { statuses.add(it) }

            val showPagesLeft = !read && lastPageRead > 0
            val resources = LocalContext.current.resources

            if (showPagesLeft && pagesLeft > 0) {
                statuses.add(
                    resources.getQuantityString(R.plurals.pages_left, pagesLeft, pagesLeft),
                )
            } else if (showPagesLeft) {
                statuses.add(
                    stringResource(id = R.string.page_, lastPageRead + 1),
                )
            }

            if (scanlator.isNotBlank()) {
                statuses.add(scanlator)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (language.isNotNullOrEmpty() && language.equals("en", true).not()) {
                    val iconRes = MdLang.fromIsoCode(language!!)?.iconResId

                    when (iconRes == null) {
                        true -> {
                            XLog.e("Missing flag for $language")
                            Text(
                                text = "$language • ",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = secondaryTextColor,
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
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = secondaryTextColor,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-.6).sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                statuses.joinToString(" • ")
            }
        }
        Box(modifier = Modifier.align(Alignment.CenterVertically), contentAlignment = Alignment.Center) {
            DownloadButton(
                themeColorState.buttonColor,
                downloadStateProvider,
                downloadProgressProvider,
                Modifier
                    .combinedClickable(
                        onClick = {
                            when (downloadStateProvider()) {
                                Download.State.NOT_DOWNLOADED -> onDownload(DownloadAction.Download)
                                else -> chapterDropdown = true
                            }
                        },
                        onLongClick = {},
                    ),
            )
            SimpleDropdownMenu(
                expanded = chapterDropdown,
                themeColorState = themeColorState,
                onDismiss = { chapterDropdown = false },
                dropDownItems =
                when (downloadStateProvider()) {
                    Download.State.DOWNLOADED -> {
                        persistentListOf(
                            SimpleDropDownItem.Action(
                                text = stringResource(R.string.remove),
                                onClick = {
                                    onDownload(DownloadAction.Remove)
                                },
                            ),
                        )
                    }
                    else -> {
                        persistentListOf(
                            SimpleDropDownItem.Action(
                                text = stringResource(R.string.start_downloading_now),
                                onClick = {
                                    onDownload(DownloadAction.ImmediateDownload)
                                },
                            ),
                            SimpleDropDownItem.Action(
                                text = stringResource(R.string.cancel),
                                onClick = {
                                    onDownload(DownloadAction.Cancel)
                                },
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun getDropDownItems(
    showScanlator: Boolean,
    scanlators: ImmutableList<SimpleDropDownItem>,
    onWebView: () -> Unit,
    markPrevious: (Boolean) -> Unit,
): ImmutableList<SimpleDropDownItem> {
    return (
        listOf(
            SimpleDropDownItem.Action(
                text = stringResource(R.string.open_in_webview),
                onClick = { onWebView() },
            ),
            SimpleDropDownItem.Parent(
                text = stringResource(R.string.mark_previous_as),
                children = listOf(
                    SimpleDropDownItem.Action(
                        text = stringResource(R.string.read),
                        onClick = { markPrevious(true) },
                    ),
                    SimpleDropDownItem.Action(
                        text = stringResource(R.string.unread),
                        onClick = { markPrevious(false) },
                    ),
                ),
            ),
        ) +
            if (showScanlator) {
                listOf(
                    SimpleDropDownItem.Parent(
                        text = stringResource(R.string.block_scanlator),
                        children = scanlators,
                    ),
                )
            } else {
                emptyList()
            }
        ).toPersistentList()
}

val decimalFormat = DecimalFormat(
    "#.###",
    DecimalFormatSymbols()
        .apply { decimalSeparator = '.' },
)
