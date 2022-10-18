package org.nekomanga.presentation.components.sheets

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.source.online.utils.MdSort
import eu.kanade.tachiyomi.util.lang.isUUID
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
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
    defaultContentRatings: ImmutableSet<String>,
    savedFilters: ImmutableList<BrowseFilterImpl>,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColorState.rippleTheme) {

        BaseSheet(themeColor = themeColorState, minSheetHeightPercentage = .75f, maxSheetHeightPercentage = 1f, bottomPaddingAroundContent = 0.dp) {

            val paddingModifier = Modifier.padding(horizontal = 8.dp)

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(weight = 1f, fill = false),
            ) {

                FilterRow(
                    items = filters.originalLanguage.toImmutableList(),
                    expanded = originalLanguageExpanded,
                    disabled = disabled,
                    headerClicked = { originalLanguageExpanded = !originalLanguageExpanded },
                    headerRes = R.string.original_language,
                    anyEnabled = filters.originalLanguage.any { it.state },
                    onClick = { originalLanguage -> filterChanged(originalLanguage.copy(state = !originalLanguage.state)) },
                    selected = { originalLanguage -> originalLanguage.state },
                    name = { originalLanguage -> originalLanguage.language.prettyPrint },
                )

                FilterRow(
                    items = filters.contentRatings.toImmutableList(),
                    expanded = contentRatingExpanded,
                    disabled = disabled,
                    headerClicked = { contentRatingExpanded = !contentRatingExpanded },
                    headerRes = R.string.content_rating,
                    anyEnabled = filters.contentRatings.any { (it.rating.key in defaultContentRatings && !it.state) || it.rating.key !in defaultContentRatings && it.state },
                    onClick = { rating -> filterChanged(rating.copy(state = !rating.state)) },
                    selected = { rating -> rating.state },
                    nameRes = { rating -> rating.rating.nameRes },
                )

                FilterRow(
                    items = filters.publicationDemographics.toImmutableList(),
                    expanded = publicationDemographicExpanded,
                    disabled = disabled,
                    headerClicked = { publicationDemographicExpanded = !publicationDemographicExpanded },
                    headerRes = R.string.publication_demographic,
                    anyEnabled = filters.publicationDemographics.any { it.state },
                    onClick = { demo -> filterChanged(demo.copy(state = !demo.state)) },
                    selected = { demo -> demo.state },
                    nameRes = { demo -> demo.demographic.nameRes },
                )

                FilterRow(
                    items = filters.statuses.toImmutableList(),
                    expanded = statusExpanded,
                    disabled = disabled,
                    headerClicked = { statusExpanded = !statusExpanded },
                    headerRes = R.string.status,
                    anyEnabled = filters.statuses.any { it.state },
                    onClick = { status -> filterChanged(status.copy(state = !status.state)) },
                    selected = { status -> status.state },
                    nameRes = { status -> status.status.statusRes },
                )


                FilterRow(
                    items = filters.sort.toImmutableList(),
                    expanded = sortExpanded,
                    disabled = disabled,
                    headerClicked = { sortExpanded = !sortExpanded },
                    headerRes = R.string.sort,
                    anyEnabled = filters.sort.any { it.state && it.sort != MdSort.Best },
                    onClick = { sort -> filterChanged(sort.copy(state = !sort.state)) },
                    selected = { sort -> sort.state },
                    name = { sort -> sort.sort.displayName },
                )

                FilterTriStateRow(
                    items = filters.tags.toImmutableList(),
                    expanded = tagExpanded,
                    disabled = disabled,
                    headerClicked = { tagExpanded = !tagExpanded },
                    headerRes = R.string.tag,
                    anyEnabled = filters.tags.any { it.state != ToggleableState.Off },
                    toggleState = { newState, tag -> filterChanged(tag.copy(state = newState)) },
                    selected = { tag -> tag.state },
                    name = { tag -> tag.tag.prettyPrint },
                )

                OtherRow(
                    isExpanded = otherExpanded,
                    disabled = disabled,
                    themeColorState = themeColorState,
                    onHeaderClick = { otherExpanded = !otherExpanded },
                    filters = filters,
                    anyEnabled = (filters.tagExclusionMode != NewFilter.TagExclusionMode() ||
                        filters.tagInclusionMode != NewFilter.TagInclusionMode() ||
                        filters.hasAvailableChapters != NewFilter.HasAvailableChapters() ||
                        filters.authorId.uuid.isNotBlank() ||
                        filters.groupId.uuid.isNotBlank()),

                    filterChanged = filterChanged,
                    filterClick = filterClick,
                )

                SavedFilters(
                    visible = savedFilters.isNotEmpty(),
                    savedFilters = savedFilters,
                    nameOfEnabledFilter = nameOfEnabledFilter,
                    themeColorState = themeColorState,
                    loadFilter = loadFilter,
                    deleteFilterClick = deleteFilterClick,
                    filterDefaultClick = filterDefaultClick,
                )

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

            Gap(8.dp)

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

@Composable
private fun <T> FilterRow(
    items: ImmutableList<T>,
    expanded: Boolean,
    disabled: Boolean,
    anyEnabled: Boolean,
    headerClicked: () -> Unit,
    @StringRes headerRes: Int,
    onClick: (T) -> Unit,
    selected: (T) -> Boolean,
    nameRes: ((T) -> Int)? = null,
    name: ((T) -> String)? = null,
) {
    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth(),
    ) {
        ExpandableRow(
            isExpanded = expanded,
            disabled = disabled,
            onClick = headerClicked,
            textColor = if (anyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            rowText = stringResource(id = headerRes),
        )

        AnimatedVisibility(
            visible = expanded,
            enter = slideEnter(),
            exit = slideExit(),
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

@Composable

private fun <T> FilterTriStateRow(
    items: ImmutableList<T>,
    expanded: Boolean,
    disabled: Boolean,
    headerClicked: () -> Unit,
    anyEnabled: Boolean,
    @StringRes headerRes: Int,
    toggleState: (ToggleableState, T) -> Unit,
    selected: (T) -> ToggleableState,
    nameRes: ((T) -> Int)? = null,
    name: ((T) -> String)? = null,
) {

    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth(),
    ) {
        ExpandableRow(
            isExpanded = expanded,
            disabled = disabled,
            onClick = headerClicked,
            textColor = if (anyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            rowText = stringResource(id = headerRes),
        )

        AnimatedVisibility(
            visible = expanded,
            enter = slideEnter(),
            exit = slideExit(),
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

@Composable
fun OtherRow(
    isExpanded: Boolean,
    disabled: Boolean,
    themeColorState: ThemeColorState,
    onHeaderClick: () -> Unit,
    anyEnabled: Boolean,
    filters: DexFilters,
    filterChanged: (NewFilter) -> Unit,
    filterClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth(),
    ) {
        ExpandableRow(
            isExpanded = isExpanded,
            disabled = disabled,
            onClick = onHeaderClick,
            textColor = if (anyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            rowText = stringResource(id = R.string.other),
        )
        AnimatedVisibility(visible = isExpanded, enter = slideEnter(), exit = slideExit()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                CheckboxRow(
                    checkedState = filters.hasAvailableChapters.state,
                    checkedChange = { newState -> filterChanged(filters.hasAvailableChapters.copy(state = newState)) },
                    rowText = stringResource(
                        id = R.string.has_available_chapters,
                    ),
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.tag_inclusion_mode), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = filters.tagInclusionMode.mode == TagMode.And,
                            onClick = { filterChanged(filters.tagInclusionMode.copy(mode = TagMode.And)) },
                        )
                        Text(text = stringResource(id = R.string.and), color = MaterialTheme.colorScheme.onSurface)
                        RadioButton(
                            selected = filters.tagInclusionMode.mode == TagMode.Or,
                            onClick = { filterChanged(filters.tagInclusionMode.copy(mode = TagMode.Or)) },
                        )
                        Text(text = stringResource(id = R.string.or), color = MaterialTheme.colorScheme.onSurface)
                    }
                }


                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.tag_exclusion_mode), modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = filters.tagExclusionMode.mode == TagMode.And,
                            onClick = { filterChanged(filters.tagExclusionMode.copy(mode = TagMode.And)) },
                        )
                        Text(text = stringResource(id = R.string.and), color = MaterialTheme.colorScheme.onSurface)
                        RadioButton(
                            selected = filters.tagExclusionMode.mode == TagMode.Or,
                            onClick = { filterChanged(filters.tagExclusionMode.copy(mode = TagMode.Or)) },
                        )
                        Text(text = stringResource(id = R.string.or), color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                val groupIdError = remember(filters.groupId.uuid) {
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
                    isError = groupIdError,
                    textChanged = { text: String -> filterChanged(NewFilter.GroupId(text)) },
                    search = { filterClick() },
                )
                Gap(4.dp)

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
    }
}

@Composable
fun SavedFilters(
    visible: Boolean,
    savedFilters: ImmutableList<BrowseFilterImpl>,
    nameOfEnabledFilter: String,
    themeColorState: ThemeColorState,
    loadFilter: (BrowseFilterImpl) -> Unit,
    deleteFilterClick: (String) -> Unit,
    filterDefaultClick: (String, Boolean) -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = slideEnter(), exit = slideExit()) {
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

                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
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

private fun slideEnter(): EnterTransition {
    return slideInVertically() + expandVertically(
        // Expand from the top.
        clip = true,
        expandFrom = Alignment.Top,
    ) + fadeIn()
}

private fun slideExit(): ExitTransition {
    return slideOutVertically { it / 3 } + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
}



