package org.nekomanga.presentation.screens.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.PersistentList
import org.nekomanga.presentation.components.sheets.BaseSheet
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun DownloadBottomSheet(
    downloads: PersistentList<Download>,
    contentPadding: PaddingValues,
    themeColorState: ThemeColorState = defaultThemeColorState()
) {
    BaseSheet(
        themeColor = themeColorState,
        maxSheetHeightPercentage = .6f,
        bottomPaddingAroundContent = contentPadding.calculateBottomPadding()
    ) {
        Gap(16.dp)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium),
            verticalArrangement = Arrangement.spacedBy(Size.small),
        ) {
            items(downloads, { download -> download.chapter.id!! }) { download ->
                DownloadChapterRow(download)
            }
        }
    }
}
