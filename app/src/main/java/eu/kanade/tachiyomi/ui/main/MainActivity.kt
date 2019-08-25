package eu.kanade.tachiyomi.ui.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.view.ViewGroup
import com.bluelinelabs.conductor.*
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.context.IconicsContextWrapper
import com.mikepenz.iconics.typeface.IIcon
import eu.kanade.tachiyomi.Migrations
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.controller.*
import eu.kanade.tachiyomi.ui.catalogue.browse.BrowseCatalogueController
import eu.kanade.tachiyomi.ui.catalogue.follows.FollowsController
import eu.kanade.tachiyomi.ui.download.DownloadController
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.recent_updates.RecentChaptersController
import eu.kanade.tachiyomi.ui.recently_read.RecentlyReadController
import eu.kanade.tachiyomi.ui.setting.SettingsMainController
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import kotlinx.android.synthetic.main.main_activity.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy


class MainActivity : BaseActivity(),  SourceLoginDialog.Listener {

    private lateinit var router: Router

    val preferences: PreferencesHelper by injectLazy()

    val source: Source by lazy { Injekt.get<SourceManager>().getSources()[0] }

    private var drawerArrow: DrawerArrowDrawable? = null

    private var secondaryDrawer: ViewGroup? = null

    private val startScreenId by lazy {
        when (preferences.startScreen()) {
            2 -> R.id.nav_drawer_recently_read
            3 -> R.id.nav_drawer_recent_updates
            else -> R.id.nav_drawer_library
        }
    }

    lateinit var tabAnimator: TabsAnimator

