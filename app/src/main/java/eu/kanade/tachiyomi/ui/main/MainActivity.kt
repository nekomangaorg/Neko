package eu.kanade.tachiyomi.ui.main

import android.app.SearchManager
import android.content.Context
import android.content.Intent
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
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.updater.AppDownloadInstallJob
import eu.kanade.tachiyomi.ui.main.states.LocalBarUpdater
import eu.kanade.tachiyomi.ui.main.states.LocalPullRefreshState
import eu.kanade.tachiyomi.ui.main.states.PullRefreshState
import eu.kanade.tachiyomi.ui.main.states.ScreenBars
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.source.browse.SearchBrowse
import eu.kanade.tachiyomi.ui.source.browse.SearchType
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.isAvailable
import eu.kanade.tachiyomi.util.manga.MangaMappings
import eu.kanade.tachiyomi.util.view.setComposeContent
import java.math.BigInteger
import kotlinx.coroutines.launch
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.R
import org.nekomanga.domain.snackbar.SnackbarColor
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.PullRefresh
import org.nekomanga.presentation.components.dialog.AppUpdateDialog
import org.nekomanga.presentation.components.snackbar.NekoSnackbarHost
import org.nekomanga.presentation.extensions.conditional
import org.nekomanga.presentation.screens.MainScreen
import org.nekomanga.presentation.screens.Screens
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

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

        val startingScreen =
            when (viewModel.preferences.startingTab().get()) {
                1,
                -2 -> Screens.Feed
                -3 -> Screens.Browse()
                else -> Screens.Library()
            }

        handleDeepLink(intent)

        setComposeContent {
            val context = LocalContext.current

            val mainScreenState by viewModel.mainScreenState.collectAsStateWithLifecycle()

            val backStack = rememberNavBackStack(startingScreen)

            val deepLink by viewModel.deepLinkScreen.collectAsStateWithLifecycle()

            LaunchedEffect(deepLink) {
                if (deepLink != null) {
                    backStack.clear()

                    deepLink!!.forEach { screen -> backStack.add(screen) }
                    viewModel.consumeDeepLink()
                }
            }

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
                    // screenBars = ScreenBars()
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
                    //   pullRefreshState = PullRefreshState()
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
                                startingScreen = startingScreen,
                                backStack = backStack,
                                windowSizeClass = windowSizeClass,
                                onMenuShowing = { visible -> mainDropdownShowing = visible },
                                incognitoMode = mainScreenState.incognitoMode,
                                incognitoClick = viewModel::toggleIncoginito,
                            )

                            if (mainScreenState.appUpdateResult != null) {
                                AppUpdateDialog(
                                    release = mainScreenState.appUpdateResult!!.release,
                                    onDismissRequest = { viewModel.consumeAppUpdateResult() },
                                    onConfirm = { url ->
                                        AppDownloadInstallJob.start(context, url, true)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    override fun onPause() {
        super.onPause()
        //    setStartingTab()
        saveExtras()
    }

    fun saveExtras() {
        viewModel.saveExtras()
    }

    /*   override fun finish() {
            if (!preferences.backReturnsToStart().get() && this !is SearchActivity) {
                setStartingTab()
            }
            if (this !is SearchActivity) {
                SecureActivityDelegate.locked = true
            }
            saveExtras()
            super.finish()
        }
    */

    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) {
            return
        }

        var deepLinkScreens: List<NavKey>? = null

        // clear the notification
        val notificationId = intent.getIntExtra(DeepLinks.Extras.NotificationId, -1)
        if (notificationId > -1) {
            NotificationReceiver.dismissNotification(
                applicationContext,
                notificationId,
                intent.getIntExtra(DeepLinks.Extras.GroupId, 0),
            )
        }
        when (intent.action) {
            DeepLinks.Actions.UpdateNotes -> {
                val extras = intent.extras ?: return
                val downloadUrl = extras.getString(DeepLinks.Extras.AppUpdateUrl) ?: return
                val notes = extras.getString(DeepLinks.Extras.AppUpdateNotes) ?: return
                val releaseUrl = extras.getString(DeepLinks.Extras.AppUpdateNotes) ?: return
                viewModel.addAppUpdateResult(downloadUrl, notes, releaseUrl)
            }
            Intent.ACTION_SEARCH,
            Intent.ACTION_SEND,
            "com.google.android.gms.actions.SEARCH_ACTION" -> {
                TimberKt.tag("DeepLink").d("Action Search Intent")
                // If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search
                // *query* on *Tachiyomi*" in Google Search/Google Assistant)

                // Get the search query provided in extras, and if not null, perform a global search
                // with it.
                val query =
                    intent.getStringExtra(SearchManager.QUERY)
                        ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                if (query != null && query.isNotEmpty()) {
                    deepLinkScreens =
                        listOf(
                            Screens.Browse(
                                searchBrowse = SearchBrowse(type = SearchType.Title, query = query)
                            )
                        )
                } else {
                    finish()
                }
            }
            DeepLinks.Intents.Search -> {
                val host = intent.data?.host
                val pathSegments = intent.data?.pathSegments
                if (host != null && pathSegments != null && pathSegments.size > 1) {
                    val path = pathSegments[0]
                    val id = pathSegments[1]
                    if (id != null && id.isNotEmpty()) {

                        val mappings: MangaMappings by injectLazy()

                        val query =
                            when {
                                host.contains("anilist", true) -> {
                                    val dexId = mappings.getMangadexUUID(id, "al")
                                    when (dexId == null) {
                                        true ->
                                            MdConstants.DeepLinkPrefix.error +
                                                "Unable to map MangaDex manga, no mapping entry found for AniList ID"
                                        false -> MdConstants.DeepLinkPrefix.manga + dexId
                                    }
                                }
                                host.contains("myanimelist", true) -> {
                                    val dexId = mappings.getMangadexUUID(id, "mal")
                                    when (dexId == null) {
                                        true ->
                                            MdConstants.DeepLinkPrefix.error +
                                                "Unable to map MangaDex manga, no mapping entry found for MyAnimeList ID"
                                        false -> MdConstants.DeepLinkPrefix.manga + dexId
                                    }
                                }
                                host.contains("mangaupdates", true) -> {
                                    val base = BigInteger(id, 36)
                                    val muID = base.toString(10)
                                    val dexId = mappings.getMangadexUUID(muID, "mu_new")
                                    when (dexId == null) {
                                        true ->
                                            MdConstants.DeepLinkPrefix.error +
                                                "Unable to map MangaDex manga, no mapping entry found for MangaUpdates ID"
                                        false -> MdConstants.DeepLinkPrefix.manga + dexId
                                    }
                                }
                                path.equals("GROUP", true) -> {
                                    MdConstants.DeepLinkPrefix.group + id
                                }
                                path.equals("AUTHOR", true) -> {
                                    MdConstants.DeepLinkPrefix.author + id
                                }
                                path.equals("LIST", true) -> {
                                    MdConstants.DeepLinkPrefix.list + id
                                }
                                else -> {
                                    MdConstants.DeepLinkPrefix.manga + id
                                }
                            }
                        deepLinkScreens =
                            listOf(
                                Screens.Browse(SearchBrowse(type = SearchType.Title, query = query))
                            )
                    }
                }
            }
            DeepLinks.Actions.Manga,
            DeepLinks.Actions.MangaBack -> {
                val extras = intent.extras ?: return
                val mangaId = extras.getLong(DeepLinks.Extras.MangaId)

                if (
                    intent.action == DeepLinks.Actions.MangaBack &&
                        viewModel.preferences.openChapterInShortcuts().get()
                ) {
                    val mangaId = extras.getLong(DeepLinks.Extras.MangaId)
                    if (mangaId != 0L) {
                        val db = Injekt.get<DatabaseHelper>()
                        val downloadManager = Injekt.get<DownloadManager>()
                        val chapters = db.getChapters(mangaId).executeAsBlocking()
                        db.getManga(mangaId).executeAsBlocking()?.let { manga ->
                            val availableChapters =
                                chapters.filter { it.isAvailable(downloadManager, manga) }
                            val nextUnreadChapter =
                                ChapterSort(manga).getNextUnreadChapter(availableChapters, false)
                            if (nextUnreadChapter != null) {
                                val activity =
                                    ReaderActivity.newIntent(this, manga, nextUnreadChapter)
                                startActivity(activity)
                                finish()
                            }
                        }
                    }
                }
                if (intent.action == DeepLinks.Actions.MangaBack) {
                    SecureActivityDelegate.promptLockIfNeeded(this, true)
                }
                deepLinkScreens = listOf(Screens.Library(), Screens.Manga(mangaId))
            }

            DeepLinks.Actions.ReaderSettings -> {
                deepLinkScreens = listOf(Screens.Settings.Main(Screens.Settings.Reader))
            }
        }
        if (deepLinkScreens != null) {
            viewModel.setDeepLink(deepLinkScreens)
        }
        setIntent(null)
    }

    companion object {
        var chapterIdToExitTo = 0L

        fun openMangaIntent(context: Context, id: Long?, canReturnToMain: Boolean = false) =
            Intent(context, MainActivity::class.java).apply {
                action =
                    if (canReturnToMain) DeepLinks.Actions.MangaBack else DeepLinks.Actions.Manga
                putExtra(DeepLinks.Extras.MangaId, id)
            }

        fun openReaderSettings(context: Context) =
            Intent(context, MainActivity::class.java).apply {
                action = DeepLinks.Actions.ReaderSettings
            }
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
