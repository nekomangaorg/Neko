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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
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
    incognitoMode: Boolean = false,
    isRoot: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(state = rememberTopAppBarState()),
    navigationIcon: ImageVector = Icons.Filled.ArrowBack,
    navigationIconLabel: String = stringResource(id = R.string.back),
    onSearch: (String?) -> Unit = {},
    snackBarHost: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit = {},
) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() > .5
    val color = getTopAppBarColor(title)
    SideEffect { systemUiController.setStatusBarColor(color, darkIcons = useDarkIcons) }
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
                            color,
                            title,
                            navigationIconLabel,
                            navigationIcon,
                            onNavigationIconClicked,
                            actions,
                            incognitoMode,
                            isRoot,
                            scrollBehavior,
                        )
                    NekoScaffoldType.NoTitle ->
                        NoTitleTopAppBar(
                            color,
                            navigationIconLabel,
                            navigationIcon,
                            onNavigationIconClicked,
                            actions,
                            scrollBehavior,
                        )
                    NekoScaffoldType.TitleAndSubtitle ->
                        TitleAndSubtitleTopAppBar(
                            color,
                            title,
                            subtitle,
                            navigationIconLabel,
                            navigationIcon,
                            onNavigationIconClicked,
                            actions,
                            scrollBehavior,
                        )
                    NekoScaffoldType.Search ->
                        NoTitleSearchTopAppBar(
                            onSearch,
                            searchPlaceHolder,
                            color,
                            navigationIconLabel,
                            navigationIcon,
                            onNavigationIconClicked,
                            actions,
                            scrollBehavior,
                        )
                    NekoScaffoldType.SearchOutline ->
                        SearchOutlineTopAppBar(
                            onSearch,
                            searchPlaceHolder,
                            color,
                            actions,
                            scrollBehavior,
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
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchEnabled by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    TopAppBar(
        title = {},
        modifier = Modifier.statusBarsPadding(),
        navigationIcon = {},
        actions = {
            SearchBar(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(Size.small)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                searchEnabled = true
                            }
                        }
                        .focusRequester(focusRequester),
                query = searchText,
                onQueryChange = {
                    searchText = it
                    onSearchText(it)
                },
                colors =
                    SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                onSearch = { onSearchText(it) },
                active = false,
                onActiveChange = {},
                leadingIcon = {
                    if (searchEnabled) {
                        ToolTipButton(
                            toolTipLabel = stringResource(id = R.string.cancel_search),
                            icon = Icons.Filled.SearchOff,
                            buttonClicked = {
                                onSearchText("")
                                searchText = ""
                                searchEnabled = false
                                focusManager.clearFocus()
                            },
                        )
                    } else {
                        ToolTipButton(
                            toolTipLabel = stringResource(id = R.string.search),
                            icon = Icons.Filled.Search,
                            buttonClicked = { searchEnabled = true },
                        )
                    }
                },
                placeholder = {
                    Text(
                        text = searchPlaceHolder,
                        color =
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = NekoColors.mediumAlphaHighContrast
                            ),
                    )
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
                    keyboardOptions =
                        KeyboardOptions.Default.copy(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
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
            actions()
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
fun getTopAppBarColor(title: String): Color {
    return when (title.isEmpty()) {
        true -> Color.Transparent
        false -> MaterialTheme.colorScheme.surface.copy(alpha = .7f)
    }
}

enum class NekoScaffoldType {
    TitleAndSubtitle,
    Title,
    NoTitle,
    Search,
    SearchOutline,
}
