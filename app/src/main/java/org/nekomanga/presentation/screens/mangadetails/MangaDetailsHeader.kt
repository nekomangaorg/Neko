package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                manga,
                Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(200.dp, 300.dp),
            )
            InformationBlock(
                manga = manga,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 70.dp),
                isExpanded = isExpanded,
                titleLongClick = titleLongClick,
                creatorLongClicked = creatorLongClicked,
            )
        }

    }
}
