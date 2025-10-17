package eu.kanade.tachiyomi.ui.main

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.IdRes
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.Router
import com.google.android.material.navigation.NavigationBarView
import eu.kanade.tachiyomi.Migrations
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateNotifier
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.data.updater.RELEASE_URL
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.MaterialMenuSheet
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.feed.FeedController
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.more.NewUpdateDialogController
import eu.kanade.tachiyomi.ui.more.about.AboutController
import eu.kanade.tachiyomi.ui.more.stats.StatsController
import eu.kanade.tachiyomi.ui.onboarding.OnboardingController
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.ui.source.browse.BrowseController
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.manga.MangaShortcutManager
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.hasSideNavBar
import eu.kanade.tachiyomi.util.system.ignoredSystemInsets
import eu.kanade.tachiyomi.util.system.isBottomTappable
import eu.kanade.tachiyomi.util.system.isInNightMode
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.prepareSideNavContext
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.canStillGoBack
import eu.kanade.tachiyomi.util.view.doOnApplyWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.getItemView
import eu.kanade.tachiyomi.util.view.withFadeInTransaction
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.databinding.MainActivityBinding
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

@SuppressLint("ResourceType")
open class MainActivity : BaseActivity<MainActivityBinding>() {

    protected lateinit var router: Router

    val source: Source by lazy { Injekt.get<SourceManager>().mangaDex }

    private var pulseAnimator: Animator? = null

    private val downloadManager: DownloadManager by injectLazy()
    private val mangaShortcutManager: MangaShortcutManager by injectLazy()
    private val hideBottomNav
        get() = router.backstackSize > 1

    private val updateChecker by lazy { AppUpdateChecker() }
    private val isUpdaterEnabled = BuildConfig.INCLUDE_UPDATER
    private var overflowDialog: Dialog? = null
    var ogWidth: Int = Int.MAX_VALUE

    private lateinit var viewModel: MainViewModel

    override fun attachBaseContext(newBase: Context?) {
        ogWidth = min(newBase?.resources?.configuration?.screenWidthDp ?: Int.MAX_VALUE, ogWidth)
        super.attachBaseContext(newBase?.prepareSideNavContext())
    }

    var backPressedCallback: OnBackPressedCallback? = null
    val backCallback = {
        pressingBack()
        reEnableBackPressedCallBack()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set up shared element transition and disable overlay so views don't show above system
        // bars
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        window.sharedElementsUseOverlay = false

        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, MainViewModelFactory())[MainViewModel::class.java]

        backPressedCallback = onBackPressedDispatcher.addCallback { backCallback() }