    override fun attachBaseContext(newBase: Context) {
        //needed for Iconics
        super.attachBaseContext(IconicsContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(when (preferences.theme()) {
            2 -> R.style.Theme_Neko_Dark
            3 -> R.style.Theme_Neko_Amoled
            else -> R.style.Theme_Neko
        })
        super.onCreate(savedInstanceState)

        // Do not let the launcher create a new activity http://stackoverflow.com/questions/16283079
        if (!isTaskRoot) {
            finish()
            return
        }

        setContentView(R.layout.main_activity)

        setSupportActionBar(toolbar)

        drawerArrow = DrawerArrowDrawable(this)
        drawerArrow?.color = Color.WHITE
        toolbar.navigationIcon = drawerArrow

        tabAnimator = TabsAnimator(tabs)

        // Set behavior of Navigation drawer
        addIconToMenu(R.id.nav_drawer_library, CommunityMaterial.Icon2.cmd_library)
        addIconToMenu(R.id.nav_drawer_recent_updates, CommunityMaterial.Icon2.cmd_update)
        addIconToMenu(R.id.nav_drawer_recently_read, CommunityMaterial.Icon.cmd_glasses)
        addIconToMenu(R.id.nav_drawer_browse, CommunityMaterial.Icon.cmd_compass_outline)
        addIconToMenu(R.id.nav_drawer_follows, CommunityMaterial.Icon.cmd_bookmark)
        addIconToMenu(R.id.nav_drawer_downloads, CommunityMaterial.Icon.cmd_download)
        addIconToMenu(R.id.nav_drawer_settings, CommunityMaterial.Icon2.cmd_settings)

        nav_view.setNavigationItemSelectedListener { item ->
            val id = item.itemId

            val currentRoot = router.backstack.firstOrNull()
            if (currentRoot?.tag()?.toIntOrNull() != id) {
                when (id) {
                    R.id.nav_drawer_library -> setRoot(LibraryController(), id)
                    R.id.nav_drawer_recent_updates -> setRoot(RecentChaptersController(), id)
                    R.id.nav_drawer_recently_read -> setRoot(RecentlyReadController(), id)
                    R.id.nav_drawer_browse -> {
                        val browseCatalogueController = BrowseCatalogueController(source)
                        setRoot(browseCatalogueController, id)
                        if (!source.isLogged()) {
                            val dialog = SourceLoginDialog(source)
                            dialog.targetController = browseCatalogueController
                            dialog.showDialog(router)
                        }
                    }
                    R.id.nav_drawer_follows -> {
                        val latestUpdatesController = FollowsController(source)
                        setRoot(latestUpdatesController, id)
                        if (!source.isLogged()) {
                            val dialog = SourceLoginDialog(source)
                            dialog.targetController = latestUpdatesController
                            dialog.showDialog(router)
                        }
                    }
                    R.id.nav_drawer_downloads -> {
                        router.pushController(DownloadController().withFadeTransaction())
                    }
                    R.id.nav_drawer_settings -> {
                        router.pushController(SettingsMainController().withFadeTransaction())
                    }
                }
            }
            drawer.closeDrawer(GravityCompat.START)
            true
        }

        val container: ViewGroup = findViewById(R.id.controller_container)

        router = Conductor.attachRouter(this, container, savedInstanceState)
        if (!router.hasRootController()) {
            // Set start screen
            if (!handleIntentAction(intent)) {
                setSelectedDrawerItem(startScreenId)
            }
        }

        toolbar.setNavigationOnClickListener {
            if (router.backstackSize == 1) {
                drawer.openDrawer(GravityCompat.START)
            } else {
                onBackPressed()
            }
        }

        router.addChangeListener(object : ControllerChangeHandler.ControllerChangeListener {
            override fun onChangeStarted(to: Controller?, from: Controller?, isPush: Boolean,
                                         container: ViewGroup, handler: ControllerChangeHandler) {

                syncActivityViewWithController(to, from)
            }

            override fun onChangeCompleted(to: Controller?, from: Controller?, isPush: Boolean,
                                           container: ViewGroup, handler: ControllerChangeHandler) {

            }

        })

        syncActivityViewWithController(router.backstack.lastOrNull()?.controller())

        if (savedInstanceState == null) {
            // Show changelog if needed
            if (Migrations.upgrade(preferences)) {
                ChangelogDialogController().showDialog(router)
            }
        }
    }

    private fun addIconToMenu(nav_drawer_library: Int, icon: IIcon) {
        //no size or color needed since navigation drawer dictates it
        nav_view.menu.findItem(nav_drawer_library).icon = IconicsDrawable(this).icon(icon)

    }

    /**
     * Called when login dialog is closed, refreshes the adapter.
     *
     * @param source clicked item containing source information.
     */
    override fun loginDialogClosed(source: Source) {
        if (source.isLogged()) {
            router.popCurrentController()
            R.id.nav_drawer_browse
            setRoot(BrowseCatalogueController(source), R.id.nav_drawer_browse)
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (!handleIntentAction(intent)) {
            super.onNewIntent(intent)
        }
    }

    private fun handleIntentAction(intent: Intent): Boolean {
        when (intent.action) {
            SHORTCUT_LIBRARY -> setSelectedDrawerItem(R.id.nav_drawer_library)
            SHORTCUT_RECENTLY_UPDATED -> setSelectedDrawerItem(R.id.nav_drawer_recent_updates)
            SHORTCUT_RECENTLY_READ -> setSelectedDrawerItem(R.id.nav_drawer_recently_read)
            SHORTCUT_CATALOGUES -> setSelectedDrawerItem(R.id.nav_drawer_browse)
            SHORTCUT_MANGA -> {
                val extras = intent.extras ?: return false
                router.setRoot(RouterTransaction.with(MangaController(extras)))
            }
            SHORTCUT_DOWNLOADS -> {
                if (router.backstack.none { it.controller() is DownloadController }) {
                    setSelectedDrawerItem(R.id.nav_drawer_downloads)
                }
            }
            Intent.ACTION_SEARCH, "com.google.android.gms.actions.SEARCH_ACTION" -> {
                //If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search *query* on *Tachiyomi*" in Google Search/Google Assistant)

                setSelectedDrawerItem(R.id.nav_drawer_browse)
                //Get the search query provided in extras, and if not null, perform a global search with it.
                intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                    router.pushController(BrowseCatalogueController(source).withFadeTransaction())
                }
            }
            else -> return false
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        nav_view?.setNavigationItemSelectedListener(null)
        toolbar?.setNavigationOnClickListener(null)
    }

    override fun onBackPressed() {
        val backstackSize = router.backstackSize
        if (drawer.isDrawerOpen(GravityCompat.START) || drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawers()
        } else if (backstackSize == 1 && router.getControllerWithTag("$startScreenId") == null) {
            setSelectedDrawerItem(startScreenId)
        } else if (backstackSize == 1 || !router.handleBack()) {
            super.onBackPressed()
        }
    }

    private fun setSelectedDrawerItem(itemId: Int) {
        if (!isFinishing) {
            nav_view.setCheckedItem(itemId)
            nav_view.menu.performIdentifierAction(itemId, 0)
        }
    }

    private fun setRoot(controller: Controller, id: Int) {
        router.setRoot(controller.withFadeTransaction().tag(id.toString()))
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun syncActivityViewWithController(to: Controller?, from: Controller? = null) {
        if (from is DialogController || to is DialogController) {
            return
        }

        val showHamburger = router.backstackSize == 1
        if (showHamburger) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }

        ObjectAnimator.ofFloat(drawerArrow, "progress", if (showHamburger) 0f else 1f).start()

        if (from is TabbedController) {
            from.cleanupTabs(tabs)
        }
        if (to is TabbedController) {
            tabAnimator.expand()
            to.configureTabs(tabs)
        } else {
            tabAnimator.collapse()
            tabs.setupWithViewPager(null)
        }

        if (from is SecondaryDrawerController) {
            if (secondaryDrawer != null) {
                from.cleanupSecondaryDrawer(drawer)
                drawer.removeView(secondaryDrawer)
                secondaryDrawer = null
            }
        }
        if (to is SecondaryDrawerController) {
            secondaryDrawer = to.createSecondaryDrawer(drawer)?.also { drawer.addView(it) }
        }

        if (to is NoToolbarElevationController) {
            appbar.disableElevation()
        } else {
            appbar.enableElevation()
        }
    }

    companion object {
        // Shortcut actions
        const val SHORTCUT_LIBRARY = "eu.kanade.tachiyomi.SHOW_LIBRARY"
        const val SHORTCUT_RECENTLY_UPDATED = "eu.kanade.tachiyomi.SHOW_RECENTLY_UPDATED"
        const val SHORTCUT_RECENTLY_READ = "eu.kanade.tachiyomi.SHOW_RECENTLY_READ"
        const val SHORTCUT_CATALOGUES = "eu.kanade.tachiyomi.SHOW_BROWSE"
        const val SHORTCUT_DOWNLOADS = "eu.kanade.tachiyomi.SHOW_DOWNLOADS"
        const val SHORTCUT_MANGA = "eu.kanade.tachiyomi.SHOW_MANGA"
    }

}
