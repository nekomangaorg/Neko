package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.source.browse.HomePageManga
import java.util.Objects
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.InLibraryBadge
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.MangaGridSubtitle
import org.nekomanga.presentation.components.MangaGridTitle
import org.nekomanga.presentation.theme.Shapes

@Composable
fun BrowseHomePage(
    browseHomePageManga: ImmutableList<HomePageManga>,
    shouldOutlineCover: Boolean,
    isComfortable: Boolean,
    onClick: (Long) -> Unit,
    onLongClick: (DisplayManga) -> Unit,
) {
    val coverSize = (maxOf(LocalConfiguration.current.screenHeightDp, LocalConfiguration.current.screenWidthDp) / 5).dp

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        browseHomePageManga.forEach { homePageManga ->
            item(key = Objects.hash(homePageManga.title)) {
                val headerText = when (homePageManga.titleRes == null) {
                    true -> homePageManga.title ?: ""
                    false -> stringResource(id = homePageManga.titleRes)
                }
                Text(
                    text = headerText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                )
                Gap(4.dp)
            }
            item(key = Objects.hash(homePageManga.title, homePageManga.titleRes, browseHomePageManga.size)) {
                LazyRow(
                    modifier = Modifier.requiredHeight(coverSize + 60.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(homePageManga.displayManga, key = { displayManga -> displayManga.mangaId }) { displayManga ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Shapes.coverRadius))
                                .combinedClickable(
                                    onClick = { onClick(displayManga.mangaId) },
                                    onLongClick = { onLongClick(displayManga) },
                                )
                                .padding(2.dp)
                                .width(IntrinsicSize.Min),
                        ) {
                            Column {
                                MangaCover.Square.invoke(
                                    manga = displayManga,
                                    shouldOutlineCover = shouldOutlineCover,
                                    modifier = Modifier.requiredHeight(coverSize),
                                )
                                MangaGridTitle(title = displayManga.title)
                                MangaGridSubtitle(displayText = displayManga.displayText)
                            }
                            if (displayManga.inLibrary) {
                                val offset = (-2).dp
                                InLibraryBadge(offset, shouldOutlineCover)
                            }
                        }
                    }
                }
            }
        }
    }
}
