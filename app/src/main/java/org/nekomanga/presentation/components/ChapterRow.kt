package org.nekomanga.presentation.components

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.merged.suwayomi.Suwayomi
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
import me.saket.swipe.SwipeAction
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.util.launchDelayed
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu
import org.nekomanga.presentation.extensions.surfaceColorAtElevationCustomColor
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun ChapterRow(
    themeColor: ThemeColorState,
    title: String,
    scanlator: String,
    uploader: String,
    language: String?,
    chapterNumber: Double,
    dateUploaded: Long,
    lastPageRead: Int,
    pagesLeft: Int,
    read: Boolean,
    bookmark: Boolean,
    isMerged: Boolean,
    isLocal: Boolean,
    isUnavailable: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    shouldHideChapterTitles: Boolean = false,
    onClick: () -> Unit,
    onBookmark: () -> Unit,
    onWebView: () -> Unit,
    onComment: () -> Unit,
    onRead: () -> Unit,
    blockScanlator: (String?, String?) -> Unit,
    markPrevious: (Boolean) -> Unit,
    onDownload: (DownloadAction) -> Unit,
) {
    CompositionLocalProvider(LocalRippleConfiguration provides themeColor.rippleConfiguration) {
        val (readIcon, readText) =
            when (read) {
                true -> Icons.Default.VisibilityOff to R.string.mark_as_unread
                false -> Icons.Default.Visibility to R.string.mark_as_read
            }

        val (bookmarkIcon, bookmarkText) =
            when (bookmark) {
                true -> Icons.Default.BookmarkRemove to R.string.remove_bookmark
                false -> Icons.Default.BookmarkAdd to R.string.add_bookmark
            }

        val markReadSwipeAction =
            SwipeAction(
                icon = {
                    SwipeIcon(
                        icon = readIcon,
                        contentColor = themeColor.buttonColor,
                        text = stringResource(readText),
                    )
                },
                background =
                    MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(
                        themeColor.buttonColor,
                        Size.small,
                    ),
                onSwipe = onRead,
            )

        val markBookmarkAction =
            SwipeAction(
                icon = {
                    SwipeIcon(
                        icon = bookmarkIcon,
                        contentColor = themeColor.buttonColor,
                        text = stringResource(bookmarkText),
                    )
                },
                background =
                    MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(
                        themeColor.buttonColor,
                        Size.small,
                    ),
                onSwipe = onBookmark,
            )

        ChapterSwipe(
            startSwipeActions = listOf(markBookmarkAction),
            endSwipeActions = listOf(markReadSwipeAction),
            backgroundInitialSwipeColor =
                MaterialTheme.colorScheme.surfaceColorAtElevationCustomColor(
                    themeColor.buttonColor,
                    Size.medium,
                ),
        ) {
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
                onComment = onComment,
                onDownload = onDownload,
                markPrevious = markPrevious,
                isMerged = isMerged,
                isLocal = isLocal,
                isUnavailable = isUnavailable,
                blockScanlator = blockScanlator,
                uploader = uploader,
            )
        }
    }
}

@Composable
private fun SwipeIcon(icon: ImageVector, text: String, contentColor: Color) {
    Box(Modifier.padding(horizontal = Size.medium)) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(text = text, textAlign = TextAlign.Center, color = contentColor)
        }
    }
}

