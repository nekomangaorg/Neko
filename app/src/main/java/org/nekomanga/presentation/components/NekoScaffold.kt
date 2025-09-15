@file:OptIn(ExperimentalComposeUiApi::class)

package org.nekomanga.presentation.components

import ToolTipButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canlioya.appbarlayoutcompose.FlexibleTopBar
import com.canlioya.appbarlayoutcompose.FlexibleTopBarColors
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.screens.ThemeColorState
import org.nekomanga.presentation.screens.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun NekoScaffold(
    type: NekoScaffoldType,
    onNavigationIconClicked: () -> Unit = {},
    modifier: Modifier = Modifier,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    title: String = "",
    subtitle: String = "",
    searchPlaceHolder: String = "",
    searchPlaceHolderAlt: String = "",
    incognitoMode: Boolean = false,
    isRoot: Boolean = false,
    altAppBarColor: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState()),
    focusRequester: FocusRequester = remember { FocusRequester() },
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navigationIconLabel: String = stringResource(id = R.string.back),
    searchNavigationEnabled: Boolean = false,
    onSearch: (String?) -> Unit = {},
    onSearchEnabled: () -> Unit = {},
    onSearchDisabled: () -> Unit = {},
    snackBarHost: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    underHeaderActions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit = {},
) {
    val systemUiController = rememberSystemUiController()
    val color = getTopAppBarColor(title, altAppBarColor)
    val useDarkIcons = color.luminance() > .5
    LaunchedEffect(key1 = color, useDarkIcons) {
        systemUiController.setStatusBarColor(color, darkIcons = useDarkIcons)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = snackBarHost,
        topBar = {
            CompositionLocalProvider(
                LocalRippleConfiguration provides themeColorState.rippleConfiguration
            ) {
                when (type) {
                    NekoScaffoldType.Title ->
                        TitleOnlyTopAppBar(
                            color = color,
                            title = title,
                            navigationIconLabel = navigationIconLabel,
                            navigationIcon = navigationIcon,
                            onNavigationIconClicked = onNavigationIconClicked,
                            actions = actions,
                            incognitoMode = incognitoMode,
                            isRoot = isRoot,
                            scrollBehavior = scrollBehavior,
                        )
                    NekoScaffoldType.NoTitle ->
                        NoTitleTopAppBar(
                            color = color,
                            navigationIconLabel = navigationIconLabel,
                            navigationIcon = navigationIcon,
                            onNavigationIconClicked = onNavigationIconClicked,
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                    NekoScaffoldType.TitleAndSubtitle ->
                        TitleAndSubtitleTopAppBar(
                            color = color,
                            title = title,
                            subtitle = subtitle,
                            navigationIconLabel = navigationIconLabel,
                            navigationIcon = navigationIcon,
                            onNavigationIconClicked = onNavigationIconClicked,
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                    NekoScaffoldType.Search ->
                        NoTitleSearchTopAppBar(
                            onSearchText = onSearch,
                            searchPlaceHolder = searchPlaceHolder,
                            color = color,
                            navigationIconLabel = navigationIconLabel,
                            navigationIcon = navigationIcon,
                            onNavigationIconClicked = onNavigationIconClicked,
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                    NekoScaffoldType.SearchOutline ->
                        SearchOutlineTopAppBar(
                            onSearchText = onSearch,
                            searchPlaceHolder = searchPlaceHolder,
                            color = color,
                            navigationEnabled = searchNavigationEnabled,
                            navigationIconLabel = navigationIconLabel,
                            navigationIcon = navigationIcon,
                            focusRequester = focusRequester,
                            onNavigationIconClicked = onNavigationIconClicked,
                            onSearchDisabled = onSearchDisabled,
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                    NekoScaffoldType.SearchOutlineDummy ->
                        SearchOutlineDummyTopAppBar(
                            searchPlaceHolder = searchPlaceHolder,
                            color = color,
                            onSearchEnabled = onSearchEnabled,
                            navigationEnabled = searchNavigationEnabled,
                            navigationIconLabel = navigationIconLabel,
                            navigationIcon = navigationIcon,
                            focusRequester = focusRequester,
                            onNavigationIconClicked = onNavigationIconClicked,
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                        )
                    NekoScaffoldType.SearchOutlineWithActions ->
                        SearchOutlineWithActionsTopAppBar(
                            onSearchText = onSearch,
                            searchPlaceHolder = searchPlaceHolder,
                            searchPlaceHolderAlt = searchPlaceHolderAlt,
                            color = color,
                            navigationEnabled = searchNavigationEnabled,
                            navigationIconLabel = navigationIconLabel,
                            navigationIcon = navigationIcon,
                            focusRequester = focusRequester,
                            onNavigationIconClicked = onNavigationIconClicked,
                            onSearchDisabled = onSearchDisabled,
                            actions = actions,
                            scrollBehavior = scrollBehavior,
                            underHeaderActions = underHeaderActions,
                        )
                }
            }
        },
    ) { paddingValues ->
        CompositionLocalProvider(
            LocalRippleConfiguration provides
                nekoRippleConfiguration(MaterialTheme.colorScheme.primary)
        ) {
            content(paddingValues)
        }
    }
}

@Composable
private fun TitleAndSubtitleTopAppBar(
    color: Color,
    title: String,
    subtitle: String,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    onNavigationIconClicked: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        modifier = Modifier.statusBarsPadding(),
        navigationIcon = {
            ToolTipButton(
                toolTipLabel = navigationIconLabel,
                icon = navigationIcon,
                buttonClicked = onNavigationIconClicked,
            )
        },
        actions = actions,
        colors = topAppBarColors(containerColor = color, scrolledContainerColor = color),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun NoTitleTopAppBar(
    color: Color,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    onNavigationIconClicked: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {},
        modifier = Modifier.statusBarsPadding(),
        navigationIcon = {
            ToolTipButton(
                toolTipLabel = navigationIconLabel,
                icon = navigationIcon,
                buttonClicked = onNavigationIconClicked,
            )
        },
        actions = actions,
        colors = topAppBarColors(containerColor = color, scrolledContainerColor = color),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun SearchOutlineTopAppBar(
    onSearchText: (String?) -> Unit,
    searchPlaceHolder: String,
    color: Color,
    navigationEnabled: Boolean,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    focusRequester: FocusRequester,
    onNavigationIconClicked: () -> Unit,
    onSearchDisabled: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchEnabled by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    TopAppBar(
        title = {},
        modifier = Modifier.statusBarsPadding(),
        navigationIcon = {},
        actions = {
            SearchBar(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Size.small)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                searchEnabled = true
                            }
                        }
                        .focusRequester(focusRequester),
                expanded = false,
                onExpandedChange = {},
                colors =
                    SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchText,
                        expanded = false,
                        onExpandedChange = {},
                        onQueryChange = {
                            searchText = it
                            onSearchText(it)
                        },
                        onSearch = { onSearchText(it) },
                        placeholder = {
                            Text(
                                text = searchPlaceHolder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingIcon = {
                            Row {
                                if (navigationEnabled) {
                                    ToolTipButton(
                                        toolTipLabel = navigationIconLabel,
                                        icon = navigationIcon,
                                        buttonClicked = onNavigationIconClicked,
                                    )
                                }
                                if (searchEnabled) {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.cancel_search),
                                        icon = Icons.Filled.SearchOff,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        buttonClicked = {
                                            onSearchText("")
                                            searchText = ""
                                            searchEnabled = false
                                            focusManager.clearFocus()
                                            onSearchDisabled()
                                        },
                                    )
                                } else {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.search),
                                        icon = Icons.Filled.Search,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        buttonClicked = { searchEnabled = true },
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            Row {
                                AnimatedVisibility(
                                    visible = searchText.isNotBlank(),
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.clear),
                                        icon = Icons.Filled.Close,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        buttonClicked = {
                                            onSearchText("")
                                            searchText = ""
                                        },
                                    )
                                }
                                if (!searchEnabled) {
                                    actions()
                                }
                            }
                        },
                    )
                },
                content = {},
            )
        },
        colors = topAppBarColors(containerColor = color, scrolledContainerColor = color),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun SearchOutlineWithActionsTopAppBar(
    onSearchText: (String?) -> Unit,
    searchPlaceHolder: String,
    searchPlaceHolderAlt: String,
    color: Color,
    navigationEnabled: Boolean,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    focusRequester: FocusRequester,
    onNavigationIconClicked: () -> Unit,
    onSearchDisabled: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    underHeaderActions: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchEnabled by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    FlexibleTopBar(
        modifier = Modifier.statusBarsPadding(),
        scrollBehavior = scrollBehavior,
        colors = FlexibleTopBarColors(containerColor = color, scrolledContainerColor = color),
    ) {
        Column {
            SearchBar(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Size.small)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                searchEnabled = true
                            }
                        }
                        .focusRequester(focusRequester),
                expanded = false,
                onExpandedChange = {},
                colors =
                    SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchText,
                        expanded = false,
                        onExpandedChange = {},
                        onQueryChange = {
                            searchText = it
                            onSearchText(it)
                        },
                        onSearch = { onSearchText(it) },
                        placeholder = {
                            Text(
                                text =
                                    if (searchEnabled && searchPlaceHolderAlt.isNotEmpty())
                                        searchPlaceHolderAlt
                                    else searchPlaceHolder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingIcon = {
                            Row {
                                if (navigationEnabled) {
                                    ToolTipButton(
                                        toolTipLabel = navigationIconLabel,
                                        icon = navigationIcon,
                                        buttonClicked = onNavigationIconClicked,
                                    )
                                }
                                if (searchEnabled) {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.cancel_search),
                                        icon = Icons.Filled.SearchOff,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        buttonClicked = {
                                            onSearchText("")
                                            searchText = ""
                                            searchEnabled = false
                                            focusManager.clearFocus()
                                            onSearchDisabled()
                                        },
                                    )
                                } else {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.search),
                                        icon = Icons.Filled.Search,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        buttonClicked = { searchEnabled = true },
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            Row {
                                AnimatedVisibility(
                                    visible = searchText.isNotBlank(),
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.clear),
                                        icon = Icons.Filled.Close,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        buttonClicked = {
                                            onSearchText("")
                                            searchText = ""
                                        },
                                    )
                                }
                                if (!searchEnabled) {
                                    actions()
                                }
                            }
                        },
                    )
                },
                content = {},
            )
            Gap(Size.tiny)
            underHeaderActions()
        }
    }
}

