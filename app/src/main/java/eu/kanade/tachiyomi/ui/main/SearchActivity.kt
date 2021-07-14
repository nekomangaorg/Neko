package eu.kanade.tachiyomi.ui.main

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.source.online.handlers.SearchHandler
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.ui.setting.SettingsReaderController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchActivity : MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbar.navigationIcon = drawerArrow
        binding.cardToolbar.navigationIcon = drawerArrow
        binding.toolbar.setNavigationOnClickListener { popToRoot() }
        binding.cardToolbar.setNavigationOnClickListener { popToRoot() }
        (router.backstack.lastOrNull()?.controller as? BaseController<*>)?.setTitle()
        (router.backstack.lastOrNull()?.controller as? SettingsController)?.setTitle()
    }

    override fun onBackPressed() {
        if (router.backstack.size <= 1 || !router.handleBack()) {
            SecureActivityDelegate.locked = true
            super.onBackPressed()
        }
    }

    private fun popToRoot() {
        if (intentShouldGoBack()) {
            onBackPressed()
        } else if (!router.handleBack()) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finishAfterTransition()
        }
    }

    override fun setFloatingToolbar(show: Boolean, solidBG: Boolean, changeBG: Boolean) {
        super.setFloatingToolbar(show, solidBG, changeBG)
        currentToolbar?.setNavigationOnClickListener { popToRoot() }
    }

    private fun intentShouldGoBack() =
        intent.action in listOf(SHORTCUT_MANGA, SHORTCUT_READER_SETTINGS, SHORTCUT_BROWSE)

    override fun syncActivityViewWithController(
        to: Controller?,
        from: Controller?,
        isPush:
            Boolean
    ) {
        if (from is DialogController || to is DialogController) {
            return
        }
        setFloatingToolbar(canShowFloatingToolbar(to))
        binding.cardToolbar.navigationIcon = drawerArrow
        binding.toolbar.navigationIcon = drawerArrow
        drawerArrow?.progress = 1f

        nav.isVisible = false
        binding.bottomView?.isVisible = false
    }

    override fun handleIntentAction(intent: Intent): Boolean {
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId > -1) NotificationReceiver.dismissNotification(
            applicationContext,
            notificationId,
            intent.getIntExtra("groupId", 0)
        )
        when (intent.action) {
            Intent.ACTION_SEARCH, Intent.ACTION_SEND, "com.google.android.gms.actions.SEARCH_ACTION" -> {
                // If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search *query* on *Tachiyomi*" in Google Search/Google Assistant)

                // Get the search query provided in extras, and if not null, perform a global search with it.
                val query = intent.getStringExtra(SearchManager.QUERY) ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                if (query != null && query.isNotEmpty()) {
                    router.replaceTopController(BrowseSourceController(query).withFadeTransaction())
                } else {
                    finish()
                }
            }
            INTENT_SEARCH -> {
                val pathSegments = intent?.data?.pathSegments
                if (pathSegments != null && pathSegments.size > 1) {
                    XLog.e(pathSegments[0])
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
            SHORTCUT_MANGA, SHORTCUT_MANGA_BACK -> {
                val extras = intent.extras ?: return false
                if (intent.action == SHORTCUT_MANGA_BACK && preferences.openChapterInShortcuts()) {
                    val mangaId = extras.getLong(MangaDetailsController.MANGA_EXTRA)
                    if (mangaId != 0L) {
                        val db = Injekt.get<DatabaseHelper>()
                        val chapters = db.getChapters(mangaId).executeAsBlocking()
                        db.getManga(mangaId).executeAsBlocking()?.let { manga ->
                            val nextUnreadChapter = ChapterSort(manga).getNextUnreadChapter(chapters, false)
                            if (nextUnreadChapter != null) {
                                val activity =
                                    ReaderActivity.newIntent(this, manga, nextUnreadChapter)
                                startActivity(activity)
                                finish()
                                return true
                            }
                        }
                    }
                }
                router.replaceTopController(
                    RouterTransaction.with(MangaDetailsController(extras))
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            SHORTCUT_SOURCE -> {
                val extras = intent.extras ?: return false
                router.replaceTopController(
                    RouterTransaction.with(BrowseSourceController(extras))
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            SHORTCUT_READER_SETTINGS -> {
                router.replaceTopController(
                    RouterTransaction.with(SettingsReaderController())
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            else -> return false
        }
        return true
    }

    companion object {
        fun openMangaIntent(context: Context, id: Long?, canReturnToMain: Boolean = false) = Intent(
            context,
            SearchActivity::class
                .java
        )
            .apply {
                action = if (canReturnToMain) SHORTCUT_MANGA_BACK else SHORTCUT_MANGA
                putExtra(MangaDetailsController.MANGA_EXTRA, id)
            }

        fun openReaderSettings(context: Context) = Intent(
            context,
            SearchActivity::class
                .java
        )
            .apply {
                action = SHORTCUT_READER_SETTINGS
            }
    }
}
