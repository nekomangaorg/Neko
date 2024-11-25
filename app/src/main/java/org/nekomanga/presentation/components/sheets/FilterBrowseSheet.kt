package org.nekomanga.presentation.components.sheets

import ToolTipButton
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.source.online.utils.MdSort
import eu.kanade.tachiyomi.util.lang.isUUID
import jp.wasabeef.gap.Gap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.nekomanga.R
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.Filter
import org.nekomanga.domain.filter.QueryType
import org.nekomanga.domain.filter.TagMode
import org.nekomanga.presentation.components.CheckboxRow
import org.nekomanga.presentation.components.ExpandableRow
import org.nekomanga.presentation.components.FilterChipWrapper
import org.nekomanga.presentation.components.SearchFooter
import org.nekomanga.presentation.components.TriStateFilterChip
import org.nekomanga.presentation.components.dialog.SaveFilterDialog
import org.nekomanga.presentation.components.sheetHandle
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Shapes
import org.nekomanga.presentation.theme.Size

@Composable
fun FilterBrowseSheet(
    filters: DexFilters,
    filterClick: () -> Unit,
    saveClick: (String) -> Unit,
    resetClick: () -> Unit,
    deleteFilterClick: (String) -> Unit,
    filterDefaultClick: (String, Boolean) -> Unit,
    loadFilter: (BrowseFilterImpl) -> Unit,
    filterChanged: (Filter) -> Unit,
    defaultContentRatings: ImmutableSet<String>,
    savedFilters: ImmutableList<BrowseFilterImpl>,
    bottomContentPadding: Dp = 16.dp,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration
    ) {
        val paddingModifier = Modifier.padding(horizontal = Size.small)

        var originalLanguageExpanded by remember { mutableStateOf(false) }
        var contentRatingExpanded by remember { mutableStateOf(false) }
        var publicationDemographicExpanded by remember { mutableStateOf(false) }
        var statusExpanded by remember { mutableStateOf(false) }
        var sortExpanded by remember { mutableStateOf(false) }
        var tagExpanded by remember { mutableStateOf(false) }
        var otherExpanded by remember { mutableStateOf(false) }

        var showSaveFilterDialog by remember { mutableStateOf(false) }

        val nameOfEnabledFilter by
            rememberSaveable(filters, savedFilters) {
                mutableStateOf(
                    savedFilters
                        .firstOrNull { Json.decodeFromString<DexFilters>(it.dexFilters) == filters }
                        ?.name ?: ""
                )
            }

        val disabled by
            remember(filters.queryMode) { mutableStateOf(filters.queryMode != QueryType.Title) }

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
            SaveFilterDialog(
                themeColorState = themeColorState,
                currentSavedFilters = savedFilters,
                onDismiss = { showSaveFilterDialog = false },
                onConfirm = { saveClick(it) },
            )
        }

        var queryText by remember { mutableStateOf(filters.query.text) }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topEnd = Shapes.sheetRadius, topStart = Shapes.sheetRadius),
        ) {
            Column(
                modifier =
                    paddingModifier
                        .verticalScroll(rememberScrollState())
                        .weight(weight = 1f, fill = false)
            ) {
                sheetHandle()
                Gap(16.dp)
                val titleRes =
                    when (filters.queryMode) {
                        QueryType.Title -> {
                            R.string.title
                        }
                        QueryType.Author -> {
                            R.string.author
                        }
                        QueryType.Group -> {
                            R.string.scanlator_group
                        }
                        QueryType.List -> {
                            R.string.list_id
                        }
                    }

                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Size.small),
                ) {
                    FilterChipWrapper(
                        filters.queryMode == QueryType.Title,
                        {
                            queryText = ""
                            filterChanged(Filter.Query("", QueryType.Title))
                        },
                        stringResource(id = R.string.title),
                    )
                    FilterChipWrapper(
                        filters.queryMode == QueryType.Author,
                        {
                            queryText = ""
                            filterChanged(Filter.Query("", QueryType.Author))
                        },
                        stringResource(id = R.string.author),
                    )
                    FilterChipWrapper(
                        filters.queryMode == QueryType.Group,
                        {
                            queryText = ""
                            filterChanged(Filter.Query("", QueryType.Group))
                        },
                        stringResource(id = R.string.scanlator_group),
                    )
                    FilterChipWrapper(
                        filters.queryMode == QueryType.List,
                        {
                            queryText = ""
                            filterChanged(Filter.Query("", QueryType.List))
                        },
                        stringResource(id = R.string.list_id),
                    )
                }

                val isError =
                    remember(filters.query.text) {
                        if (filters.queryMode != QueryType.List || filters.query.text.isBlank()) {
                            false
                        } else {
                            !filters.query.text.isUUID()
                        }
                    }

                SearchFooter(
                    themeColorState = themeColorState,
                    labelText = stringResource(id = titleRes),
                    showDivider = false,
                    title = queryText,
                    isError = isError,
                    textChanged = { text: String ->
                        queryText = text
                        filterChanged(filters.query.copy(text = text))
                    },
                    search = { filterClick() },
                )

                FilterRow(
                    items = filters.originalLanguage.toImmutableList(),
                    expanded = originalLanguageExpanded,
                    disabled = disabled,
                    headerClicked = { originalLanguageExpanded = !originalLanguageExpanded },
                    headerRes = R.string.original_language,
                    anyEnabled = filters.originalLanguage.any { it.state },
                    onClick = { originalLanguage ->
                        filterChanged(originalLanguage.copy(state = !originalLanguage.state))
                    },
                    selected = { originalLanguage -> originalLanguage.state },
                    name = { originalLanguage -> originalLanguage.language.prettyPrint },
                )

                if (filters.contentRatingVisible) {
                    FilterRow(
                        items = filters.contentRatings.toImmutableList(),
                        expanded = contentRatingExpanded,
                        disabled = disabled,
                        headerClicked = { contentRatingExpanded = !contentRatingExpanded },
                        headerRes = R.string.content_rating,
                        anyEnabled =
                            filters.contentRatings.any {
                                (it.rating.key in defaultContentRatings && !it.state) ||
                                    it.rating.key !in defaultContentRatings && it.state
                            },
                        onClick = { rating -> filterChanged(rating.copy(state = !rating.state)) },
                        selected = { rating -> rating.state },
                        nameRes = { rating -> rating.rating.nameRes },
                    )
                }

                FilterRow(
                    items = filters.publicationDemographics.toImmutableList(),
                    expanded = publicationDemographicExpanded,
                    disabled = disabled,
                    headerClicked = {
                        publicationDemographicExpanded = !publicationDemographicExpanded
                    },
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
                    anyEnabled =
                        (filters.tagExclusionMode != Filter.TagExclusionMode() ||
                            filters.tagInclusionMode != Filter.TagInclusionMode() ||
                            filters.hasAvailableChapters != Filter.HasAvailableChapters() ||
                            filters.authorId.uuid.isNotBlank() ||
                            filters.groupId.uuid.isNotBlank()),
                    filterChanged = filterChanged,
                    filterClick = filterClick,
                )
            }

            Gap(Size.tiny)

            SavedFilters(
                visible = savedFilters.isNotEmpty(),
                savedFilters = savedFilters,
                nameOfEnabledFilter = nameOfEnabledFilter,
                loadFilter = loadFilter,
                deleteFilterClick = deleteFilterClick,
                filterDefaultClick = filterDefaultClick,
            )

            Gap(Size.small)

            Row(
                modifier = paddingModifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = {
                        queryText = ""
                        resetClick()
                    },
                    shape = RoundedCornerShape(Size.extraLarge),
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                    Gap(Size.tiny)
                    Text(
                        text = stringResource(id = R.string.reset),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                AnimatedVisibility(
                    nameOfEnabledFilter.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    TextButton(
                        onClick = { showSaveFilterDialog = true },
                        shape = RoundedCornerShape(Size.extraLarge),
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Gap(Size.tiny)
                        Text(
                            text = stringResource(id = R.string.save),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }

                ElevatedButton(
                    onClick = filterClick,
                    shape = RoundedCornerShape(Size.extraLarge),
                    colors =
                        ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                    Gap(Size.tiny)
                    Text(
                        text = stringResource(id = R.string.filter),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Gap(bottomContentPadding + Size.small)
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
    modifier: Modifier = Modifier,
    nameRes: ((T) -> Int)? = null,
    name: ((T) -> String)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ExpandableRow(
            isExpanded = expanded,
            disabled = disabled,
            onClick = headerClicked,
            textColor =
                if (anyEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            rowText = stringResource(id = headerRes),
        )

        AnimatedVisibility(visible = expanded, enter = slideEnter(), exit = slideExit()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
                horizontalArrangement = Arrangement.spacedBy(Size.small, Alignment.Start),
            ) {
                items.forEach { item ->
                    val itemName =
                        when {
                            nameRes != null -> stringResource(id = nameRes(item))
                            name != null -> name(item)
                            else -> ""
                        }
                    FilterChipWrapper(
                        selected = selected(item),
                        onClick = { onClick(item) },
                        name = itemName,
                    )
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
    modifier: Modifier = Modifier,
    nameRes: ((T) -> Int)? = null,
    name: ((T) -> String)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ExpandableRow(
            isExpanded = expanded,
            disabled = disabled,
            onClick = headerClicked,
            textColor =
                if (anyEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            rowText = stringResource(id = headerRes),
        )

        AnimatedVisibility(visible = expanded, enter = slideEnter(), exit = slideExit()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
                horizontalArrangement = Arrangement.spacedBy(Size.small, Alignment.Start),
            ) {
                items.forEach { item ->
                    val itemName =
                        when {
                            nameRes != null -> stringResource(id = nameRes(item))
                            name != null -> name(item)
                            else -> ""
                        }
                    TriStateFilterChip(
                        state = selected(item),
                        toggleState = { state -> toggleState(state, item) },
                        name = itemName,
                    )
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
    filterChanged: (Filter) -> Unit,
    filterClick: () -> Unit,
) {
    Column(modifier = Modifier.imePadding().fillMaxWidth()) {
        ExpandableRow(
            isExpanded = isExpanded,
            disabled = disabled,
            onClick = onHeaderClick,
            textColor =
                if (anyEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            rowText = stringResource(id = R.string.other),
        )
        AnimatedVisibility(visible = isExpanded, enter = slideEnter(), exit = slideExit()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CheckboxRow(
                    checkedState = filters.hasAvailableChapters.state,
                    checkedChange = { newState ->
                        filterChanged(filters.hasAvailableChapters.copy(state = newState))
                    },
                    rowText = stringResource(id = R.string.has_available_chapters),
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.tag_inclusion_mode),
                        modifier = Modifier.padding(start = Size.small),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = filters.tagInclusionMode.mode == TagMode.And,
                            onClick = {
                                filterChanged(filters.tagInclusionMode.copy(mode = TagMode.And))
                            },
                        )
                        Text(
                            text = stringResource(id = R.string.and),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        RadioButton(
                            selected = filters.tagInclusionMode.mode == TagMode.Or,
                            onClick = {
                                filterChanged(filters.tagInclusionMode.copy(mode = TagMode.Or))
                            },
                        )
                        Text(
                            text = stringResource(id = R.string.or),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.tag_exclusion_mode),
                        modifier = Modifier.padding(start = Size.small),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = filters.tagExclusionMode.mode == TagMode.And,
                            onClick = {
                                filterChanged(filters.tagExclusionMode.copy(mode = TagMode.And))
                            },
                        )
                        Text(
                            text = stringResource(id = R.string.and),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        RadioButton(
                            selected = filters.tagExclusionMode.mode == TagMode.Or,
                            onClick = {
                                filterChanged(filters.tagExclusionMode.copy(mode = TagMode.Or))
                            },
                        )
                        Text(
                            text = stringResource(id = R.string.or),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                val groupIdError =
                    remember(filters.groupId.uuid) {
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
                    textChanged = { text: String -> filterChanged(Filter.GroupId(text)) },
                    search = { filterClick() },
                )
                Gap(Size.tiny)

                val isError =
                    remember(filters.authorId.uuid) {
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
                    textChanged = { text: String -> filterChanged(Filter.AuthorId(text)) },
                    search = { filterClick() },
                )
                Gap(Size.tiny)
            }
        }
    }
}

@Composable
fun SavedFilters(
    visible: Boolean,
    savedFilters: ImmutableList<BrowseFilterImpl>,
    nameOfEnabledFilter: String,
    loadFilter: (BrowseFilterImpl) -> Unit,
    deleteFilterClick: (String) -> Unit,
    filterDefaultClick: (String, Boolean) -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = slideEnter(), exit = slideExit()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val sortedFilters by
                remember(nameOfEnabledFilter) {
                    val enabledFilterIndex =
                        savedFilters.indexOfFirst { nameOfEnabledFilter.equals(it.name, true) }
                    if (enabledFilterIndex == -1) {
                        mutableStateOf(savedFilters)
                    } else {
                        val mutableFilters = savedFilters.toMutableList()
                        val enabledFilter = mutableFilters.removeAt(enabledFilterIndex)
                        mutableStateOf(
                            persistentListOf(enabledFilter) + mutableFilters.toImmutableList()
                        )
                    }
                }
            val listState: LazyListState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            LazyRow(verticalAlignment = Alignment.CenterVertically, state = listState) {
                item { Gap(Size.tiny) }

                items(sortedFilters) { filter ->
                    val isEnabled = nameOfEnabledFilter.equals(filter.name, true)
                    FilterChipWrapper(
                        modifier = Modifier.animateItemPlacement(),
                        selected = isEnabled,
                        onClick = {
                            scope.launch { listState.animateScrollToItem(0) }
                            loadFilter(filter)
                        },
                        name = filter.name,
                    )
                    // AnimatedVisibility(visible = isEnabled, enter = slideInHorizontally() +
                    // fadeIn(), exit = slideOutHorizontally() + fadeOut()) {
                    if (isEnabled) {
                        Row(modifier = Modifier.animateItemPlacement()) {
                            ToolTipButton(
                                toolTipLabel = stringResource(id = R.string.delete_filter),
                                icon = Icons.Outlined.Delete,
                                buttonClicked = { deleteFilterClick(nameOfEnabledFilter) },
                            )
                            val isDefault =
                                savedFilters
                                    .firstOrNull { nameOfEnabledFilter.equals(it.name, true) }
                                    ?.default ?: false
                            val (textRes, makeDefault, icon) =
                                when (isDefault) {
                                    true ->
                                        Triple(
                                            R.string.remove_default,
                                            false,
                                            Icons.Default.HeartBroken,
                                        )
                                    false ->
                                        Triple(R.string.make_default, true, Icons.Default.Favorite)
                                }
                            ToolTipButton(
                                toolTipLabel = stringResource(textRes),
                                icon = icon,
                                buttonClicked = {
                                    filterDefaultClick(nameOfEnabledFilter, makeDefault)
                                },
                            )
                        }
                    }
                    Gap(Size.tiny)
                }
                item { Gap(Size.tiny) }
            }
        }
    }
}

private fun slideEnter(): EnterTransition {
    return slideInVertically() +
        expandVertically(
            // Expand from the top.
            clip = true,
            expandFrom = Alignment.Top,
        ) +
        fadeIn()
}

private fun slideExit(): ExitTransition {
    return slideOutVertically { it / 3 } +
        shrinkVertically(shrinkTowards = Alignment.Top) +
        fadeOut()
}
