package org.nekomanga.presentation.components

import TooltipBox
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import eu.kanade.tachiyomi.R

@Composable
fun NekoScaffold(
    title: String,
    onNavigationIconClicked: () -> Unit,
    navigationIcon: ImageVector = Icons.Filled.ArrowBack,
    navigationIconLabel: String = stringResource(id = R.string.back),
    subtitle: String = "",
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit = {},
) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() > .5
    val color = getTopAppBarColor()
    SideEffect {
        systemUiController.setStatusBarColor(color, darkIcons = useDarkIcons)
    }
    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar =
        {
            CompositionLocalProvider(LocalRippleTheme provides CoverRippleTheme) {
                if (subtitle.isEmpty()) {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = getTopAppBarColor(),
                            scrolledContainerColor = getTopAppBarColor(),
                        ),
                        modifier = Modifier
                            .statusBarsPadding(),
                        title = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            TooltipBox(
                                toolTipLabel = navigationIconLabel,
                                icon = navigationIcon,
                                buttonClicked = onNavigationIconClicked,
                            )
                        },
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                    )
                } else {
                    SmallTopAppBar(
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = getTopAppBarColor(),
                            scrolledContainerColor = getTopAppBarColor(),
                        ),
                        modifier = Modifier
                            .statusBarsPadding(),
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
                        navigationIcon = {
                            TooltipBox(
                                toolTipLabel = navigationIconLabel,
                                icon = navigationIcon,
                                buttonClicked = onNavigationIconClicked,
                            )
                        },
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
    ) { paddingValues ->
        content(paddingValues)
    }
}

@Composable
fun getTopAppBarColor(): Color {
    return MaterialTheme.colorScheme.surface.copy(alpha = .7f)
}
