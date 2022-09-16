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
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.SmallToolbarInterface
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
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

    var backToMain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbar.navigationIcon = backDrawable
        binding.toolbar.setNavigationOnClickListener { popToRoot() }
        binding.searchToolbar.setNavigationOnClickListener {
            val rootSearchController = router.backstack.lastOrNull()?.controller
            if ((
                rootSearchController is RootSearchInterface ||
                    (currentToolbar != binding.searchToolbar && binding.appBar.useLargeToolbar)
                ) && rootSearchController !is SmallToolbarInterface
            ) {
                binding.searchToolbar.menu.findItem(R.id.action_search)?.expandActionView()
            } else {
                popToRoot()
            }
        }
        (router.backstack.lastOrNull()?.controller as? BaseController<*>)?.setTitle()
        (router.backstack.lastOrNull()?.controller as? SettingsController)?.setTitle()
    }

    // Override finishAfterTransition since the animation gets weird when launching this from other apps
    override fun finishAfterTransition() {
        if (backToMain) {
            super.finishAfterTransition()
        } else {
            finish()
        }
    }

    override fun backPress() {
        if (router.backstack.size <= 1 || !router.handleBack()) {
            SecureActivityDelegate.locked = true
        }
    }

    private fun popToRoot() {
        if (intentShouldGoBack()) {
            onBackPressedDispatcher.onBackPressed()
        } else if (!router.handleBack()) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            backToMain = true
            startActivity(intent)
            finishAfterTransition()
        }
    }

    override fun setFloatingToolbar(show: Boolean, solidBG: Boolean, changeBG: Boolean, showSearchAnyway: Boolean) {
        super.setFloatingToolbar(show, solidBG, changeBG, showSearchAnyway)
        val useLargeTB = binding.appBar.useLargeToolbar
        if (!useLargeTB) {
            binding.searchToolbar.navigationIcon = backDrawable
        } else if (showSearchAnyway) {
            binding.searchToolbar.navigationIcon =
                if (!show) searchDrawable else backDrawable
        }
    }

    private fun intentShouldGoBack() =
        intent.action in listOf(SHORTCUT_MANGA, SHORTCUT_READER_SETTINGS, SHORTCUT_BROWSE)

    override fun syncActivityViewWithController(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
    ) {
        if (from is DialogController || to is DialogController) {
            return
        }
        reEnableBackPressedCallBack()
        setFloatingToolbar(canShowFloatingToolbar(to))
        nav.isVisible = false
        binding.bottomView?.isVisible = false
    }

    override fun handleIntentAction(intent: Intent): Boolean {
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId > -1) {
            NotificationReceiver.dismissNotification(
                applicationContext,
                notificationId,
                intent.getIntExtra("groupId", 0),
            )
        }
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
                val pathSegments = intent.data?.pathSegments
                if (pathSegments != null && pathSegments.size > 1) {
                    val path = pathSegments[0]
                    val id = pathSegments[1]
                    if (id != null && id.isNotEmpty()) {
                        if (router.backstackSize > 1) {
                            router.popToRoot()
                        }
                        val (query, mangaDeepLink) = if (path.equals("GROUP", true)) {
                            Pair("${MdUtil.PREFIX_GROUP_ID_SEARCH}$id", false)
                        } else {
                            Pair("${MdUtil.PREFIX_ID_SEARCH}$id", true)
                        }
                        router.replaceTopController(
                            BrowseSourceController(
                                query,
                                mangaDeepLink,
                                mangaDeepLink,
                            ).withFadeTransaction(),
                        )
                    }
                }
            }
            SHORTCUT_MANGA, SHORTCUT_MANGA_BACK -> {
                val extras = intent.extras ?: return false
                if (intent.action == SHORTCUT_MANGA_BACK && preferences.openChapterInShortcuts()) {
                    val mangaId = extras.getLong(MangaDetailController.MANGA_EXTRA)
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
                if (intent.action == SHORTCUT_MANGA_BACK) {
                    SecureActivityDelegate.promptLockIfNeeded(this, true)
                }
                router.replaceTopController(
                    RouterTransaction.with(MangaDetailController(extras))
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler()),
                )
            }
            SHORTCUT_SOURCE -> {
                val extras = intent.extras ?: return false
                SecureActivityDelegate.promptLockIfNeeded(this, true)
                router.replaceTopController(
                    RouterTransaction.with(BrowseSourceController(extras))
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler()),
                )
            }
            SHORTCUT_READER_SETTINGS -> {
                router.replaceTopController(
                    RouterTransaction.with(SettingsReaderController())
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler()),
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
                .java,
        )
            .apply {
                action = if (canReturnToMain) SHORTCUT_MANGA_BACK else SHORTCUT_MANGA
                putExtra(MangaDetailController.MANGA_EXTRA, id)
            }

        fun openReaderSettings(context: Context) = Intent(
            context,
            SearchActivity::class
                .java,
        )
            .apply {
                action = SHORTCUT_READER_SETTINGS
            }
    }
}
