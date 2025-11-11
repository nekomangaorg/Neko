/*
package eu.kanade.tachiyomi.ui.main

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.ui.source.browse.BrowseController
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.isAvailable
import eu.kanade.tachiyomi.util.manga.MangaMappings
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import java.math.BigInteger
import org.nekomanga.constants.MdConstants
import org.nekomanga.presentation.screens.Screens
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SearchActivity : MainActivity() {

    var backToMain = false

    private val mappings: MangaMappings by injectLazy()
    private val downloadManager: DownloadManager by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    // Override finishAfterTransition since the animation gets weird when launching this from other
    // apps
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
            val intent =
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            backToMain = true
            startActivity(intent)
            finishAfterTransition()
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
            Intent.ACTION_SEARCH,
            Intent.ACTION_SEND,
            "com.google.android.gms.actions.SEARCH_ACTION" -> {
                // If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search
                // *query* on *Tachiyomi*" in Google Search/Google Assistant)

                // Get the search query provided in extras, and if not null, perform a global search
                // with it.
                val query =
                    intent.getStringExtra(SearchManager.QUERY)
                        ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                if (query != null && query.isNotEmpty()) {
                    router.replaceTopController(BrowseController(query).withFadeTransaction())
                } else {
                    finish()
                }
            }
            INTENT_SEARCH -> {
                val host = intent.data?.host
                val pathSegments = intent.data?.pathSegments
                if (host != null && pathSegments != null && pathSegments.size > 1) {
                    val path = pathSegments[0]
                    val id = pathSegments[1]
                    if (id != null && id.isNotEmpty()) {
                        if (router.backstackSize > 1) {
                            router.popToRoot()
                        }

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
                        router.replaceTopController(BrowseController(query).withFadeTransaction())
                    }
                }
            }
            SHORTCUT_MANGA,
            SHORTCUT_MANGA_BACK -> {
                val extras = intent.extras ?: return false
                if (
                    intent.action == SHORTCUT_MANGA_BACK &&
                        preferences.openChapterInShortcuts().get()
                ) {
                    val mangaId = extras.getLong(MangaDetailController.MANGA_EXTRA)
                    if (mangaId != 0L) {
                        val db = Injekt.get<DatabaseHelper>()
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
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            SHORTCUT_SOURCE -> {
                val extras = intent.extras ?: return false
                SecureActivityDelegate.promptLockIfNeeded(this, true)
                router.replaceTopController(
                    RouterTransaction.with(BrowseController())
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            SHORTCUT_READER_SETTINGS -> {
                val settingsController = SettingsController()
                settingsController.presenter.deepLink = Screens.Settings.Reader
                router.replaceTopController(
                    RouterTransaction.with(settingsController)
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler())
                )
            }
            else -> return false
        }
        return true
    }

    companion object {
        fun openMangaIntent(context: Context, id: Long?, canReturnToMain: Boolean = false) =
            Intent(context, SearchActivity::class.java).apply {
                action = if (canReturnToMain) SHORTCUT_MANGA_BACK else SHORTCUT_MANGA
                putExtra(MangaDetailController.MANGA_EXTRA, id)
            }

        fun openReaderSettings(context: Context) =
            Intent(context, SearchActivity::class.java).apply { action = SHORTCUT_READER_SETTINGS }
    }
}
*/
