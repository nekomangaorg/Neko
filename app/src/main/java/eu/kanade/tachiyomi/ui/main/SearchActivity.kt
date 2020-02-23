package eu.kanade.tachiyomi.ui.main

import android.app.SearchManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.graphics.ColorUtils
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.NoToolbarElevationController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchController
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePadding
import kotlinx.android.synthetic.main.search_activity.*

class SearchActivity: MainActivity() {
    override var trulyGoBack = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usingBottomNav = false
        setContentView(R.layout.search_activity)

        setSupportActionBar(sToolbar)

        drawerArrow = DrawerArrowDrawable(this)
        drawerArrow?.color = getResourceColor(R.attr.actionBarTintColor)
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
        if (Build.VERSION.SDK_INT >= 26 && currentNightMode == Configuration.UI_MODE_NIGHT_NO &&
            preferences.theme() >= 8) {
            content.systemUiVisibility = content.systemUiVisibility.or(View
                .SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && currentNightMode == Configuration
                .UI_MODE_NIGHT_NO && preferences.theme() >= 8)
            content.systemUiVisibility = content.systemUiVisibility.or(View
                .SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

        val searchContainer: FrameLayout = findViewById(R.id.search_container)
        searchContainer.setOnApplyWindowInsetsListener { v, insets ->
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

    override fun onBackPressed() {
        if (router.backstack.size <= 1 || !router.handleBack()) {
            SecureActivityDelegate.locked = true
            super.onBackPressed()
        }
    }

    private fun popToRoot() {
        if (!router.handleBack()) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finishAfterTransition()
        }
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

    override fun onResume() {
        super.onResume()
        usingBottomNav = false
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