package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.database.models.Manga
import jp.wasabeef.gap.Gap

@Composable
fun MangaDetailsHeader(
    manga: Manga,
    isExpanded: Boolean = true,
    themeBasedOffCover: Boolean = true,
    titleLongClick: (String) -> Unit = {},
    creatorLongClick: (String) -> Unit = {},
    trackServiceCount: Int,
    favoriteClick: () -> Unit = {},
    trackingClick: () -> Unit = {},
    artworkClick: () -> Unit = {},
    similarClick: () -> Unit = {},
    mergeClick: () -> Unit = {},
    linksClick: () -> Unit = {},
    shareClick: () -> Unit = {},
) {
    Column {
        BoxWithConstraints {
            BackDrop(
                manga = manga,
                themeBasedOffCover = themeBasedOffCover,
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(250.dp, 400.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                        ),
                    ),
            )
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                InformationBlock(
                    manga = manga,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 70.dp),
                    isExpanded = isExpanded,
                    titleLongClick = titleLongClick,
                    creatorLongClicked = creatorLongClick,
                )
                Gap(height = 24.dp)
                ButtonBlock(
                    manga = manga,
                    trackServiceCount = trackServiceCount,
                    themeBasedOffCover = themeBasedOffCover,
                    favoriteClick = favoriteClick,
                    trackingClick = trackingClick,
                    artworkClick = artworkClick,
                    similarClick = similarClick,
                    mergeClick = mergeClick,
                    linksClick = linksClick,
                    shareClick = shareClick,
                )
            }
        }
        Gap(height = 16.dp)
        DescriptionBlock(manga, isExpanded)

    }
}
