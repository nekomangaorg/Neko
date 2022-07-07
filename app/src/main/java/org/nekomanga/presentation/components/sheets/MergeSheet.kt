package org.nekomanga.presentation.components.sheets

import Header
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergedManga
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun MergeSheet(themeColorState: ThemeColorState, mergedManga: MergedManga, openMergeSource: (String) -> Unit, removeMergeSource: () -> Unit, cancelClick: () -> Unit) {

    if (mergedManga is MergedManga.IsMerged) {
        BaseSheet(themeColor = themeColorState) {

            TextButton(onClick = { openMergeSource(mergedManga.url) }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.open_merged_in_webview), color = themeColorState.buttonColor)
            }
            Gap(8.dp)
            TextButton(onClick = removeMergeSource, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.remove_merged_source), color = themeColorState.buttonColor)
            }
        }
    } else {
        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .6
        
        BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {

            Header(stringResource(id = R.string.select_an_entry), cancelClick)

        }
    }
}
