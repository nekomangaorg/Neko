package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibraryGroup
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun GroupBySheet(
    groupByOptions: List<LibraryGroup>,
    currentGroupBy: LibraryGroup,
    groupByClick: (LibraryGroup) -> Unit,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    bottomContentPadding: Dp = Size.medium,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration
    ) {
        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .4

        BaseSheet(
            themeColor = themeColorState,
            maxSheetHeightPercentage = .9f,
            bottomPaddingAroundContent = bottomContentPadding,
        ) {
            val paddingModifier = Modifier.padding(horizontal = Size.small)

            Gap(Size.medium)
            Text(
                modifier = paddingModifier.fillMaxWidth(),
                text = stringResource(R.string.sort_by),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Gap(Size.medium)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().requiredHeightIn(Size.none, maxLazyHeight.dp)
            ) {
                items(groupByOptions) { groupBy ->
                    val textColor =
                        if (currentGroupBy == groupBy) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .conditional(currentGroupBy != groupBy) {
                                    this.clickable(onClick = { groupByClick(groupBy) })
                                }
                                .padding(Size.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = groupBy.icon,
                            contentDescription = null,
                            tint = textColor,
                        )
                        Gap(Size.small)
                        Text(
                            text = stringResource(groupBy.nameRes),
                            modifier = Modifier.weight(1f),
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
