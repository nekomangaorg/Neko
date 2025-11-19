package eu.kanade.tachiyomi.ui.main

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.work.WorkInfo
import androidx.work.WorkManager
import eu.kanade.tachiyomi.Migrations
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.updater.AppDownloadInstallJob
import eu.kanade.tachiyomi.data.updater.RELEASE_URL
import eu.kanade.tachiyomi.ui.base.activity.BaseMainActivity
import eu.kanade.tachiyomi.ui.main.states.SideNavMode
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.source.browse.SearchBrowse
import eu.kanade.tachiyomi.ui.source.browse.SearchType
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.isAvailable
import eu.kanade.tachiyomi.util.manga.MangaMappings
import eu.kanade.tachiyomi.util.view.setComposeContent
import java.math.BigInteger
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.nekomanga.BuildConfig
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.R
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.dialog.AppUpdateDialog
import org.nekomanga.presentation.components.dialog.WhatsNewDialog
import org.nekomanga.presentation.screens.MainScreen
import org.nekomanga.presentation.screens.Screens
import org.nekomanga.presentation.screens.main.BottomBar
import org.nekomanga.presentation.screens.main.NavigationSideBar
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MainActivity : BaseMainActivity() {

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

        val isInitialDeepLink = isDeepLink(intent)

        val startingScreen =
            if (isInitialDeepLink) {
                Screens.Loading
            } else if (!viewModel.preferences.hasShownOnboarding().get()) {
                Screens.Onboarding
            } else {
                when (viewModel.preferences.startingTab().get()) {
                    1 -> {
                        if (viewModel.preferences.lastUsedStartingTab().get() == 0) {
                            Screens.Library()
                        } else {
                            Screens.Feed
                        }
                    }

                    -2 -> Screens.Feed
                    -3 -> Screens.Browse()
                    else -> Screens.Library()
                }
            }

        handleDeepLink(intent)

        if (Migrations.upgrade(viewModel.preferences)) {
            if (!BuildConfig.DEBUG) {
                viewModel.setWhatsNewDialog(true)
            }
        }

        setComposeContent {
            val context = LocalContext.current
            val mainScreenState by viewModel.mainScreenState.collectAsStateWithLifecycle()

            val lifecycleOwner = LocalLifecycleOwner.current

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

            var libraryUpdating by remember { mutableStateOf(false) }
            var downloaderRunning by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        WorkManager.getInstance(this@MainActivity)
                            .getWorkInfosByTagFlow(LibraryUpdateJob.TAG)
                            .map { workInfoList ->
                                workInfoList.any { it.state == WorkInfo.State.RUNNING }
                            }
                            .collect { running -> libraryUpdating = running }
                    }
                }
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        viewModel.downloadManager.isDownloaderRunning.collect { running ->
                            downloaderRunning = running
                        }
                    }
                }
            }

            DisposableEffect(lifecycleOwner, selectedItemIndex) {
                // Create an observer
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_DESTROY) {
                        viewModel.saveExtras(selectedItemIndex == 0)
                    }
                }

                // Add the observer to the lifecycle
                lifecycleOwner.lifecycle.addObserver(observer)

                // When the composable leaves the composition, remove the observer
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val deepLink by viewModel.deepLinkScreen.collectAsStateWithLifecycle()

            LaunchedEffect(deepLink) {
                if (deepLink != null) {
                    backStack.clear()

                    backStack.addAll(deepLink!!)
                    viewModel.consumeDeepLink()
                }
            }

            if (mainScreenState.showWhatsNewDialog) {
                WhatsNewDialog(
                    onDismissRequest = { viewModel.setWhatsNewDialog(false) },
                    onSeeWhatsNewClick = {
                        val intent = Intent(Intent.ACTION_VIEW, RELEASE_URL.toUri())
                        startActivity(intent)
                    },
                )
            }

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
                remember(windowSizeClass.widthSizeClass, mainScreenState.sideNavMode) {
                    when {
                        mainScreenState.sideNavMode == SideNavMode.Always -> true
                        mainScreenState.sideNavMode == SideNavMode.Never -> false
                        else -> windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                    }
                }

            val navigationRail: @Composable () -> Unit = {
                if (showNavigationRail) {
                    NavigationSideBar(
                        items = navItems,
                        sideNavAlignment = mainScreenState.sideNavAlignment,
                        libraryUpdating = libraryUpdating,
                        downloaderRunning = downloaderRunning,
                        selectedItemIndex = selectedItemIndex,
                        onNavigate = { screen ->
                            backStack.clear()
                            backStack.add(screen)
                        },
                    )
                }
            }

            val bottomBar: @Composable () -> Unit = {
                if (!showNavigationRail) {
                    BottomBar(
                        items = navItems,
                        libraryUpdating = libraryUpdating,
                        downloaderRunning = downloaderRunning,
                        selectedItemIndex = selectedItemIndex,
                        onNavigate = { screen ->
                            backStack.clear()
                            backStack.add(screen)
                        },
                    )
                }
            }

            MainScreen(
                backStack = backStack,
                windowSizeClass = windowSizeClass,
                incognitoMode = mainScreenState.incognitoMode,
                incognitoClick = viewModel::toggleIncoginito,
                onboardingCompleted = viewModel::onboardingCompleted,
                navigationRail = navigationRail,
                bottomBar = bottomBar,
            )

            if (mainScreenState.appUpdateResult != null) {
                AppUpdateDialog(
                    release = mainScreenState.appUpdateResult!!.release,
                    onDismissRequest = { viewModel.consumeAppUpdateResult() },
                    onConfirm = { url -> AppDownloadInstallJob.start(context, url, true) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

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
                                ChapterItemSort()
                                    .getNextUnreadChapter(
                                        manga,
                                        availableChapters.map {
                                            it.toSimpleChapter()!!.toChapterItem()
                                        },
                                        false,
                                    )
                            if (nextUnreadChapter != null) {
                                val activity =
                                    ReaderActivity.newIntent(
                                        this,
                                        manga,
                                        nextUnreadChapter.chapter.toDbChapter(),
                                    )
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

    private fun isDeepLink(intent: Intent?): Boolean {
        if (intent == null) return false
        return when (intent.action) {
            Intent.ACTION_SEARCH,
            Intent.ACTION_SEND,
            "com.google.android.gms.actions.SEARCH_ACTION",
            DeepLinks.Intents.Search,
            DeepLinks.Actions.Manga,
            DeepLinks.Actions.MangaBack,
            DeepLinks.Actions.ReaderSettings -> true
            else -> false
        }
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
