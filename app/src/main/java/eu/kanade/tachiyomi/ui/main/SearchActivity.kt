package eu.kanade.tachiyomi.ui.main

import android.animation.ObjectAnimator
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.material.tabs.TabLayout
import eu.kanade.tachiyomi.Migrations
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.NoToolbarElevationController
import eu.kanade.tachiyomi.ui.base.controller.SecondaryDrawerController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.catalogue.CatalogueController
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchController
import eu.kanade.tachiyomi.ui.download.DownloadController
import eu.kanade.tachiyomi.ui.extension.ExtensionController
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.recent_updates.RecentChaptersController
import eu.kanade.tachiyomi.ui.recently_read.RecentlyReadController
import eu.kanade.tachiyomi.ui.setting.SettingsMainController
import eu.kanade.tachiyomi.util.doOnApplyWindowInsets
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.marginBottom
import eu.kanade.tachiyomi.util.marginTop
import eu.kanade.tachiyomi.util.openInBrowser
import eu.kanade.tachiyomi.util.updateLayoutParams
import eu.kanade.tachiyomi.util.updatePadding
import eu.kanade.tachiyomi.util.updatePaddingRelative
import kotlinx.android.synthetic.main.search_activity.*
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

class SearchActivity: MainActivity() {
    override var trulyGoBack = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.search_activity)

        setSupportActionBar(sToolbar)

        drawerArrow = DrawerArrowDrawable(this)
        drawerArrow?.color = Color.WHITE
        sToolbar.navigationIcon = drawerArrow

        tabAnimator = TabsAnimator(sTabs)

        val container: ViewGroup = findViewById(R.id.controller_container)

        val content: LinearLayout = findViewById(R.id.main_content)
        container.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        content.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        content.setOnApplyWindowInsetsListener { v, insets ->
            window.navigationBarColor =
                // if the os does not support light nav bar and is portrait, draw a dark translucent
                // nav bar
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        (v.rootWindowInsets.systemWindowInsetLeft > 0 ||
                            v.rootWindowInsets.systemWindowInsetRight > 0))
                    // For lollipop, draw opaque nav bar
                        Color.BLACK
                    else Color.argb(179, 0, 0, 0)
                }
                // if the android q+ device has gesture nav, transparent nav bar
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && (v.rootWindowInsets.systemWindowInsetBottom != v.rootWindowInsets
                        .tappableElementInsets.bottom)) {
                    getColor(android.R.color.transparent)
                }
                // if in landscape with 2/3 button mode, fully opaque nav bar
                else if (v.rootWindowInsets.systemWindowInsetLeft > 0
                    || v.rootWindowInsets.systemWindowInsetRight > 0) {
                    getResourceColor(android.R.attr.colorBackground)
                }
                // if in portrait with 2/3 button mode, translucent nav bar
                else {
                    ColorUtils.setAlphaComponent(
                        getResourceColor(android.R.attr.colorBackground), 179)
                }
            v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, 0)
            insets
        }
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (Build.VERSION.SDK_INT >= 26 && currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            content.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        val drawerContainer: FrameLayout = findViewById(R.id.search_container)
        drawerContainer.setOnApplyWindowInsetsListener { v, insets ->
            window.statusBarColor = getResourceColor(R.attr.colorPrimary)
            val contextView = window?.decorView?.findViewById<View>(R.id.action_mode_bar)
            contextView?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.systemWindowInsetLeft
                rightMargin = insets.systemWindowInsetRight
            }
            // Consume any horizontal insets and pad all content in. There's not much we can do
            // with horizontal insets
            v.updatePadding(
                left = insets.systemWindowInsetLeft,
                right = insets.systemWindowInsetRight
            )
            insets.replaceSystemWindowInsets(
                0, insets.systemWindowInsetTop,
                0, insets.systemWindowInsetBottom
            )
        }

        router = Conductor.attachRouter(this, container, savedInstanceState)
        if (!router.hasRootController()) {
            // Set start screen
            handleIntentAction(intent)
        }

        sToolbar.setNavigationOnClickListener {
            popToRoot()
        }

        router.addChangeListener(object : ControllerChangeHandler.ControllerChangeListener {
            override fun onChangeStarted(to: Controller?, from: Controller?, isPush: Boolean,
                container: ViewGroup, handler: ControllerChangeHandler
            ) {

                syncActivityViewWithController(to, from)
            }

            override fun onChangeCompleted(to: Controller?, from: Controller?, isPush: Boolean,
                container: ViewGroup, handler: ControllerChangeHandler
            ) {

            }

        })

        syncActivityViewWithController(router.backstack.lastOrNull()?.controller())
    }

    private fun Context.popToRoot() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finishAfterTransition()
    }

    override fun syncActivityViewWithController(to: Controller?, from: Controller?) {
        if (from is DialogController || to is DialogController) {
            return
        }
        drawerArrow?.progress = 1f

        if (from is TabbedController) {
            from.cleanupTabs(sTabs)
        }
        if (to is TabbedController) {
            tabAnimator.expand()
            to.configureTabs(sTabs)
        } else {
            tabAnimator.collapse()
            sTabs.setupWithViewPager(null)
        }

        if (to is NoToolbarElevationController) {
            appbar.disableElevation()
        } else {
            appbar.enableElevation()
        }
    }

    override fun handleIntentAction(intent: Intent): Boolean {
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId > -1) NotificationReceiver.dismissNotification(
            applicationContext, notificationId, intent.getIntExtra("groupId", 0)
        )
        when (intent.action) {
            Intent.ACTION_SEARCH, "com.google.android.gms.actions.SEARCH_ACTION" -> {
                //If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search *query* on *Tachiyomi*" in Google Search/Google Assistant)

                //Get the search query provided in extras, and if not null, perform a global search with it.
                val query = intent.getStringExtra(SearchManager.QUERY)
                if (query != null && query.isNotEmpty()) {
                    router.replaceTopController(CatalogueSearchController(query).withFadeTransaction())
                }
            }
            INTENT_SEARCH -> {
                val query = intent.getStringExtra(INTENT_SEARCH_QUERY)
                val filter = intent.getStringExtra(INTENT_SEARCH_FILTER)
                if (query != null && query.isNotEmpty()) {
                    if (router.backstackSize > 1) {
                        router.popToRoot()
                    }
                    router.replaceTopController(CatalogueSearchController(query, filter).withFadeTransaction())
                }
            }
            else -> return false
        }
        return true
    }
}