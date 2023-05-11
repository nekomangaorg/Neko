package org.nekomanga.presentation.screens.feed

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crazylegend.string.isNotNullOrEmpty
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.recents.FeedHistoryGroup
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
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Padding
import org.nekomanga.presentation.theme.Shapes

@Composable
fun FeedPage(
    contentPadding: PaddingValues,
    feedMangaList: ImmutableList<FeedManga>,
    outlineCovers: Boolean,
    hasMoreResults: Boolean,
    mangaClick: (Long) -> Unit,
    loadNextPage: () -> Unit,
    feedScreenType: FeedScreenType,
    historyScreenGrouping: FeedHistoryGroup,
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
            Column(modifier = Modifier.fillMaxWidth()) {
                var expanded by rememberSaveable { mutableStateOf(false) }
                Text(
                    text = feedManga.mangaTitle, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = LocalContentColor.current,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Padding.small, vertical = Padding.extraSmall),
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Padding.small),
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
                                .size(64.dp),
                        )
                    }
                    Gap(Padding.small)

                    Column(Modifier.fillMaxWidth()) {

                        val textColor = when (feedManga.chapters.first().read) {
                            true -> LocalContentColor.current.copy(alpha = NekoColors.disabledAlphaLowContrast)
                            false -> LocalContentColor.current
                        }


                        Text(text = feedManga.chapters.first().name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor)

                        Row(modifier = Modifier.fillMaxWidth()) {
                            when (feedScreenType) {
                                FeedScreenType.History -> {
                                    Column(modifier = Modifier.weight(1f)) {

                                        Text(
                                            text = "Read ${feedManga.chapters.first().lastRead.timeSpanFromNow}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = updatedColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (!feedManga.chapters.first().read && feedManga.chapters.first().pagesLeft > 0) {
                                            Text(
                                                text = pluralStringResource(id = R.plurals.pages_left, count = 1, feedManga.chapters.first().pagesLeft),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = updatedColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }

                                FeedScreenType.Updates -> {
                                    Text(
                                        text = "Updated ${feedManga.chapters.first().dateUpload.timeSpanFromNow}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = updatedColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (feedManga.chapters.size > 1) {
                                IconButton(modifier = Modifier.align(Alignment.CenterVertically), onClick = { expanded = !expanded }) {
                                    Icon(
                                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                    )
                                }
                            }
                            if (feedScreenType == FeedScreenType.History) {
                                IconButton(modifier = Modifier.align(Alignment.CenterVertically), onClick = { }) {
                                    Icon(
                                        imageVector = Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                    )
                                }
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
                if (expanded) {
                    feedManga.chapters.forEachIndexed { index, simpleChapter ->
                        if (index > 0) {
                            val textColor = when (simpleChapter.read) {
                                true -> LocalContentColor.current.copy(alpha = NekoColors.disabledAlphaLowContrast)
                                false -> LocalContentColor.current
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 64.dp + Padding.medium, end = Padding.small, top = Padding.small, bottom = Padding.small),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .fillMaxWidth(.8f),
                                ) {
                                    /*  val titleText = when (shouldHideChapterTitles) {
                                          true -> stringResource(id = R.string.chapter_, decimalFormat.format(chapterNumber))
                                          false -> title
                                      }*/
                                    val titleText = simpleChapter.name

                                    Row {
                                        if (simpleChapter.bookmark) {
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
                                            style = MaterialTheme.typography.bodyMedium.copy(color = textColor, fontWeight = FontWeight.Medium, letterSpacing = (-.6).sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    val statuses = mutableListOf<String>()

                                    statuses.add("Read ${simpleChapter.lastRead.timeSpanFromNow}")

                                    val showPagesLeft = !simpleChapter.read && simpleChapter.lastPageRead > 0
                                    val resources = LocalContext.current.resources

                                    if (showPagesLeft && simpleChapter.pagesLeft > 0) {
                                        statuses.add(
                                            resources.getQuantityString(R.plurals.pages_left, simpleChapter.pagesLeft, simpleChapter.pagesLeft),
                                        )
                                    } else if (showPagesLeft) {
                                        statuses.add(
                                            pluralStringResource(id = R.plurals.pages_left, count = 1, simpleChapter.pagesLeft),
                                        )
                                    }

                                    if (simpleChapter.scanlator.isNotBlank()) {
                                        statuses.add(simpleChapter.scanlator)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (simpleChapter.language.isNotNullOrEmpty() && !simpleChapter.language.equals("en", true)) {
                                            val iconRes = MdLang.fromIsoCode(simpleChapter.language)?.iconResId

                                            when (iconRes == null) {
                                                true -> {
                                                    loggycat(LogPriority.ERROR) { "Missing flag for $simpleChapter.language" }
                                                    Text(
                                                        text = "$simpleChapter.language • ",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = updatedColor,
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
                                                color = updatedColor,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = (-.6).sp,
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )

                                    }
                                }
                            }
                        }
                    }
                }
            }
            Gap(Padding.small)

            LaunchedEffect(scrollState) {
                if (hasMoreResults && feedMangaList.indexOf(feedManga) >= feedMangaList.size - 4) {
                    loadNextPage()
                }
            }
        }
    }
}
