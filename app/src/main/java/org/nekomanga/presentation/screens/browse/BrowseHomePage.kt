package org.nekomanga.presentation.screens.browse

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.source.browse.HomePageManga
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import java.util.Objects
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.presentation.components.InLibraryBadge
import org.nekomanga.presentation.components.MangaCover
import org.nekomanga.presentation.components.MangaGridSubtitle
import org.nekomanga.presentation.components.MangaGridTitle
import org.nekomanga.presentation.theme.Padding
import org.nekomanga.presentation.theme.Shapes

@Composable
fun BrowseHomePage(
    browseHomePageManga: ImmutableList<HomePageManga>,
    shouldOutlineCover: Boolean,
    onClick: (Long) -> Unit,
    onLongClick: (DisplayManga) -> Unit,
    titleClick: (DisplayScreenType, String) -> Unit,
) {
    val coverSize = (maxOf(LocalConfiguration.current.screenHeightDp, LocalConfiguration.current.screenWidthDp) / 5).dp

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(browseHomePageManga, key = { homePageManga -> Objects.hash(homePageManga) }) { homePageManga ->
            val headerText = when (homePageManga.displayScreenType.titleRes == null) {
                true -> homePageManga.altTitle
                false -> stringResource(id = homePageManga.displayScreenType.titleRes)
            }
            TextButton(
                onClick = { titleClick(homePageManga.displayScreenType, homePageManga.altTitle) },
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                    Gap(8.dp)
                    Icon(imageVector = Icons.Default.ArrowForward, modifier = Modifier.size(24.dp), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Gap(4.dp)
            LazyRow(
                modifier = Modifier
                    .requiredHeight(coverSize + 70.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item { Gap(Padding.smallHorizontalPadding) }

                items(homePageManga.displayManga, key = { displayManga -> displayManga.mangaId }) { displayManga ->
                    if (displayManga.isVisible) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Shapes.coverRadius))
                                    .combinedClickable(
                                        onClick = { onClick(displayManga.mangaId) },
                                        onLongClick = { onLongClick(displayManga) },
                                    )
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
                            }

                            if (displayManga.inLibrary) {
                                InLibraryBadge(shouldOutlineCover)
                            }
                        }
                    }
                }

                item { Gap(Padding.smallHorizontalPadding) }
            }
        }
    }
}