        // Do not let the launcher create a new activity http://stackoverflow.com/questions/16283079
        if (!isTaskRoot && this !is SearchActivity) {
            finish()
            return
        }
        binding = MainActivityBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }

        nav.getItemView(R.id.nav_library)?.setOnLongClickListener {
            if (!LibraryUpdateJob.isRunning(this)) {
                LibraryUpdateJob.startNow(this)
            }
            true
        }

        val container: ViewGroup = binding.controllerContainer

        val content: ViewGroup = binding.mainContent
        downloadManager.isDownloaderRunning.onEach(::downloadStatusChanged).launchIn(lifecycleScope)
        lifecycleScope.launchIO { downloadManager.deletePendingChapters() }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setNavBarColor(content.rootWindowInsetsCompat)
        nav.isVisible = false
        content.doOnApplyWindowInsetsCompat { v, insets, _ ->
            setNavBarColor(insets)
            val systemInsets = insets.ignoredSystemInsets
            val contextView = window?.decorView?.findViewById<View>(R.id.action_mode_bar)
            contextView?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemInsets.left
                rightMargin = systemInsets.right
            }
            // Consume any horizontal insets and pad all content in. There's not much we can do
            // with horizontal insets
            v.updatePadding(left = systemInsets.left, right = systemInsets.right)
            binding.bottomNav?.updatePadding(bottom = systemInsets.bottom)
            binding.sideNav?.updatePadding(
                left = 0,
                right = 0,
                bottom = systemInsets.bottom,
                top = systemInsets.top,
            )
            binding.bottomView?.isVisible = systemInsets.bottom > 0
            binding.bottomView?.updateLayoutParams<ViewGroup.LayoutParams> {
                height = systemInsets.bottom
            }
        }
        // Set this as nav view will try to set its own insets and they're hilariously bad
        ViewCompat.setOnApplyWindowInsetsListener(nav) { _, insets -> insets }

        router = Conductor.attachRouter(this, container, savedInstanceState)

        if (router.hasRootController()) {
            nav.selectedItemId =
                when (router.backstack.firstOrNull()?.controller) {
                    is FeedController -> R.id.nav_feed
                    is BrowseController -> R.id.nav_browse
                    is LibraryController -> R.id.nav_feed
                    else -> R.id.nav_library
                }
        }

        nav.setOnItemSelectedListener { item ->
            val id = item.itemId
            val currentRoot = router.backstack.firstOrNull()
            if (currentRoot?.tag()?.toIntOrNull() != id) {
                setRoot(
                    when (id) {
                        R.id.nav_library -> LibraryController()
                        R.id.nav_feed -> FeedController() // FeedController()
                        else -> BrowseController()
                    },
                    id,
                )
            }
            true
        }

        if (!router.hasRootController()) {
            // Set start screen
            if (!handleIntentAction(intent)) {
                goToStartingTab()
                if (!preferences.hasShownOnboarding().get()) {
                    router.pushController(OnboardingController().withFadeInTransaction())
                }
            }
        }

        nav.isVisible = !hideBottomNav
        /*
                updateControllersWithSideNavChanges()
        */
        binding.bottomView?.visibility =
            if (hideBottomNav) View.GONE else binding.bottomView?.visibility ?: View.GONE
        nav.alpha = if (hideBottomNav) 0f else 1f
        router.addChangeListener(
            object : ControllerChangeHandler.ControllerChangeListener {
                override fun onChangeStarted(
                    to: Controller?,
                    from: Controller?,
                    isPush: Boolean,
                    container: ViewGroup,
                    handler: ControllerChangeHandler,
                ) {
                    syncActivityViewWithController(to, from, isPush)
                }

                override fun onChangeCompleted(
                    to: Controller?,
                    from: Controller?,
                    isPush: Boolean,
                    container: ViewGroup,
                    handler: ControllerChangeHandler,
                ) {
                    nav.translationY = 0f
                    if (router.backstackSize == 1) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !isPush) {
                            window?.setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                            )
                        }
                    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        window?.setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        )
                    }
                }
            }
        )

        syncActivityViewWithController(router.backstack.lastOrNull()?.controller)

        if (savedInstanceState == null && this !is SearchActivity) {
            // Reset Incognito Mode on relaunch
            securityPreferences.incognitoMode().set(false)

            // Show changelog if needed
            if (Migrations.upgrade(preferences)) {
                if (!BuildConfig.DEBUG) {
                    content.post { whatsNewSheet().show() }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WorkManager.getInstance(this@MainActivity)
                    .getWorkInfosByTagFlow(LibraryUpdateJob.TAG)
                    .map { workInfoList -> workInfoList.any { it.state == WorkInfo.State.RUNNING } }
                    .collect { running ->
                        if (running) {
                            startIconPulse(binding)
                        } else {
                            stopIconPulse(binding)
                        }
                    }
            }
        }

        preferences
            .sideNavIconAlignment()
            .changes()
            .onEach {
                binding.sideNav?.menuGravity =
                    when (it) {
                        1 -> Gravity.CENTER
                        2 -> Gravity.BOTTOM
                        else -> Gravity.TOP
                    }
            }
            .launchIn(lifecycleScope)
    }

    fun reEnableBackPressedCallBack() {
        val returnToStart = preferences.backReturnsToStart().get() && this !is SearchActivity
        backPressedCallback?.isEnabled =
            router.canStillGoBack() || (returnToStart && startingTab() != nav.selectedItemId)
    }

    private fun setNavBarColor(insets: WindowInsetsCompat?) {
        if (insets == null) return
        window.navigationBarColor =
            when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 -> {
                    // basically if in landscape on a phone
                    // For lollipop, draw opaque nav bar
                    when {
                        insets.hasSideNavBar() -> Color.BLACK
                        isInNightMode() ->
                            ColorUtils.setAlphaComponent(
                                getResourceColor(R.attr.colorSurfaceContainer),
                                179,
                            )
                        else -> Color.argb(179, 0, 0, 0)
                    }
                }
                // if the android q+ device has gesture nav, transparent nav bar
                // this is here in case some crazy with a notch uses landscape
                insets.isBottomTappable() -> {
                    getColor(android.R.color.transparent)
                }
                // if in landscape with 2/3 button mode, fully opaque nav bar
                insets.hasSideNavBar() -> {
                    getResourceColor(R.attr.colorSurfaceContainer)
                }
                // if in portrait with 2/3 button mode, translucent nav bar
                else -> {
                    ColorUtils.setAlphaComponent(
                        getResourceColor(R.attr.colorSurfaceContainer),
                        179,
                    )
                }
            }
    }

    override fun onResume() {
        super.onResume()
        checkForAppUpdates()
        reEnableBackPressedCallBack()
    }

    override fun onPause() {
        super.onPause()
        setStartingTab()
        saveExtras()
    }

    fun saveExtras() {
        mangaShortcutManager.updateShortcuts()
        MangaCoverMetadata.savePrefs()
    }

    private fun checkForAppUpdates() {
        if (isUpdaterEnabled) {
            lifecycleScope.launchIO {
                try {
                    val result = updateChecker.checkForUpdate(this@MainActivity)
                    if (result is AppUpdateResult.NewUpdate) {
                        val body = result.release.info
                        val url = result.release.downloadLink

                        // Create confirmation window
                        withContext(Dispatchers.Main) {
                            AppUpdateNotifier.releasePageUrl = result.release.releaseLink
                            NewUpdateDialogController(body, url).showDialog(router)
                        }
                    }
                } catch (error: Exception) {
                    TimberKt.e(error) { "Error checking for app update" }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (!handleIntentAction(intent)) {
            super.onNewIntent(intent)
        }
    }

    protected open fun handleIntentAction(intent: Intent): Boolean {
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId > -1) {
            NotificationReceiver.dismissNotification(
                applicationContext,
                notificationId,
                intent.getIntExtra("groupId", 0),
            )
        }
        when (intent.action) {
            SHORTCUT_LIBRARY -> nav.selectedItemId = R.id.nav_library
            SHORTCUT_BROWSE -> nav.selectedItemId = R.id.nav_browse
            SHORTCUT_MANGA -> {
                val extras = intent.extras ?: return false
                if (router.backstack.isEmpty()) nav.selectedItemId = R.id.nav_library
                router.pushController(MangaDetailController(extras).withFadeTransaction())
            }
            SHORTCUT_UPDATE_NOTES -> {
                val extras = intent.extras ?: return false
                if (router.backstack.isEmpty()) nav.selectedItemId = R.id.nav_library
                if (router.backstack.lastOrNull()?.controller !is NewUpdateDialogController) {
                    NewUpdateDialogController(extras).showDialog(router)
                }
            }
            else -> return false
        }
        return true
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        when (val controller = router.backstack.lastOrNull()?.controller) {
        /*  is MangaDetailController -> {
            val url =
                try {
                    (source as HttpSource)
                        .mangaDetailsRequest(controller.presenter.manga.value!!)
                        .url
                        .toString()
                } catch (e: Exception) {
                    return
                }
            outContent.webUri = Uri.parse(url)
        }*/
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overflowDialog?.dismiss()
        overflowDialog = null
    }

    private fun pressingBack() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ViewCompat.getRootWindowInsets(window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        ) {
            WindowInsetsControllerCompat(window, binding.root).hide(WindowInsetsCompat.Type.ime())
        } else {
            backPress()
        }
    }

    override fun finish() {
        if (!preferences.backReturnsToStart().get() && this !is SearchActivity) {
            setStartingTab()
        }
        if (this !is SearchActivity) {
            SecureActivityDelegate.locked = true
        }
        saveExtras()
        super.finish()
    }

    protected open fun backPress() {
        val controller = router.backstack.lastOrNull()?.controller
        if (
            if (router.backstackSize == 1) controller?.handleBack() != true
            else !router.handleBack()
        ) {
            if (shouldGoToStartingTab()) {
                goToStartingTab()
            }
        }
    }

    fun shouldGoToStartingTab(): Boolean {
        return preferences.backReturnsToStart().get() &&
            this !is SearchActivity &&
            startingTab() != nav.selectedItemId
    }

    protected val nav: NavigationBarView
        get() = binding.bottomNav ?: binding.sideNav!!

    private fun setStartingTab() {
        if (this is SearchActivity || !isBindingInitialized) return
        if (nav.selectedItemId != R.id.nav_browse && preferences.startingTab().get() >= 0) {
            preferences
                .startingTab()
                .set(
                    when (nav.selectedItemId) {
                        R.id.nav_library -> 0
                        else -> 1
                    }
                )
        }
    }

    @IdRes
    private fun startingTab(): Int {
        return when (preferences.startingTab().get()) {
            0,
            -1 -> R.id.nav_library
            1,
            -2 -> R.id.nav_feed
            -3 -> R.id.nav_browse
            else -> R.id.nav_library
        }
    }

    private fun goToStartingTab() {
        nav.selectedItemId = startingTab()
    }

    fun goToTab(@IdRes id: Int) {
        nav.selectedItemId = id
    }

    private fun setRoot(controller: Controller, id: Int) {
        router.setRoot(controller.withFadeInTransaction().tag(id.toString()))
    }

    fun showSettings() {
        router.pushController(SettingsController().withFadeTransaction())
    }

    fun showAbout() {
        router.pushController(AboutController().withFadeTransaction())
    }

    fun showStats() {
        router.pushController(StatsController().withFadeTransaction())
    }

    protected open fun syncActivityViewWithController(
        to: Controller?,
        from: Controller? = null,
        isPush: Boolean = false,
    ) {

        reEnableBackPressedCallBack()
        nav.visibility = if (!hideBottomNav) View.VISIBLE else nav.visibility
        nav.isVisible = !hideBottomNav
        nav.alpha = 1f
    }

    fun isSideNavigation(): Boolean {
        return binding.bottomNav == null
    }

    private fun downloadStatusChanged(downloading: Boolean) {
        lifecycleScope.launchUI {
            val hasQueue = downloading || downloadManager.queueState.value.isNotEmpty()

            if (hasQueue) {
                nav.getOrCreateBadge(R.id.nav_feed).apply {
                    backgroundColor = this@MainActivity.getResourceColor(R.attr.colorSecondary)
                }
            } else {
                nav.removeBadge(R.id.nav_feed)
            }
        }
    }

    private fun whatsNewSheet() =
        MaterialMenuSheet(
            this,
            listOf(
                MaterialMenuSheet.MenuSheetItem(
                    0,
                    textRes = R.string.whats_new_this_release,
                    drawable = R.drawable.ic_new_releases_24dp,
                ),
                MaterialMenuSheet.MenuSheetItem(
                    1,
                    textRes = R.string.close,
                    drawable = R.drawable.ic_close_24dp,
                ),
            ),
            title = getString(R.string.updated_to_, BuildConfig.VERSION_NAME),
            showDivider = true,
            selectedId = 0,
            onMenuItemClicked = { _, item ->
                if (item == 0) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, RELEASE_URL.toUri())
                        startActivity(intent)
                    } catch (e: Throwable) {
                        toast(e.message)
                    }
                }
                true
            },
        )

    private fun startIconPulse(binding: MainActivityBinding) {
        val iconView = getIconItemView(binding)
        iconView ?: return
        // If animation is not already running, start it
        if (pulseAnimator == null || !pulseAnimator!!.isRunning) {
            pulseAnimator =
                AnimatorInflater.loadAnimator(this, R.animator.pulse_animator).apply {
                    setTarget(iconView)
                    start()
                }
        }
    }

    private fun stopIconPulse(binding: MainActivityBinding) {
        pulseAnimator?.cancel()
        val iconView = getIconItemView(binding)
        iconView ?: return
        iconView.scaleX = 1.0f
        iconView.scaleY = 1.0f
    }

    private fun getIconItemView(binding: MainActivityBinding): View? {
        val itemContainerView =
            binding.bottomNav?.findViewById<View>(R.id.nav_library)
                ?: binding.sideNav?.findViewById<View>(R.id.nav_library)
        itemContainerView ?: return null
        return itemContainerView.findViewById(
            com.google.android.material.R.id.navigation_bar_item_icon_view
        )
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
