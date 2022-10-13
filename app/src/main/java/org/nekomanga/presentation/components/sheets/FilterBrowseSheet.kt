package org.nekomanga.presentation.components.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.isUUID
import jp.wasabeef.gap.Gap
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.NewFilter
import org.nekomanga.domain.filter.TagMode
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.components.ExpandableRow
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.SearchFooter
import org.nekomanga.presentation.components.SortRow
import org.nekomanga.presentation.components.TriStateCheckboxRow
import org.nekomanga.presentation.components.dialog.SaveFilterDialog
import org.nekomanga.presentation.extensions.surfaceColorAtElevation
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState

@Composable
fun FilterBrowseSheet(
    filters: DexFilters,
    bottomPadding: Dp = 16.dp,
    filterClick: () -> Unit,
    saveClick: (String) -> Unit,
    resetClick: () -> Unit,
    filterChanged: (NewFilter) -> Unit,
    savedFilters: List<String>,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme) {

        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .75

        BaseSheet(themeColor = themeColorState, minSheetHeightPercentage = .9f, maxSheetHeightPercentage = 1f, bottomPaddingAroundContent = bottomPadding) {

            val paddingModifier = Modifier.padding(horizontal = 8.dp)

            val onSurface = MaterialTheme.colorScheme.onSurface
            val disabledOnSurface = MaterialTheme.colorScheme.onSurface.copy(NekoColors.disabledAlphaLowContrast)

            var originalLanguageExpanded by remember { mutableStateOf(false) }
            var contentRatingExpanded by remember { mutableStateOf(false) }
            var publicationDemographicExpanded by remember { mutableStateOf(false) }
            var statusExpanded by remember { mutableStateOf(false) }
            var sortExpanded by remember { mutableStateOf(false) }
            var tagExpanded by remember { mutableStateOf(false) }
            var saveExpanded by remember { mutableStateOf(false) }
            var otherExpanded by remember { mutableStateOf(false) }
            var showSaveFilterDialog by remember { mutableStateOf(false) }

            if (showSaveFilterDialog) {
                SaveFilterDialog(themeColorState = themeColorState, currentSavedFilters = savedFilters, onDismiss = { showSaveFilterDialog = false }, onConfirm = { saveClick(it) })
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(0.dp, maxLazyHeight.dp),
            ) {

                item {
                    ExpandableRow(
                        isExpanded = originalLanguageExpanded,
                        onClick = { originalLanguageExpanded = !originalLanguageExpanded },
                        rowText = stringResource(id = R.string.original_language),
                    )
                }
                items(filters.originalLanguage) { originalLanguage ->
                    AnimatedVisibility(visible = originalLanguageExpanded) {
                        CheckboxRow(
                            modifier = Modifier.fillMaxWidth(),
                            checkedState = originalLanguage.state,
                            disabled = !originalLanguage.enabled,
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
                            disabled = !contentRating.enabled,
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
                            disabled = !demographic.enabled,
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
                            disabled = !status.enabled,
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
                            disabled = !sort.enabled,
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
                            disabled = !tag.enabled,
                            toggleState = { newState -> filterChanged(tag.copy(state = newState)) },
                            rowText = tag.tag.prettyPrint,
                        )
                    }
                }

                item {
                    ExpandableRow(
                        isExpanded = otherExpanded,
                        onClick = { otherExpanded = !otherExpanded },
                        rowText = stringResource(id = R.string.other),
                    )
                }
                item {
                    AnimatedVisibility(visible = otherExpanded) {
                        CheckboxRow(
                            checkedState = filters.hasAvailableChapters.state,
                            disabled = !filters.hasAvailableChapters.enabled,
                            checkedChange = { newState -> filterChanged(filters.hasAvailableChapters.copy(state = newState)) },
                            rowText = stringResource(
                                id = R.string.has_available_chapters,
                            ),
                        )
                    }
                }

                items(filters.tags) { tag ->
                    AnimatedVisibility(visible = tagExpanded) {
                        TriStateCheckboxRow(
                            modifier = Modifier.fillMaxWidth(),
                            state = tag.state,
                            disabled = !tag.enabled,
                            toggleState = { newState -> filterChanged(tag.copy(state = newState)) },
                            rowText = tag.tag.prettyPrint,
                        )
                    }
                }

                item {
                    AnimatedVisibility(visible = otherExpanded) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(text = stringResource(id = R.string.tag_inclusion_mode), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = filters.tagInclusionMode.mode == TagMode.And,
                                    enabled = filters.tagInclusionMode.enabled,
                                    onClick = { filterChanged(filters.tagInclusionMode.copy(mode = TagMode.And)) },
                                )
                                Text(text = stringResource(id = R.string.and), color = if (filters.tagInclusionMode.enabled) onSurface else disabledOnSurface)
                                RadioButton(
                                    selected = filters.tagInclusionMode.mode == TagMode.Or,
                                    enabled = filters.tagInclusionMode.enabled,
                                    onClick = { filterChanged(filters.tagInclusionMode.copy(mode = TagMode.Or)) },
                                )
                                Text(text = stringResource(id = R.string.or), color = if (filters.tagInclusionMode.enabled) onSurface else disabledOnSurface)
                            }
                        }

                    }
                }

                item {
                    AnimatedVisibility(visible = otherExpanded) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(text = stringResource(id = R.string.tag_exclusion_mode), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = filters.tagExclusionMode.mode == TagMode.And,
                                    enabled = filters.tagExclusionMode.enabled,
                                    onClick = { filterChanged(filters.tagExclusionMode.copy(mode = TagMode.And)) },
                                )
                                Text(text = stringResource(id = R.string.and), color = if (filters.tagInclusionMode.enabled) onSurface else disabledOnSurface)
                                RadioButton(
                                    selected = filters.tagExclusionMode.mode == TagMode.Or,
                                    enabled = filters.tagExclusionMode.enabled,
                                    onClick = { filterChanged(filters.tagExclusionMode.copy(mode = TagMode.Or)) },
                                )
                                Text(text = stringResource(id = R.string.or), color = if (filters.tagInclusionMode.enabled) onSurface else disabledOnSurface)
                            }
                        }

                    }
                }

                item {
                    AnimatedVisibility(visible = otherExpanded) {
                        val isError = remember(filters.groupId.uuid) {
                            if (filters.groupId.uuid.isBlank()) {
                                false
                            } else {
                                !filters.groupId.uuid.isUUID()
                            }
                        }
                        SearchFooter(
                            themeColorState = themeColorState,
                            labelText = stringResource(id = R.string.scanlator_group_id),
                            showDivider = false,
                            title = filters.groupId.uuid,
                            isError = isError,
                            enabled = filters.groupId.enabled,
                            textChanged = { text: String -> filterChanged(NewFilter.GroupId(text)) },
                            search = { filterClick() },
                        )
                        Gap(4.dp)
                    }
                }

                item {
                    AnimatedVisibility(visible = otherExpanded) {
                        val isError = remember(filters.authorId.uuid) {
                            if (filters.authorId.uuid.isBlank()) {
                                false
                            } else {
                                !filters.authorId.uuid.isUUID()
                            }
                        }
                        SearchFooter(
                            themeColorState = themeColorState,
                            labelText = stringResource(id = R.string.author_id),
                            showDivider = false,
                            title = filters.authorId.uuid,
                            isError = isError,
                            enabled = filters.authorId.enabled,
                            textChanged = { text: String -> filterChanged(NewFilter.AuthorId(text)) },
                            search = { filterClick() },
                        )
                    }
                }

                if (savedFilters.isNotEmpty()) {
                    item {
                        ExpandableRow(
                            isExpanded = saveExpanded,
                            onClick = { saveExpanded = !saveExpanded },
                            rowText = stringResource(id = R.string.saved_filter),
                        )
                    }
                }

                item {
                    AnimatedVisibility(visible = saveExpanded) {
                        FlowRow(modifier = Modifier.fillMaxWidth(), mainAxisSpacing = 4.dp) {
                            savedFilters.forEach { name ->
                                FilterChip(
                                    selected = false,
                                    onClick = { showSaveFilterDialog = true },
                                    shape = RoundedCornerShape(100),
                                    label = { Text(text = name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            }
                        }
                    }
                }

                item {
                    SearchFooter(
                        themeColorState = themeColorState,
                        labelText = stringResource(id = R.string.title),
                        showDivider = false,
                        title = filters.titleQuery.query,
                        enabled = filters.titleQuery.enabled,
                        textChanged = { text: String -> filterChanged(NewFilter.TitleQuery(text)) },
                        search = { filterClick() },
                    )
                }

                item {
                    SearchFooter(
                        themeColorState = themeColorState,
                        labelText = stringResource(id = R.string.author),
                        showDivider = false,
                        enabled = filters.authorQuery.enabled,
                        title = filters.authorQuery.query,
                        textChanged = { text: String -> filterChanged(NewFilter.AuthorQuery(text)) },
                        search = { filterClick() },
                    )
                }

                item {
                    SearchFooter(
                        themeColorState = themeColorState,
                        labelText = stringResource(id = R.string.scanlator_group),
                        showDivider = false,
                        enabled = filters.groupQuery.enabled,
                        title = filters.groupQuery.query,
                        textChanged = { text: String -> filterChanged(NewFilter.GroupQuery(text)) },
                        search = { filterClick() },
                    )

                }

            }

            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = paddingModifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = resetClick, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.reset), style = MaterialTheme.typography.titleSmall)
                }


                TextButton(onClick = { showSaveFilterDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                    Text(text = stringResource(id = R.string.save), style = MaterialTheme.typography.titleSmall)
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
