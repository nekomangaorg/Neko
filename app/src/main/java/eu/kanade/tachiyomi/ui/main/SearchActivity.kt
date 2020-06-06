package eu.kanade.tachiyomi.ui.main

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.android.synthetic.main.main_activity.*
import timber.log.Timber

class SearchActivity : MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbar?.navigationIcon = backArrow
        toolbar?.setNavigationOnClickListener {
            popToRoot()
        }
    }

    override fun onBackPressed() {
        if (router.backstack.size <= 1 || !router.handleBack()) {
            SecureActivityDelegate.locked = true
            super.onBackPressed()
        }
    }

    private fun popToRoot() {
        if (intent.action == SHORTCUT_MANGA) {
            onBackPressed()
        } else if (!router.handleBack()) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finishAfterTransition()
        }
    }

    override fun syncActivityViewWithController(
        to: Controller?,
        from: Controller?,
        isPush:
        Boolean
    ) {
        if (from is DialogController || to is DialogController) {
            return
        }
        toolbar.navigationIcon = backArrow

        bottom_nav.gone()
    }

    override fun handleIntentAction(intent: Intent): Boolean {
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId > -1) NotificationReceiver.dismissNotification(
            applicationContext, notificationId, intent.getIntExtra("groupId", 0)
        )
        when (intent.action) {
            Intent.ACTION_SEARCH, "com.google.android.gms.actions.SEARCH_ACTION" -> {
                // If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search *query* on *Tachiyomi*" in Google Search/Google Assistant)

                // Get the search query provided in extras, and if not null, perform a global search with it.
                val query = intent.getStringExtra(SearchManager.QUERY)
                if (query != null && query.isNotEmpty()) {
                    router.replaceTopController(BrowseSourceController(query).withFadeTransaction())
                }
            }
            INTENT_SEARCH -> {
                val pathSegments = intent?.data?.pathSegments
                if (pathSegments != null && pathSegments.size > 1) {
                    Timber.e(pathSegments[0])
                    val id = pathSegments[1]
                    if (id != null && id.isNotEmpty()) {
                        if (router.backstackSize > 1) {
                            router.popToRoot()
                        }
                        val query = "${SearchHandler.PREFIX_ID_SEARCH}$id"
                        router.replaceTopController(BrowseSourceController(query, true, true).withFadeTransaction())
                    }
                }
            }
            SHORTCUT_MANGA -> {
                val extras = intent.extras ?: return false
                router.replaceTopController(
                    RouterTransaction.with(MangaDetailsController(extras))
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            else -> return false
        }
        return true
    }

    companion object {
        fun openMangaIntent(context: Context, id: Long) = Intent(
            context, SearchActivity::class
                .java
        )
            .apply {
                action = SHORTCUT_MANGA
                putExtra(MangaDetailsController.MANGA_EXTRA, id)
            }
    }
}
