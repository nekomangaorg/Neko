package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.presentation.screens.ChapterRow
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun ChapterBlock(themeColorState: ThemeColorState, chapterItems: List<ChapterItem>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(chapterItems) { chapterItem ->
            ChapterRow(themeColor = themeColorState, chapterItem = chapterItem)
        }
    }
}
