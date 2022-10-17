package org.nekomanga.presentation.components.sheets

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.util.lang.isUUID
import jp.wasabeef.gap.Gap
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.NewFilter
import org.nekomanga.domain.filter.QueryType
import org.nekomanga.domain.filter.TagMode
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.components.ExpandableRow
import org.nekomanga.presentation.components.FilterChipWrapper
import org.nekomanga.presentation.components.SearchFooter
import org.nekomanga.presentation.components.SortRow
import org.nekomanga.presentation.components.TriStateFilterChip
import org.nekomanga.presentation.components.dialog.SaveFilterDialog
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState

@Composable
fun FilterBrowseSheet(
    filters: DexFilters,
    bottomPadding: Dp = 16.dp,
    filterClick: () -> Unit,
    saveClick: (String) -> Unit,
    resetClick: () -> Unit,
    deleteFilterClick: (String) -> Unit,
    filterDefaultClick: (String, Boolean) -> Unit,
    loadFilter: (BrowseFilterImpl) -> Unit,
    filterChanged: (NewFilter) -> Unit,
    savedFilters: List<BrowseFilterImpl>,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme) {

        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .75

        BaseSheet(themeColor = themeColorState, minSheetHeightPercentage = .75f, maxSheetHeightPercentage = 1f, bottomPaddingAroundContent = 0.dp) {

            val paddingModifier = Modifier.padding(horizontal = 8.dp)

            val onSurface = MaterialTheme.colorScheme.onSurface

            var originalLanguageExpanded by remember { mutableStateOf(false) }
            var contentRatingExpanded by remember { mutableStateOf(false) }
            var publicationDemographicExpanded by remember { mutableStateOf(false) }
            var statusExpanded by remember { mutableStateOf(false) }
            var sortExpanded by remember { mutableStateOf(false) }
            var tagExpanded by remember { mutableStateOf(false) }
            var otherExpanded by remember { mutableStateOf(false) }

            var showSaveFilterDialog by remember { mutableStateOf(false) }

            var nameOfEnabledFilter by rememberSaveable(filters, savedFilters) {
                mutableStateOf(
                    savedFilters.firstOrNull { Json.decodeFromString<DexFilters>(it.dexFilters) == filters }?.name ?: "",
                )
            }

            val disabled by remember(filters.queryMode) {
                mutableStateOf(filters.queryMode != QueryType.Title)
            }

            LaunchedEffect(key1 = filters.queryMode) {
                if (filters.queryMode != QueryType.Title) {
                    originalLanguageExpanded = false
                    contentRatingExpanded = false
                    publicationDemographicExpanded = false
                    statusExpanded = false
                    sortExpanded = false
                    tagExpanded = false
                    otherExpanded = false
                }
            }


            if (showSaveFilterDialog) {
                SaveFilterDialog(themeColorState = themeColorState, currentSavedFilters = savedFilters, onDismiss = { showSaveFilterDialog = false }, onConfirm = { saveClick(it) })
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(0.dp, maxLazyHeight.dp),
            ) {

                filterRow(
                    items = filters.originalLanguage,
                    expanded = originalLanguageExpanded,
                    disabled = disabled,
                    headerClicked = { originalLanguageExpanded = !originalLanguageExpanded },
                    headerRes = R.string.original_language,
                    onClick = { originalLanguage -> filterChanged(originalLanguage.copy(state = !originalLanguage.state)) },
                    selected = { originalLanguage -> originalLanguage.state },
                    name = { originalLanguage -> originalLanguage.language.prettyPrint },
                )

                filterRow(
                    items = filters.contentRatings,
                    expanded = contentRatingExpanded,
                    disabled = disabled,
                    headerClicked = { contentRatingExpanded = !contentRatingExpanded },
                    headerRes = R.string.content_rating,
                    onClick = { rating -> filterChanged(rating.copy(state = !rating.state)) },
                    selected = { rating -> rating.state },
                    nameRes = { rating -> rating.rating.nameRes },
                )

                filterRow(
                    items = filters.publicationDemographics,
                    expanded = publicationDemographicExpanded,
                    disabled = disabled,
                    headerClicked = { publicationDemographicExpanded = !publicationDemographicExpanded },
                    headerRes = R.string.publication_demographic,
                    onClick = { demo -> filterChanged(demo.copy(state = !demo.state)) },
                    selected = { demo -> demo.state },
                    nameRes = { demo -> demo.demographic.nameRes },
                )

                filterRow(
                    items = filters.statuses,
                    expanded = statusExpanded,
                    disabled = disabled,
                    headerClicked = { statusExpanded = !statusExpanded },
                    headerRes = R.string.status,
                    onClick = { status -> filterChanged(status.copy(state = !status.state)) },
                    selected = { status -> status.state },
                    nameRes = { status -> status.status.statusRes },
                )


                item {
                    ExpandableRow(
                        isExpanded = sortExpanded,
                        disabled = disabled,
                        onClick = { sortExpanded = !sortExpanded }, rowText = stringResource(id = R.string.sort),
                    )
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

                filterTriStateRow(
                    items = filters.tags,
                    expanded = tagExpanded,
                    disabled = disabled,
                    headerClicked = { tagExpanded = !tagExpanded },
                    headerRes = R.string.tag,
                    toggleState = { newState, tag -> filterChanged(tag.copy(state = newState)) },
                    selected = { tag -> tag.state },
                    name = { tag -> tag.tag.prettyPrint },
                )


                item {
                    ExpandableRow(
                        isExpanded = otherExpanded,
                        disabled = disabled,
                        onClick = { otherExpanded = !otherExpanded },
                        rowText = stringResource(id = R.string.other),
                    )
                }
                item {
                    AnimatedVisibility(visible = otherExpanded) {
                        CheckboxRow(
                            checkedState = filters.hasAvailableChapters.state,
                            checkedChange = { newState -> filterChanged(filters.hasAvailableChapters.copy(state = newState)) },
                            rowText = stringResource(
                                id = R.string.has_available_chapters,
                            ),
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
                                    onClick = { filterChanged(filters.tagInclusionMode.copy(mode = TagMode.And)) },
                                )
                                Text(text = stringResource(id = R.string.and), color = onSurface)
                                RadioButton(
                                    selected = filters.tagInclusionMode.mode == TagMode.Or,
                                    onClick = { filterChanged(filters.tagInclusionMode.copy(mode = TagMode.Or)) },
                                )
                                Text(text = stringResource(id = R.string.or), color = onSurface)
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
                                    onClick = { filterChanged(filters.tagExclusionMode.copy(mode = TagMode.And)) },
                                )
                                Text(text = stringResource(id = R.string.and), color = onSurface)
                                RadioButton(
                                    selected = filters.tagExclusionMode.mode == TagMode.Or,
                                    onClick = { filterChanged(filters.tagExclusionMode.copy(mode = TagMode.Or)) },
                                )
                                Text(text = stringResource(id = R.string.or), color = onSurface)
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
                            textChanged = { text: String -> filterChanged(NewFilter.AuthorId(text)) },
                            search = { filterClick() },
                        )
                        Gap(8.dp)
                    }
                }

                item {
                    AnimatedVisibility(visible = savedFilters.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {

                            Text(text = stringResource(id = R.string.saved_filter), modifier = Modifier.padding(start = 16.dp), style = MaterialTheme.typography.labelMedium)

                            LazyRow(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                item { Gap(4.dp) }
                                items(savedFilters) { filter ->
                                    FilterChipWrapper(nameOfEnabledFilter.equals(filter.name, true), { loadFilter(filter) }, filter.name)
                                }
                                item { Gap(4.dp) }
                            }
                            AnimatedVisibility(visible = nameOfEnabledFilter.isNotBlank()) {

                                Row(modifier = paddingModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val isDefault = savedFilters.firstOrNull { nameOfEnabledFilter.equals(it.name, true) }?.default ?: false

                                    val (textRes, makeDefault) = when (isDefault) {
                                        true -> R.string.remove_default to false
                                        false -> R.string.make_default to true
                                    }

                                    TextButton(onClick = { deleteFilterClick(nameOfEnabledFilter) }, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                                        Text(text = stringResource(id = R.string.delete_filter), style = MaterialTheme.typography.titleSmall)
                                    }

                                    ElevatedButton(
                                        onClick = { filterDefaultClick(nameOfEnabledFilter, makeDefault) },
                                        colors = ButtonDefaults.elevatedButtonColors(containerColor = themeColorState.buttonColor),
                                    ) {
                                        Text(text = stringResource(id = textRes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.surface)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val titleRes = when (filters.queryMode) {
                        QueryType.Title -> {
                            R.string.title
                        }
                        QueryType.Author -> {
                            R.string.author
                        }
                        QueryType.Group -> {
                            R.string.scanlator_group
                        }
                    }
                    SearchFooter(
                        themeColorState = themeColorState,
                        labelText = stringResource(id = titleRes),
                        showDivider = false,
                        title = filters.query.text,
                        textChanged = { text: String -> filterChanged(filters.query.copy(text = text)) },
                        search = { filterClick() },
                    )

                }
                item {
                    Row(Modifier.fillMaxWidth(), Arrangement.Center) {
                        FilterChipWrapper(
                            filters.queryMode == QueryType.Title,
                            { filterChanged(NewFilter.Query("", QueryType.Title)) },
                            stringResource(id = R.string.title),
                        )
                        Gap(8.dp)
                        FilterChipWrapper(
                            filters.queryMode == QueryType.Author,
                            { filterChanged(NewFilter.Query("", QueryType.Author)) },
                            stringResource(id = R.string.author),
                        )
                        Gap(8.dp)
                        FilterChipWrapper(
                            filters.queryMode == QueryType.Group,
                            { filterChanged(NewFilter.Query("", QueryType.Group)) },
                            stringResource(id = R.string.scanlator_group),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = paddingModifier
                    .fillMaxWidth()
                    .padding(bottom = bottomPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = resetClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor),
                ) {
                    Text(text = stringResource(id = R.string.reset), style = MaterialTheme.typography.titleSmall)
                }

                AnimatedVisibility(nameOfEnabledFilter.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    TextButton(onClick = { showSaveFilterDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = themeColorState.buttonColor)) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Gap(4.dp)
                        Text(text = stringResource(id = R.string.save), style = MaterialTheme.typography.titleSmall)
                    }
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

private fun <T> LazyListScope.filterRow(
    items: List<T>,
    expanded: Boolean,
    disabled: Boolean,
    headerClicked: () -> Unit,
    @StringRes headerRes: Int,
    onClick: (T) -> Unit,
    selected: (T) -> Boolean,
    nameRes: ((T) -> Int)? = null,
    name: ((T) -> String)? = null,
) {

    item {
        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth(),
        ) {
            ExpandableRow(
                isExpanded = expanded,
                disabled = disabled,
                onClick = headerClicked,
                rowText = stringResource(id = headerRes),
            )
            val density = LocalDensity.current

            AnimatedVisibility(
                visible = expanded,
                enter = slideInVertically {
                    // Slide in from 40 dp from the top.
                    with(density) { -60.dp.roundToPx() }
                } + expandVertically(
                    // Expand from the top.
                    clip = true,
                    expandFrom = Alignment.Top,
                ) + fadeIn(),
                exit = slideOutVertically() + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    mainAxisSpacing = 8.dp,
                    mainAxisAlignment = MainAxisAlignment.Start,
                ) {

                    items.forEach { item ->
                        val itemName = when {
                            nameRes != null -> stringResource(id = nameRes(item))
                            name != null -> name(item)
                            else -> ""
                        }
                        FilterChipWrapper(selected = selected(item), onClick = { onClick(item) }, name = itemName)
                    }
                }
            }
        }
    }
}

private fun <T> LazyListScope.filterTriStateRow(
    items: List<T>,
    expanded: Boolean,
    disabled: Boolean,
    headerClicked: () -> Unit,
    @StringRes headerRes: Int,
    toggleState: (ToggleableState, T) -> Unit,
    selected: (T) -> ToggleableState,
    nameRes: ((T) -> Int)? = null,
    name: ((T) -> String)? = null,
) {

    item {
        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth(),
        ) {
            ExpandableRow(
                isExpanded = expanded,
                disabled = disabled,
                onClick = headerClicked,
                rowText = stringResource(id = headerRes),
            )
            val density = LocalDensity.current

            AnimatedVisibility(
                visible = expanded,
                enter = slideInVertically {
                    // Slide in from 40 dp from the top.
                    with(density) { -60.dp.roundToPx() }
                } + expandVertically(
                    // Expand from the top.
                    clip = true,
                    expandFrom = Alignment.Top,
                ) + fadeIn(),
                exit = slideOutVertically() + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    mainAxisSpacing = 8.dp,
                    mainAxisAlignment = MainAxisAlignment.Start,
                ) {

                    items.forEach { item ->
                        val itemName = when {
                            nameRes != null -> stringResource(id = nameRes(item))
                            name != null -> name(item)
                            else -> ""
                        }
                        TriStateFilterChip(state = selected(item), toggleState = { state -> toggleState(state, item) }, name = itemName)
                    }
                }
            }
        }
    }
}


