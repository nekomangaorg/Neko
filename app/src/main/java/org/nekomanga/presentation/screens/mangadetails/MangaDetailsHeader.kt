package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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

@Composable
fun MangaDetailsHeader(
    manga: Manga,
    isExpanded: Boolean = true,
    titleLongClick: (String) -> Unit = {},
    creatorLongClicked: (String) -> Unit = {},
) {
    Column {
        BoxWithConstraints {
            BackDrop(
                manga = manga,
                modifier = Modifier
                    .fillMaxWidth()

                    .requiredHeightIn(250.dp, 400.dp),
            )
            Spacer(
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
                    creatorLongClicked = creatorLongClicked,
                )
                ButtonBlock(
                    manga,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

        }

    }
}
