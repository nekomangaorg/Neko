package eu.kanade.tachiyomi.ui.main

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import eu.kanade.tachiyomi.ui.library.LibraryViewModel
import eu.kanade.tachiyomi.util.view.setComposeContent
import org.nekomanga.core.R
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.LibraryScreen
import org.nekomanga.presentation.screens.Screens

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Force the 3-button navigation bar to be transparent
            // See:
            // https://developer.android.com/develop/ui/views/layout/edge-to-edge#create-transparent
            window.isNavigationBarContrastEnforced = false
        }

        setComposeContent {
            val context = LocalContext.current

            var screenBars by remember { mutableStateOf(ScreenBars()) }

            val updateScreenBars: (ScreenBars) -> Unit = { newBars -> screenBars = newBars }

            var pullRefreshState by remember { mutableStateOf(PullRefreshState()) }
            val updateRefreshScreenBars: (PullRefreshState) -> Unit = { newPullRefreshState ->
                pullRefreshState = newPullRefreshState
            }

            // TODO load the correct one in future
            val backStack = rememberNavBackStack(Screens.Library)

            var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

            // TODO status bar colors and navigation bar colors

            val windowSizeClass = calculateWindowSizeClass(this)
            val navItems =
                listOf(
                    NavigationItem(
                        screen = Screens.Library,
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
                        screen = Screens.Browse,
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
                val nestedScroll = screenBars.scrollBehavior?.nestedScrollConnection
                PullRefresh(
                    enabled = pullRefreshState.enabled,
                    isRefreshing = pullRefreshState.isRefreshing,
                    onRefresh = pullRefreshState.onRefresh,
                ) {
                    Scaffold(
                        modifier =
                            Modifier.conditional(nestedScroll != null) {
                                this.nestedScroll(nestedScroll!!)
                            },
                        topBar = { screenBars.topBar?.invoke() },
                        bottomBar = {
                            if (!showNavigationRail && backStack.size == 1) {
                                BottomBar(
                                    items = navItems,
                                    selectedItemIndex = selectedItemIndex,
                                    onNavigate = { screen, index ->
                                        selectedItemIndex = index
                                        backStack.clear()
                                        backStack.add(screen)
                                    },
                                )
                            }
                        },
                    ) { innerPadding ->
                        val updatedInnerPadding =
                            remember(showNavigationRail) {
                                when (showNavigationRail) {
                                    true ->
                                        PaddingValues(
                                            start =
                                                innerPadding.calculateStartPadding(
                                                    LayoutDirection.Ltr
                                                ),
                                            top = innerPadding.calculateTopPadding(),
                                            end =
                                                innerPadding.calculateEndPadding(
                                                    LayoutDirection.Ltr
                                                ),
                                            bottom = innerPadding.calculateBottomPadding(),
                                        )

                                    false -> innerPadding
                                }
                            }

                        Box(modifier = Modifier.fillMaxSize().padding(updatedInnerPadding)) {
                            NavDisplay(
                                backStack = backStack,
                                onBack = { backStack.removeLastOrNull() },
                                entryDecorators =
                                    listOf(
                                        rememberSaveableStateHolderNavEntryDecorator(),
                                        rememberViewModelStoreNavEntryDecorator(),
                                    ),
                                entryProvider =
                                    entryProvider {
                                        entry<Screens.Library> {
                                            val libraryViewModel: LibraryViewModel = viewModel()
                                            LibraryScreen(
                                                libraryViewModel = libraryViewModel,
                                                windowSizeClass = windowSizeClass,
                                                incomingContentPadding = innerPadding,
                                            )
                                        }
                                    },
                            )
                        }
                    }

                    if (showNavigationRail && backStack.size == 1) {
                        NavigationSideBar(
                            items = navItems,
                            selectedItemIndex = selectedItemIndex,
                            onNavigate = { screen, index ->
                                selectedItemIndex = index
                                backStack.clear()
                                backStack.add(screen)
                            },
                        )
                    }
                }
            }
        }
    }

    // TODO remove the below functions
    fun isSideNavigation(): Boolean {
        return false
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
fun BottomBar(
    items: List<NavigationItem>,
    selectedItemIndex: Int,
    onNavigate: (NavKey, Int) -> Unit,
) {

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        content = {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedItemIndex == index,
                    onClick = { onNavigate(item.screen, index) },
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
    onNavigate: (NavKey, Int) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        content = {
            items.forEachIndexed { index, item ->
                NavigationRailItem(
                    selected = selectedItemIndex == index,
                    onClick = { onNavigate(item.screen, index) },
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
