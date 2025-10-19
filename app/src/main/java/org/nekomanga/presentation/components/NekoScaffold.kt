
package org.nekomanga.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.nekomanga.R
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.components.theme.nekoRippleConfiguration

@Composable
fun NekoScaffold(
    type: NekoTopAppBarType,
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
    onSearch: (String?) -> Unit = {},
    onSearchEnabled: () -> Unit = {},
    onSearchDisabled: () -> Unit = {},
    snackBarHost: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    underHeaderActions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit = {},
) {
    val systemUiController = rememberSystemUiController()
    val (color, onColor, useDarkIcons) = getTopAppBarColor(title, altAppBarColor)
    DisposableEffect(color, useDarkIcons) {
        systemUiController.setStatusBarColor(color, darkIcons = useDarkIcons)
        onDispose {}
    }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = snackBarHost,
        topBar = {
            CompositionLocalProvider(
                LocalRippleConfiguration provides themeColorState.rippleConfiguration,
            ) {
                NekoTopAppBar(
                    type = type,
                    title = title,
                    subtitle = subtitle,
                    scrollBehavior = scrollBehavior,
                    color = color,
                    onColor = onColor,
                    navigationIcon = navigationIcon,
                    navigationIconLabel = navigationIconLabel,
                    onNavigationIconClicked = onNavigationIconClicked,
                    actions = actions,
                    searchPlaceholder = searchPlaceHolder,
                    searchPlaceholderAlt = searchPlaceHolderAlt,
                    onSearch = onSearch,
                    onSearchEnabled = onSearchEnabled,
                    onSearchDisabled = onSearchDisabled,
                    focusRequester = focusRequester,
                    incognitoMode = incognitoMode,
                    isRoot = isRoot,
                    underHeaderActions = underHeaderActions,
                )
            }
        },
    ) { paddingValues ->
        CompositionLocalProvider(
            LocalRippleConfiguration provides
                nekoRippleConfiguration(MaterialTheme.colorScheme.primary),
        ) {
            content(paddingValues)
        }
    }
}

@Composable
fun getTopAppBarColor(title: String, altAppBarColor: Boolean): Triple<Color, Color, Boolean> {
    return when {
        title.isEmpty() && !altAppBarColor ->
            Triple(
                Color.Transparent,
                Color.Black,
                (MaterialTheme.colorScheme.surface.luminance() > 0.5f),
            )
        title.isNotEmpty() && !altAppBarColor ->
            Triple(
                MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                MaterialTheme.colorScheme.onSurface,
                (MaterialTheme.colorScheme.surface.copy(alpha = .7f).luminance() > 0.5f),
            )
        else ->
            Triple(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .7f),
                MaterialTheme.colorScheme.onSecondaryContainer,
                (MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .7f).luminance() > 0.5f),
            )
    }
}