@Composable
fun SearchOutlineDummyTopAppBar(
    onSearchEnabled: () -> Unit,
    searchPlaceHolder: String,
    color: Color,
    navigationEnabled: Boolean,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    focusRequester: FocusRequester,
    onNavigationIconClicked: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {},
        modifier = Modifier.statusBarsPadding(),
        navigationIcon = {},
        actions = {
            SearchBar(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Size.small)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                onSearchEnabled()
                            }
                        }
                        .focusRequester(focusRequester),
                expanded = false,
                onExpandedChange = {},
                colors =
                    SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                inputField = {
                    SearchBarDefaults.InputField(
                        query = "",
                        expanded = false,
                        onExpandedChange = {},
                        onQueryChange = {},
                        onSearch = {},
                        placeholder = {
                            Text(
                                text = searchPlaceHolder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingIcon = {
                            Row {
                                if (navigationEnabled) {
                                    ToolTipButton(
                                        toolTipLabel = navigationIconLabel,
                                        icon = navigationIcon,
                                        buttonClicked = onNavigationIconClicked,
                                    )
                                } else {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.search),
                                        icon = Icons.Filled.Search,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        buttonClicked = { onSearchEnabled() },
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            Row {
                                if (navigationEnabled) {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.search),
                                        icon = Icons.Filled.Search,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        buttonClicked = { onSearchEnabled() },
                                    )
                                } else {
                                    actions()
                                }
                            }
                        },
                    )
                },
                content = {},
            )
        },
        colors = topAppBarColors(containerColor = color, scrolledContainerColor = color),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun NoTitleSearchTopAppBar(
    onSearchText: (String?) -> Unit,
    searchPlaceHolder: String,
    color: Color,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    onNavigationIconClicked: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var showTextField by rememberSaveable { mutableStateOf(false) }
    var alreadyRequestedFocus by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    TopAppBar(
        title = {},
        modifier = Modifier.statusBarsPadding(),
        navigationIcon = {
            ToolTipButton(
                toolTipLabel = navigationIconLabel,
                icon = navigationIcon,
                buttonClicked = onNavigationIconClicked,
            )
        },
        actions = {
            AnimatedVisibility(
                visible = showTextField,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
            ) {
                // research on configuration change

                OutlinedTextField(
                    modifier =
                        Modifier.weight(1f)
                            .padding(top = Size.small, bottom = Size.small, start = Size.extraLarge)
                            .focusRequester(focusRequester),
                    value = searchText,
                    placeholder = { Text(text = stringResource(id = R.string.search_chapters)) },
                    onValueChange = {
                        searchText = it
                        onSearchText(it)
                    },
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            cursorColor =
                                LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchText.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            ToolTipButton(
                                toolTipLabel = stringResource(id = R.string.clear),
                                icon = Icons.Filled.Close,
                                buttonClicked = {
                                    onSearchText("")
                                    searchText = ""
                                },
                            )
                        }
                    },
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchText(searchText) }),
                )
                LaunchedEffect(Unit) {
                    if (!alreadyRequestedFocus) {
                        focusRequester.requestFocus()
                        alreadyRequestedFocus = true
                    }
                    if (searchText.isNotBlank()) {
                        onSearchText(searchText)
                    }
                }
            }
            val icon =
                when (showTextField) {
                    true -> Icons.Filled.SearchOff
                    false -> Icons.Filled.Search
                }
            ToolTipButton(
                toolTipLabel = searchPlaceHolder,
                icon = icon,
                buttonClicked = {
                    searchText = ""
                    alreadyRequestedFocus = false
                    onSearchText(null)
                    showTextField = !showTextField
                },
            )
            Row { actions() }
        },
        colors = topAppBarColors(containerColor = color, scrolledContainerColor = color),
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun TitleOnlyTopAppBar(
    color: Color,
    title: String,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    onNavigationIconClicked: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    incognitoMode: Boolean,
    isRoot: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    CenterAlignedTopAppBar(
        colors = topAppBarColors(containerColor = color, scrolledContainerColor = color),
        modifier = Modifier.statusBarsPadding(),
        title = {
            AutoSizeText(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = (-.6).sp),
            )
        },
        navigationIcon = {
            if (incognitoMode) {
                Image(
                    CommunityMaterial.Icon2.cmd_incognito_circle,
                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                    modifier = Modifier.padding(start = 12.dp).size(32.dp),
                )
            } else if (!isRoot) {
                ToolTipButton(
                    toolTipLabel = navigationIconLabel,
                    icon = navigationIcon,
                    buttonClicked = onNavigationIconClicked,
                )
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun getTopAppBarColor(title: String, altAppBarColor: Boolean): Color {
    return when {
        title.isEmpty() && !altAppBarColor -> Color.Transparent
        title.isNotEmpty() && !altAppBarColor -> MaterialTheme.colorScheme.surface.copy(alpha = .7f)
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = .7f)
    }
}

enum class NekoScaffoldType {
    TitleAndSubtitle,
    Title,
    NoTitle,
    Search,
    SearchOutline,
    SearchOutlineDummy,
    SearchOutlineWithActions,
}
