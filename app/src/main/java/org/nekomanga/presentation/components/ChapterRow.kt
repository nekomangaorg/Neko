package org.nekomanga.presentation.components

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeToDismiss
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crazylegend.string.isNotNullOrEmpty
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import jp.wasabeef.gap.Gap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import onColor
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.presentation.screens.ThemeColorState
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

@Composable
fun ChapterRow(themeColor: ThemeColorState, chapterItem: ChapterItem, shouldHideChapterTitles: Boolean = false, onClick: () -> Unit, onBookmark: () -> Unit, onRead: () -> Unit) {
    val scope = rememberCoroutineScope()
    CompositionLocalProvider(LocalRippleTheme provides themeColor.rippleTheme) {
        val dismissState = rememberDismissState(initialValue = DismissValue.Default)
        SwipeToDismiss(
            state = dismissState,
            modifier = Modifier
                .padding(vertical = Dp(1f)),
            directions = setOf(
                DismissDirection.EndToStart,
                DismissDirection.StartToEnd,
            ),
            dismissThresholds = { FractionalThreshold(.1f) },
            background = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        DismissValue.Default -> MaterialTheme.colorScheme.surface
                        DismissValue.DismissedToStart -> themeColor.altContainerColor
                        DismissValue.DismissedToEnd -> themeColor.buttonColor
                    },
                )

                val scale by animateFloatAsState(
                    if (dismissState.targetValue == DismissValue.Default) 0f else 1.25f,
                )
                when (dismissState.dismissDirection) {
                    DismissDirection.EndToStart -> {
                        val icon = when (chapterItem.chapter.read) {
                            true -> Icons.Default.VisibilityOff
                            false -> Icons.Default.Visibility
                        }
                        Background(icon, null, Alignment.CenterEnd, color, scale, themeColor.altContainerColor.onColor())
                    }
                    DismissDirection.StartToEnd -> {
                        val icon = when (chapterItem.chapter.bookmark) {
                            true -> Icons.Default.BookmarkRemove
                            false -> Icons.Default.BookmarkAdd
                        }
                        Background(icon, null, Alignment.CenterStart, color, scale, MaterialTheme.colorScheme.surface)
                    }
                    else -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                        )
                    }
                }
            },
            dismissContent = {
                ChapterInfo(themeColor = themeColor, chapterItem = chapterItem, onClick = onClick, shouldHideChapterTitles = shouldHideChapterTitles)
            },
        )
        when {
            dismissState.isDismissed(DismissDirection.EndToStart) -> reset(scope = scope, dismissState = dismissState, action = onRead)
            dismissState.isDismissed(DismissDirection.StartToEnd) -> reset(scope = scope, dismissState = dismissState, action = onBookmark)

        }

    }
}

@Composable
private fun reset(scope: CoroutineScope, dismissState: DismissState, action: () -> Unit) {
    LaunchedEffect(key1 = dismissState.dismissDirection) {
        scope.launch {
            dismissState.reset()
            delay(100)
            action()
        }
    }
}

@Composable
private fun Background(icon: ImageVector, contentDescription: String?, alignment: Alignment, color: Color, scale: Float, iconColor: Color) {
    Box(
        Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = Dp(20f)),
        contentAlignment = alignment,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.scale(scale),
            tint = iconColor,
        )
    }
}

@Composable
private fun ChapterInfo(themeColor: ThemeColorState, chapterItem: ChapterItem, onClick: () -> Unit, shouldHideChapterTitles: Boolean) {
    val chapter = chapterItem.chapter
    val lowContrast = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast)
    val (textColor, secondaryTextColor) = when (chapter.read) {
        true -> lowContrast to lowContrast
        false -> MaterialTheme.colorScheme.onSurface to MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth(.8f),
        ) {

            val titleText = when (shouldHideChapterTitles) {
                true -> stringResource(id = R.string.chapter_, decimalFormat.format(chapter.chapterNumber.toDouble()))
                false -> chapter.name
            }

            Row {
                if (chapter.bookmark) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark, contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.CenterVertically),
                        tint = themeColor.buttonColor,
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

            ChapterUtil.relativeDate(chapter.dateUpload)?.let { statuses.add(it) }

            val showPagesLeft = !chapter.read && chapter.lastPageRead > 0
            val resources = LocalContext.current.resources

            if (showPagesLeft && chapter.pagesLeft > 0) {
                statuses.add(
                    resources.getQuantityString(R.plurals.pages_left, chapter.pagesLeft, chapter.pagesLeft),
                )
            } else if (showPagesLeft) {
                statuses.add(
                    stringResource(id = R.string.page_, chapter.lastPageRead + 1),
                )
            }

            if (chapter.scanlator.isNotBlank()) {
                statuses.add(chapter.scanlator)
            }

            Row {
                if (chapter.language.isNotNullOrEmpty() && chapter.language.equals("en", true).not()) {
                    val drawable = AppCompatResources.getDrawable(LocalContext.current, MdLang.fromIsoCode(chapter.language)?.iconResId!!)
                    Image(
                        painter = rememberDrawablePainter(drawable = drawable),
                        modifier = Modifier
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentDescription = "flag",
                    )
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

        DownloadButton(
            themeColor.buttonColor, chapterItem.downloadState, chapterItem.downloadProgress.toFloat(),
            Modifier
                .align(Alignment.CenterVertically)
                .combinedClickable(onClick = {}, onLongClick = {}),
        )
    }
}

@Composable
fun ChapterRowTester(buttonColor: Color, state: Download.State = Download.State.NOT_DOWNLOADED) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Downloaded", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.DOWNLOADED, 0f, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Downloading", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.DOWNLOADING, 1f, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "NotDownloaded", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.NOT_DOWNLOADED, 0f, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Queue", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.QUEUE, -1f, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Checked", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.CHECKED, 0f, Modifier.align(Alignment.CenterVertically))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(text = "Error", color = MaterialTheme.colorScheme.onSurface)
            DownloadButton(buttonColor, Download.State.ERROR, 0f, Modifier.align(Alignment.CenterVertically))
        }
    }
}

val decimalFormat = DecimalFormat(
    "#.###",
    DecimalFormatSymbols()
        .apply { decimalSeparator = '.' },
)

