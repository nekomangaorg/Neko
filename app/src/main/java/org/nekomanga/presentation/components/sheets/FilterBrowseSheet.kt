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
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.NewFilter
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.components.ExpandableRow
import org.nekomanga.presentation.components.SearchFooter
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState

@Composable
fun FilterBrowseSheet(
    filters: DexFilters,
    bottomPadding: Dp = 16.dp,
    filterClick: () -> Unit,
    resetClick: () -> Unit,
    filterChanged: (NewFilter) -> Unit,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme) {

        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .65

        BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = 1f, bottomPaddingAroundContent = bottomPadding) {

            val paddingModifier = Modifier.padding(horizontal = 8.dp)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(0.dp, maxLazyHeight.dp),
            ) {

                item {
                    ExpandableRow(modifier = Modifier.fillMaxWidth(), rowText = stringResource(id = R.string.original_language)) {
                        filters.originalLanguage.forEach { originalLanguage ->
                            CheckboxRow(
                                modifier = Modifier.fillMaxWidth(),
                                checkedState = originalLanguage.state,
                                checkedChange = { newState -> filterChanged(originalLanguage.copy(state = newState)) },
                                rowText = originalLanguage.language.prettyPrint,
                            )
                        }
                    }
                }

                item {
                    ExpandableRow(modifier = Modifier.fillMaxWidth(), rowText = stringResource(id = R.string.content_rating)) {
                        filters.contentRatings.forEach { contentRating ->
                            CheckboxRow(
                                modifier = Modifier.fillMaxWidth(),
                                checkedState = contentRating.state,
                                checkedChange = { newState -> filterChanged(contentRating.copy(state = newState)) },
                                rowText = contentRating.rating.prettyPrint(),
                            )
                        }
                    }
                }

                item {
                    ExpandableRow(modifier = Modifier.fillMaxWidth(), rowText = stringResource(id = R.string.publication_demographic)) {
                        filters.publicationDemographics.forEach { demographic ->
                            CheckboxRow(
                                modifier = Modifier.fillMaxWidth(),
                                checkedState = demographic.state,
                                checkedChange = { newState -> filterChanged(demographic.copy(state = newState)) },
                                rowText = demographic.demographic.prettyPrint(),
                            )
                        }
                    }
                }

                item {
                    ExpandableRow(modifier = Modifier.fillMaxWidth(), rowText = stringResource(id = R.string.status)) {
                        filters.statuses.forEach { status ->
                            CheckboxRow(
                                modifier = Modifier.fillMaxWidth(),
                                checkedState = status.state,
                                checkedChange = { newState -> filterChanged(status.copy(state = newState)) },
                                rowText = stringResource(id = status.status.statusRes),
                            )
                        }
                    }
                }

                item {
                    SearchFooter(
                        themeColorState = themeColorState,
                        labelText = stringResource(id = R.string.title),
                        showDivider = false,
                        title = filters.titleQuery.query,
                        textChanged = { text: String -> filterChanged(NewFilter.TitleQuery(text)) },
                        search = { filterClick() },
                    )

                }
            }

            Gap(height = 4.dp)
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
        }
    }
}
