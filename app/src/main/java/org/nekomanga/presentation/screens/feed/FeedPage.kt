package org.nekomanga.presentation.screens.feed

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.ui.feed.FeedHistoryGroup
import eu.kanade.tachiyomi.ui.feed.FeedManga
import eu.kanade.tachiyomi.ui.feed.FeedScreenActions
import eu.kanade.tachiyomi.ui.feed.FeedScreenType
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.feed.history.FeedHistoryPage
import org.nekomanga.presentation.screens.feed.summary.FeedSummaryPage
import org.nekomanga.presentation.screens.feed.updates.FeedUpdatesPage
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun FeedPage(
    feedMangaList: ImmutableList<FeedManga>,
    outlineCovers: Boolean,
    outlineCards: Boolean,
    hasMoreResults: Boolean,
    loadingResults: Boolean,
    groupedBySeries: Boolean,
    updatesFetchSort: Boolean,
    feedScreenActions: FeedScreenActions,
    loadNextPage: () -> Unit,
    feedScreenType: FeedScreenType,
    historyGrouping: FeedHistoryGroup,
    contentPadding: PaddingValues = PaddingValues(),
) {
    when (feedScreenType) {
        FeedScreenType.Summary -> {
            FeedSummaryPage(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding)
        }
        FeedScreenType.History -> {
            FeedHistoryPage(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                feedHistoryMangaList = feedMangaList,
                outlineCovers = outlineCovers,
                outlineCards = outlineCards,
                feedScreenActions = feedScreenActions,
                hasMoreResults = hasMoreResults,
                loadingResults = loadingResults,
                loadNextPage = loadNextPage,
                historyGrouping = historyGrouping,
            )
        }
        FeedScreenType.Updates -> {
            FeedUpdatesPage(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                feedUpdatesMangaList = feedMangaList,
                outlineCovers = outlineCovers,
                groupedBySeries = groupedBySeries,
                hasMoreResults = hasMoreResults,
                loadingResults = loadingResults,
                updatesFetchSort = updatesFetchSort,
                feedScreenActions = feedScreenActions,
                loadNextPage = loadNextPage,
            )
        }
    }
}

@Composable
fun getReadTextColor(
    isRead: Boolean,
    defaultColor: Color = MaterialTheme.colorScheme.onSurface,
): Color {
    return when (isRead) {
        true ->
            MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.disabledAlphaLowContrast)
        false -> defaultColor
    }
}

@Composable
fun FeedCover(
    artwork: Artwork,
    outlined: Boolean,
    coverSize: Dp,
    modifier: Modifier = Modifier,
    shoulderOverlayCover: Boolean = false,
    onClick: () -> Unit,
) {

    Box(modifier = modifier.clip(RoundedCornerShape(Shapes.coverRadius)).clickable { onClick() }) {
        MangaCover.Square.invoke(
            artwork = artwork,
            shouldOutlineCover = outlined,
            modifier = Modifier.size(coverSize),
            shoulderOverlayCover = shoulderOverlayCover,
        )
    }
}

@Composable
fun FeedChapterTitleLine(
    language: String,
    isBookmarked: Boolean,
    title: String,
    style: TextStyle,
    textColor: Color,
) {
    Row {
        if (isBookmarked) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(Size.medium).align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.primary,
            )
            Gap(Size.extraTiny)
        }
        if (language.isNotEmpty() && !language.equals("en", true)) {
            val iconRes = MdLang.fromIsoCode(language)?.iconResId

            when (iconRes == null) {
                true -> {
                    TimberKt.e { "Missing flag for $language" }
                }
                false -> {
                    val painter =
                        rememberDrawablePainter(
                            drawable = AppCompatResources.getDrawable(LocalContext.current, iconRes)
                        )
                    Image(
                        painter = painter,
                        modifier =
                            Modifier.height(Size.medium)
                                .clip(RoundedCornerShape(Size.tiny))
                                .align(Alignment.CenterVertically),
                        contentDescription = "flag",
                    )
                    Gap(Size.extraTiny)
                }
            }
        }
        Text(
            text = title,
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
