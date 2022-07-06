package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.screens.ThemeColorState

@Composable
fun MergeSheet(themeColorState: ThemeColorState, isMerged: Boolean) {

    BaseSheet(themeColor = themeColorState) {
        if (isMerged) {
            TextButton(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.open_merged_in_webview))
            }
            Gap(8.dp)
            TextButton(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.remove_merged_source))
            }
        } else {

        }

    }
}
