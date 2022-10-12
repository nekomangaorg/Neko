package org.nekomanga.presentation.components.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.nekomanga.presentation.components.SortRow
import org.nekomanga.presentation.components.TriStateCheckboxRow
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

            var originalLanguageExpanded by remember { mutableStateOf(false) }
            var contentRatingExpanded by remember { mutableStateOf(false) }
            var publicationDemographicExpanded by remember { mutableStateOf(false) }
            var statusExpanded by remember { mutableStateOf(false) }
            var sortExpanded by remember { mutableStateOf(false) }
            var tagExpanded by remember { mutableStateOf(false) }


            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(0.dp, maxLazyHeight.dp),
            ) {

                item {
                    ExpandableRow(isExpanded = originalLanguageExpanded, onClick = { originalLanguageExpanded = !originalLanguageExpanded }, rowText = stringResource(id = R.string.original_language))
                }
                items(filters.originalLanguage) { originalLanguage ->
                    AnimatedVisibility(visible = originalLanguageExpanded) {
                        CheckboxRow(
                            modifier = Modifier.fillMaxWidth(),
                            checkedState = originalLanguage.state,
                            checkedChange = { newState -> filterChanged(originalLanguage.copy(state = newState)) },
                            rowText = originalLanguage.language.prettyPrint,
                        )
                    }
                }

                item {
                    ExpandableRow(isExpanded = contentRatingExpanded, onClick = { contentRatingExpanded = !contentRatingExpanded }, rowText = stringResource(id = R.string.content_rating))
                }
                items(filters.contentRatings) { contentRating ->
                    AnimatedVisibility(visible = contentRatingExpanded) {

                        CheckboxRow(
                            modifier = Modifier.fillMaxWidth(),
                            checkedState = contentRating.state,
                            checkedChange = { newState -> filterChanged(contentRating.copy(state = newState)) },
                            rowText = contentRating.rating.prettyPrint(),
                        )
                    }
                }


                item {
                    ExpandableRow(
                        isExpanded = publicationDemographicExpanded,
                        onClick = { publicationDemographicExpanded = !publicationDemographicExpanded },
                        rowText = stringResource(id = R.string.publication_demographic),
                    )
                }
                items(filters.publicationDemographics) { demographic ->
                    AnimatedVisibility(visible = publicationDemographicExpanded) {
                        CheckboxRow(
                            modifier = Modifier.fillMaxWidth(),
                            checkedState = demographic.state,
                            checkedChange = { newState -> filterChanged(demographic.copy(state = newState)) },
                            rowText = demographic.demographic.prettyPrint(),
                        )
                    }
                }

                item {
                    ExpandableRow(isExpanded = statusExpanded, onClick = { statusExpanded = !statusExpanded }, rowText = stringResource(id = R.string.status))
                }

                items(filters.statuses) { status ->
                    AnimatedVisibility(visible = statusExpanded) {
                        CheckboxRow(
                            modifier = Modifier.fillMaxWidth(),
                            checkedState = status.state,
                            checkedChange = { newState -> filterChanged(status.copy(state = newState)) },
                            rowText = stringResource(id = status.status.statusRes),
                        )
                    }
                }

                item {
                    ExpandableRow(isExpanded = sortExpanded, onClick = { sortExpanded = !sortExpanded }, rowText = stringResource(id = R.string.sort))
                }
                items(filters.sort) { sort ->
                    AnimatedVisibility(visible = sortExpanded) {
                        SortRow(
                            modifier = Modifier.fillMaxWidth(),
                            sortState = sort.state,
                            sortChanged = { sortState -> filterChanged(sort.copy(state = sortState)) },
                            rowText = sort.sort.displayName,
                        )
                    }
                }

                item {
                    ExpandableRow(isExpanded = tagExpanded, onClick = { tagExpanded = !tagExpanded }, rowText = stringResource(id = R.string.tag))
                }

                items(filters.tags) { tag ->
                    AnimatedVisibility(visible = tagExpanded) {
                        TriStateCheckboxRow(
                            modifier = Modifier.fillMaxWidth(),
                            state = tag.state,
                            toggleState = { newState -> filterChanged(tag.copy(state = newState)) },
                            rowText = tag.tag.prettyPrint,
                        )
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