@Composable
private fun ChapterInfo(
    themeColorState: ThemeColorState,
    shouldHideChapterTitles: Boolean,
    title: String,
    scanlator: String,
    uploader: String,
    language: String?,
    chapterNumber: Double,
    dateUploaded: Long,
    lastPageRead: Int,
    pagesLeft: Int,
    read: Boolean,
    bookmark: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    onClick: () -> Unit,
    onWebView: () -> Unit,
    onComment: () -> Unit,
    onDownload: (DownloadAction) -> Unit,
    markPrevious: (Boolean) -> Unit,
    isMerged: Boolean = false,
    isLocal: Boolean = false,
    isUnavailable: Boolean,
    blockScanlator: (String?, String?) -> Unit,
) {
    var dropdown by remember { mutableStateOf(false) }

    val downloadState = remember(downloadStateProvider()) { downloadStateProvider() }
    val downloadProgress = remember(downloadProgressProvider()) { downloadProgressProvider() }

    val splitScanlator = remember {
        val scanlators = ChapterUtil.getScanlators(scanlator).toMutableList()
        when (scanlators.first()) {
            Suwayomi.name -> scanlators.subList(0, 2).clear()
            in SourceManager.mergeSourceNames -> scanlators.removeAt(0)
        }

        scanlators
            .map { if (it == "No Group") null to uploader else it to null }
            .filterNot { it == null to null || it == null to "" }
            .map { (scanlator, uploader) ->
                SimpleDropDownItem.Action(
                    text = UiText.String(scanlator ?: uploader!!),
                    onClick = { blockScanlator(scanlator, uploader) },
                )
            }
            .toImmutableList()
            .apply { TimberKt.d { this.toString() } }
    }

    val haptic = LocalHapticFeedback.current

    val lowContrast =
        MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast)
    val (textColor, secondaryTextColor) =
        when (read) {
            true -> lowContrast to lowContrast
            false ->
                MaterialTheme.colorScheme.onSurface to
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = NekoColors.mediumAlphaLowContrast
                    )
        }

    val rowColor =
        when (dropdown) {
            true ->
                themeColorState.rippleConfiguration.color.copy(
                    alpha = themeColorState.rippleConfiguration.rippleAlpha!!.focusedAlpha
                )
            false -> MaterialTheme.colorScheme.surface
        }

    SimpleDropdownMenu(
        expanded = dropdown,
        themeColorState = themeColorState,
        onDismiss = { dropdown = false },
        dropDownItems =
            getDropDownItems(
                isLocal = isLocal,
                showScanlator = scanlator.isNotBlank() && splitScanlator.isNotEmpty() && !isLocal,
                showComments = !isMerged && !isLocal,
                scanlators = splitScanlator,
                onWebView = onWebView,
                onComment = onComment,
                markPrevious = markPrevious,
            ),
    )

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(color = rowColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dropdown = !dropdown
                    },
                )
                .padding(start = Size.small, top = Size.small, bottom = Size.small),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.align(Alignment.CenterVertically).fillMaxWidth(.8f)) {
            val titleText =
                when (shouldHideChapterTitles) {
                    true ->
                        stringResource(id = R.string.chapter_, decimalFormat.format(chapterNumber))
                    false -> title
                }

            Row {
                if (bookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).align(Alignment.CenterVertically),
                        tint = themeColorState.buttonColor,
                    )
                    Gap(Size.tiny)
                }
                Text(
                    text = titleText,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-.6).sp,
                        ),
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
                    resources.getQuantityString(R.plurals.pages_left, pagesLeft, pagesLeft)
                )
            } else if (showPagesLeft) {
                statuses.add(stringResource(id = R.string.page_, lastPageRead + 1))
            }

            if (scanlator.isNotBlank()) {
                if (scanlator == Constants.NO_GROUP) {
                    statuses.add(uploader)
                }
                statuses.add(scanlator)

                if (isMerged && uploader.isNotBlank()) {
                    statuses.add(uploader)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!language.isNullOrEmpty() && !language.equals(MdLang.ENGLISH.lang, true)) {
                    val iconRes = MdLang.fromIsoCode(language)?.iconResId

                    when (iconRes == null) {
                        true -> {
                            TimberKt.e { "Missing flag for $language" }
                            Text(
                                text = "$language ? ",
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = secondaryTextColor,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = (-.6).sp,
                                    ),
                            )
                        }
                        false -> {
                            val painter =
                                rememberDrawablePainter(
                                    drawable =
                                        AppCompatResources.getDrawable(
                                            LocalContext.current,
                                            iconRes,
                                        )
                                )
                            Image(
                                painter = painter,
                                modifier =
                                    Modifier.height(Size.medium)
                                        .clip(RoundedCornerShape(Size.tiny)),
                                contentDescription = "flag",
                            )
                            Gap(Size.tiny)
                        }
                    }
                }
                Text(
                    text = statuses.joinToString(Constants.SEPARATOR),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = secondaryTextColor,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-.6).sp,
                        ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                statuses.joinToString(Constants.SEPARATOR)
            }
        }
        val noLocalCopy = isUnavailable && downloadState != Download.State.DOWNLOADED
        val localCopy = isUnavailable && downloadState == Download.State.DOWNLOADED
        val unsupported = MdConstants.UnsupportedOfficialGroupList.contains(scanlator)

        var showLocalDropdown by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        when {
            noLocalCopy || unsupported -> {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier =
                        Modifier.align(Alignment.CenterVertically)
                            .padding(Size.smedium)
                            .size(Size.large),
                    tint = themeColorState.buttonColor,
                )
            }
            localCopy -> {
                Box(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center,
                ) {
                    SimpleDropdownMenu(
                        expanded = showLocalDropdown,
                        themeColorState = themeColorState,
                        onDismiss = { showLocalDropdown = false },
                        dropDownItems =
                            persistentListOf(
                                SimpleDropDownItem.Action(
                                    text = UiText.StringResource(R.string.remove),
                                    onClick = {
                                        scope.launchDelayed { onDownload(DownloadAction.Remove) }
                                    },
                                )
                            ),
                    )
                    Box(
                        modifier =
                            Modifier.clip(CircleShape).size(Size.huge).clickable {
                                showLocalDropdown = !showLocalDropdown
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.requiredSize(24.dp),
                                tint = themeColorState.buttonColor,
                            )
                        }
                    }
                }
            }
            else -> {
                DownloadButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    themeColorState = themeColorState,
                    downloadState = downloadState,
                    downloadProgress = downloadProgress,
                    onDownload = onDownload,
                )
            }
        }
    }
}

@Composable
private fun getDropDownItems(
    isLocal: Boolean,
    showScanlator: Boolean,
    showComments: Boolean,
    scanlators: ImmutableList<SimpleDropDownItem>,
    onWebView: () -> Unit,
    onComment: () -> Unit,
    markPrevious: (Boolean) -> Unit,
): ImmutableList<SimpleDropDownItem> {
    return (getList(
            !isLocal,
            listOf(
                SimpleDropDownItem.Action(
                    text = UiText.StringResource(R.string.open_in_webview),
                    onClick = { onWebView() },
                )
            ),
        ) +
            listOf(
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
            ) +
            getList(
                showScanlator,
                listOf(
                    SimpleDropDownItem.Parent(
                        text = UiText.StringResource(R.string.block_scanlator),
                        children = scanlators,
                    )
                ),
            ) +
            getList(
                showComments,
                listOf(
                    SimpleDropDownItem.Action(
                        text = UiText.StringResource(R.string.comments),
                        onClick = onComment,
                    )
                ),
            ))
        .toPersistentList()
}

private fun getList(
    booleanCondition: Boolean,
    trueList: List<SimpleDropDownItem>,
): List<SimpleDropDownItem> {
    return if (booleanCondition) {
        trueList
    } else {
        emptyList()
    }
}

val decimalFormat = DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })
