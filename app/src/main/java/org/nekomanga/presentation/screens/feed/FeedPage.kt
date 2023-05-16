package org.nekomanga.presentation.screens.feed

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.crazylegend.string.isNotNullOrEmpty
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.recents.FeedManga
import eu.kanade.tachiyomi.ui.recents.FeedScreenType
import eu.kanade.tachiyomi.util.system.loggycat
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import logcat.LogPriority
import org.nekomanga.presentation.components.DownloadButton
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.decimalFormat
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Padding
import org.nekomanga.presentation.theme.Shapes

@Composable
fun FeedPage(
    contentPadding: PaddingValues,
    feedMangaList: ImmutableList<FeedManga>,
    outlineCovers: Boolean,
    hasMoreResults: Boolean,
    hideChapterTitles: Boolean,
    mangaClick: (Long) -> Unit,
    loadNextPage: () -> Unit,
    feedScreenType: FeedScreenType,
) {
    val scrollState = rememberLazyListState()

    val updatedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast)
    var chapterDropdown by remember { mutableStateOf(false) }

    val themeColorState = defaultThemeColorState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = scrollState,
        contentPadding = contentPadding,
    ) {
        items(feedMangaList) { feedManga ->
            var expanded by rememberSaveable { mutableStateOf(false) }
            val cardColor: Color by animateColorAsState(if (expanded) MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp) else MaterialTheme.colorScheme.surface)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Padding.small)
                    .animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
            ) {
                val titleColor = getTextColor(isRead = feedManga.chapters.all { it.read })

                Text(
                    text = feedManga.mangaTitle, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = titleColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Padding.small, vertical = Padding.extraSmall),
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Padding.small),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Shapes.coverRadius))
                            .clickable { mangaClick(feedManga.mangaId) }
                            .align(Alignment.CenterVertically),
                    ) {
                        MangaCover.Square.invoke(
                            artwork = feedManga.artwork,
                            shouldOutlineCover = outlineCovers,
                            modifier = Modifier
                                .size(80.dp),
                        )
                    }
                    Gap(Padding.small)

                    Column(Modifier.fillMaxWidth()) {
                        val firstChapter = feedManga.chapters.first()
                        val textColor = getTextColor(isRead = firstChapter.read)

                        ChapterTitleLine(
                            hideChapterTitles = hideChapterTitles,
                            isBookmarked = firstChapter.bookmark,
                            chapterNumber = firstChapter.chapterNumber,
                            title = firstChapter.name,
                            style = MaterialTheme.typography.bodyMedium,
                            textColor = textColor,
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            when (feedScreenType) {
                                FeedScreenType.History -> {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val readColor = getTextColor(isRead = firstChapter.read, updatedColor)
                                        LastReadLine(
                                            lastRead = firstChapter.lastRead,
                                            scanlator = firstChapter.scanlator,
                                            language = firstChapter.language,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textColor = readColor,
                                        )
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
                                            if (feedManga.chapters.size > 1) {
                                                IconButton(modifier = Modifier.align(Alignment.CenterVertically), onClick = { expanded = !expanded }) {
                                                    Icon(
                                                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                            IconButton(modifier = Modifier.align(Alignment.CenterVertically), onClick = { }) {
                                                Icon(
                                                    imageVector = Icons.Filled.VisibilityOff,
                                                    contentDescription = null,
                                                )
                                            }
                                            Box(modifier = Modifier.align(Alignment.CenterVertically), contentAlignment = Alignment.Center) {
                                                DownloadButton(
                                                    themeColorState.buttonColor,
                                                    Download.State.NOT_DOWNLOADED,
                                                    0f,
                                                    Modifier
                                                        .combinedClickable(
                                                            onClick = {
                                                                when (Download.State.NOT_DOWNLOADED) {
                                                                    Download.State.NOT_DOWNLOADED -> Unit //onDownload(MangaConstants.DownloadAction.Download)
                                                                    else -> chapterDropdown = true
                                                                }
                                                            },
                                                            onLongClick = {},
                                                        ),
                                                )
                                            }
                                        }

                                    }
                                }

                                FeedScreenType.Updates -> {
                                    Text(
                                        text = "Updated ${firstChapter.dateUpload.timeSpanFromNow}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = updatedColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                    }

                }
                if (expanded) {
                    feedManga.chapters.forEachIndexed { index, simpleChapter ->
                        if (index > 0) {
                            Divider(Modifier.padding(Padding.small))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = Padding.small),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .fillMaxWidth(.8f),
                                ) {

                                    ChapterTitleLine(
                                        hideChapterTitles = hideChapterTitles,
                                        isBookmarked = simpleChapter.bookmark,
                                        chapterNumber = simpleChapter.chapterNumber,
                                        title = simpleChapter.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textColor = getTextColor(isRead = simpleChapter.read),
                                    )

                                    LastReadLine(
                                        lastRead = simpleChapter.lastRead,
                                        scanlator = simpleChapter.scanlator,
                                        language = simpleChapter.language,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textColor = getTextColor(isRead = simpleChapter.read, updatedColor),
                                    )
                                }
                                Box(modifier = Modifier.align(Alignment.CenterEnd), contentAlignment = Alignment.Center) {
                                    DownloadButton(
                                        themeColorState.buttonColor,
                                        Download.State.NOT_DOWNLOADED,
                                        0f,
                                        Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    when (Download.State.NOT_DOWNLOADED) {
                                                        Download.State.NOT_DOWNLOADED -> Unit //onDownload(MangaConstants.DownloadAction.Download)
                                                        else -> chapterDropdown = true
                                                    }
                                                },
                                                onLongClick = {},
                                            ),
                                    )
                                }

                            }
                        }
                    }
                    Gap(Padding.small)
                }
            }



            LaunchedEffect(scrollState) {
                if (hasMoreResults && feedMangaList.indexOf(feedManga) >= feedMangaList.size - 4) {
                    loadNextPage()
                }
            }
        }
    }
}

@Composable
private fun getTextColor(isRead: Boolean, defaultColor: Color = LocalContentColor.current): Color {
    return when (isRead) {
        true -> LocalContentColor.current.copy(alpha = NekoColors.disabledAlphaLowContrast)
        false -> defaultColor
    }
}

@Composable
private fun ChapterTitleLine(hideChapterTitles: Boolean, isBookmarked: Boolean, chapterNumber: Float, title: String, style: TextStyle, textColor: Color) {
    val titleText = when (hideChapterTitles) {
        true -> stringResource(id = R.string.chapter_, decimalFormat.format(chapterNumber))
        false -> title
    }
    Row {
        if (isBookmarked) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.primary,
            )
            Gap(4.dp)
        }
        Text(
            text = titleText,
            style = style.copy(color = textColor, fontWeight = FontWeight.Medium, letterSpacing = (-.6).sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
                    loggycat(LogPriority.ERROR) { "Missing flag for $language" }
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

