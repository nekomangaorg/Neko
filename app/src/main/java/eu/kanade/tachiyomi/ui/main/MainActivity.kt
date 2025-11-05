package eu.kanade.tachiyomi.ui.main

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import eu.kanade.tachiyomi.ui.main.states.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.states.LocalPullRefreshState
import eu.kanade.tachiyomi.ui.main.states.PullRefreshState
import eu.kanade.tachiyomi.ui.main.states.ScreenBars
import eu.kanade.tachiyomi.util.view.setComposeContent
import kotlinx.coroutines.launch
import org.nekomanga.core.R
import org.nekomanga.domain.snackbar.SnackbarColor
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.components.snackbar.NekoSnackbarHost
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.MainScreen
import org.nekomanga.presentation.screens.Screens

class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Force the 3-button navigation bar to be transparent
            // See:
            // https://developer.android.com/develop/ui/views/layout/edge-to-edge#create-transparent
            window.isNavigationBarContrastEnforced = false
        }

        val startingScreen = when(viewModel.preferences.startingTab().get()){
            1,
            -2 -> Screens.Feed
            -3 -> Screens.Browse()
            else -> Screens.Library()
        }


        setComposeContent {
            val context = LocalContext.current

            val mainScreenState by viewModel.mainScreenState.collectAsStateWithLifecycle()


            val backStack = rememberNavBackStack(startingScreen)

            val selectedItemIndex =
                remember(backStack.lastOrNull()) {
                    when (backStack.lastOrNull()) {
                        is Screens.Library -> 0
                        is Screens.Feed -> 1
                        is Screens.Browse -> 2
                        else -> -1
                    }
                }

            val snackbarHostState = remember { SnackbarHostState() }
            var currentSnackbarColor by remember { mutableStateOf<SnackbarColor?>(null) }

            LaunchedEffect(snackbarHostState.currentSnackbarData) {
                if (snackbarHostState.currentSnackbarData == null) {
                    currentSnackbarColor = null
                }
            }

            var mainDropdownShowing by remember { mutableStateOf(false) }

            var screenBars by remember { mutableStateOf(ScreenBars()) }

            val updateScreenBars: (ScreenBars) -> Unit = { newBars ->
                if (newBars.id == screenBars.id && newBars.topBar == null) {
                    // This is a screen being disposed, only clear if it is the current screen
                    screenBars = ScreenBars()
                } else if (newBars.topBar != null) {
                    screenBars = newBars
                }
            }

            var pullRefreshState by remember { mutableStateOf(PullRefreshState()) }
            val updateRefreshScreenBars: (PullRefreshState) -> Unit = { newPullRefreshState ->
                if (
                    newPullRefreshState.id == pullRefreshState.id &&
                        newPullRefreshState.onRefresh == null
                ) {
                    // This is a screen being disposed, only clear if it is the current screen
                    pullRefreshState = PullRefreshState()
                } else if (newPullRefreshState.onRefresh != null) {
                    pullRefreshState = newPullRefreshState
                }
            }

            // TODO status bar colors and navigation bar colors

            val windowSizeClass = calculateWindowSizeClass(this)
            val navItems =
                listOf(
                    NavigationItem(
                        screen = Screens.Library(),
                        title = stringResource(R.string.library),
                        unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks,
                        selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
                    ),
                    NavigationItem(
                        screen = Screens.Feed,
                        title = stringResource(R.string.feed),
                        unselectedIcon = Icons.Outlined.AccessTime,
                        selectedIcon = Icons.Filled.AccessTimeFilled,
                    ),
                    NavigationItem(
                        screen = Screens.Browse(),
                        title = stringResource(R.string.browse),
                        unselectedIcon = Icons.Outlined.Explore,
                        selectedIcon = Icons.Filled.Explore,
                    ),
                )

            val showNavigationRail =
                remember(windowSizeClass.widthSizeClass) {
                    windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                }
            CompositionLocalProvider(
                LocalBarUpdater provides updateScreenBars,
                LocalPullRefreshState provides updateRefreshScreenBars,
            ) {
                val scope = rememberCoroutineScope()
                ObserveAsEvents(viewModel.appSnackbarManager.events, snackbarHostState) { event ->
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        currentSnackbarColor = event.snackBarColor
                        val result =
                            snackbarHostState.showSnackbar(
                                message = event.getFormattedMessage(context),
                                actionLabel = event.getFormattedActionLabel(context),
                                duration = event.snackbarDuration,
                                withDismissAction = true,
                            )
                        when (result) {
                            SnackbarResult.ActionPerformed -> event.action?.invoke()
                            SnackbarResult.Dismissed -> event.dismissAction?.invoke()
                            else -> Unit
                        }
                    }
                }

                val nestedScroll = screenBars.scrollBehavior?.nestedScrollConnection
                PullRefresh(
                    enabled = pullRefreshState.enabled,
                    isRefreshing = pullRefreshState.isRefreshing,
                    onRefresh = pullRefreshState.onRefresh,
                    blurBackground = mainDropdownShowing,
                    trackColor = pullRefreshState.trackColor ?: MaterialTheme.colorScheme.secondary,
                ) {
                    Row(Modifier.fillMaxSize()) {
                        if (showNavigationRail && backStack.size == 1) {
                            NavigationSideBar(
                                items = navItems,
                                selectedItemIndex = selectedItemIndex,
                                onNavigate = { screen ->
                                    backStack.clear()
                                    backStack.add(screen)
                                },
                            )
                        }
                        Scaffold(
                            modifier =
                                Modifier.fillMaxHeight().weight(1f).conditional(
                                    nestedScroll != null
                                ) {
                                    this.nestedScroll(nestedScroll!!)
                                },
                            topBar = { screenBars.topBar?.invoke() },
                            snackbarHost = {
                                NekoSnackbarHost(snackbarHostState, currentSnackbarColor)
                            },
                            bottomBar = {
                                if (!showNavigationRail && backStack.size == 1) {
                                    BottomBar(
                                        items = navItems,
                                        selectedItemIndex = selectedItemIndex,
                                        onNavigate = { screen ->
                                            backStack.clear()
                                            backStack.add(screen)
                                        },
                                    )
                                }
                            },
                        ) { innerPadding ->
                            val currentScreen = backStack.lastOrNull()
                            val drawUnderTopBar = currentScreen is Screens.Manga
                            val layoutDirection = LocalLayoutDirection.current

                            val padding =
                                if (drawUnderTopBar) {
                                    PaddingValues(
                                        start = innerPadding.calculateStartPadding(layoutDirection),
                                        end = innerPadding.calculateEndPadding(layoutDirection),
                                        bottom = innerPadding.calculateBottomPadding(),
                                        top = 0.dp,
                                    )
                                } else {
                                    innerPadding
                                }

                            MainScreen(
                                contentPadding = padding,
                                backStack = backStack,
                                windowSizeClass = windowSizeClass,
                                onMenuShowing = { visible -> mainDropdownShowing = visible },
                                incognitoMode = mainScreenState.incognitoMode,
                                incognitoClick = viewModel::toggleIncoginito,
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {

        // Shortcut actions
        const val SHORTCUT_LIBRARY = "eu.kanade.tachiyomi.SHOW_LIBRARY"
        const val SHORTCUT_RECENTLY_UPDATED = "eu.kanade.tachiyomi.SHOW_RECENTLY_UPDATED"
        const val SHORTCUT_RECENTLY_READ = "eu.kanade.tachiyomi.SHOW_RECENTLY_READ"
        const val SHORTCUT_BROWSE = "eu.kanade.tachiyomi.SHOW_BROWSE"
        const val SHORTCUT_DOWNLOADS = "eu.kanade.tachiyomi.SHOW_DOWNLOADS"
        const val SHORTCUT_MANGA = "eu.kanade.tachiyomi.SHOW_MANGA"
        const val SHORTCUT_MANGA_BACK = "eu.kanade.tachiyomi.SHOW_MANGA_BACK"
        const val SHORTCUT_UPDATE_NOTES = "eu.kanade.tachiyomi.SHOW_UPDATE_NOTES"
        const val SHORTCUT_SOURCE = "eu.kanade.tachiyomi.SHOW_SOURCE"
        const val SHORTCUT_READER_SETTINGS = "eu.kanade.tachiyomi.READER_SETTINGS"
        const val SHORTCUT_EXTENSIONS = "eu.kanade.tachiyomi.EXTENSIONS"

        const val INTENT_SEARCH = "neko.SEARCH"
        const val INTENT_SEARCH_QUERY = "query"
        const val INTENT_SEARCH_FILTER = "filter"

        var chapterIdToExitTo = 0L
    }
}

@Composable
fun BottomBar(items: List<NavigationItem>, selectedItemIndex: Int, onNavigate: (NavKey) -> Unit) {

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        content = {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedItemIndex == index,
                    onClick = { onNavigate(item.screen) },
                    icon = {
                        Icon(
                            imageVector =
                                if (selectedItemIndex == index) item.selectedIcon
                                else item.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(text = item.title) },
                )
            }
        },
    )
}

@Composable
fun NavigationSideBar(
    items: List<NavigationItem>,
    selectedItemIndex: Int,
    onNavigate: (NavKey) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        content = {
            items.forEachIndexed { index, item ->
                NavigationRailItem(
                    selected = selectedItemIndex == index,
                    onClick = { onNavigate(item.screen) },
                    icon = {
                        Icon(
                            imageVector =
                                if (selectedItemIndex == index) item.selectedIcon
                                else item.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(text = item.title) },
                )
            }
        },
    )
}
