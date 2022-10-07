package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.SearchFooter
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState

@Composable
fun FilterBrowseSheet(
    query: String = "",
    bottomPadding: Dp = 16.dp,
    filterClick: () -> Unit,
    resetClick: () -> Unit,
    filterChanged: (String) -> Unit,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme) {

        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .7

        BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f, bottomPaddingAroundContent = bottomPadding) {

            val paddingModifier = Modifier.padding(horizontal = 8.dp)


            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(0.dp, maxLazyHeight.dp),
            ) {

            }

            SearchFooter(themeColorState = themeColorState, title = query, textChanged = filterChanged, search = { filterClick() })

            Divider()
            Gap(4.dp)
            Row(modifier = paddingModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = resetClick, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.reset), style = MaterialTheme.typography.titleSmall)
                }
                ElevatedButton(
                    onClick = filterClick,
                    colors = ButtonDefaults.elevatedButtonColors(containerColor = themeColorState.buttonColor),
                ) {
                    Text(text = stringResource(id = R.string.filter), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.surface)
                }
            }
            Gap(16.dp)
        }
    }
}
