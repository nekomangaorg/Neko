package eu.kanade.tachiyomi.ui.recents

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.constants.MdConstants
import org.nekomanga.presentation.screens.FeedScreen

class FeedController : BaseComposeController<FeedPresenter>() {
    override val presenter = FeedPresenter()

    @Composable
    override fun ScreenContent() {
        val windowSizeClass = calculateWindowSizeClass(this.activity!!)
        val isSideNav = (this.activity as? MainActivity)?.isSideNavigation() == true

        val context = LocalContext.current

        BackHandler((this.activity as? MainActivity)?.shouldGoToStartingTab() == true) {
            (this.activity as? MainActivity)?.backCallback?.invoke()
        }

        FeedScreen(
            feedScreenState = presenter.feedScreenState.collectAsState(),
            loadNextPage = presenter::loadNextPage,
            windowSizeClass = windowSizeClass,
            incognitoClick = presenter::toggleIncognitoMode,
            legacySideNav = isSideNav,
            feedSettingActions =
                FeedSettingActions(
                    groupHistoryClick = presenter::toggleGroupHistoryType,
                    clearHistoryClick = presenter::deleteAllHistoryForAllManga,
                    switchUploadsSortOrder = presenter::toggleUploadsSortOrder,
                    outlineCoversClick = presenter::toggleOutlineCovers,
                    outlineCardsClick = presenter::toggleOutlineCards,
                    clearDownloadQueueClick = presenter::clearDownloadQueue,
                    toggleDownloadOnlyOnWifi = presenter::toggleDownloadOnlyOnWifi,
                ),
            feedScreenActions =
                FeedScreenActions(
                    mangaClick = ::openManga,
                    chapterClick = { mangaId, chapterId ->
                        openChapter(context, mangaId, chapterId)
                    },
                    switchViewType = presenter::switchViewType,
                    deleteAllHistoryClick = presenter::deleteAllHistory,
                    deleteHistoryClick = presenter::deleteHistory,
                    search = presenter::search,
                    downloadClick = { chapterItem, feedManga, downloadAction ->
                        if (
                            MdConstants.UnsupportedOfficialGroupList.contains(
                                chapterItem.chapter.scanlator
                            )
                        ) {
                            context.toast(
                                "${chapterItem.chapter.scanlator} not supported, try WebView"
                            )
                        } else {
                            presenter.downloadChapter(chapterItem, feedManga, downloadAction)
                        }
                    },
                    toggleShowingDownloads = presenter::toggleShowingDownloads,
                    updateLibrary = { start -> updateLibrary(start, context) },
                ),
            downloadScreenActions =
                DownloadScreenActions(
                    downloadSwiped = presenter::removeDownload,
                    fabClick = presenter::toggleDownloader,
                    moveDownloadClick = presenter::moveDownload,
                    moveSeriesClick = presenter::moveDownloadSeries,
                    cancelSeriesClick = presenter::cancelDownloadSeries,
                ),
            settingsClick = { (this.activity as? MainActivity)?.showSettings() },
            statsClick = { (this.activity as? MainActivity)?.showStats() },
            aboutClick = { (this.activity as? MainActivity)?.showAbout() },
            helpClick = {
                (this.activity as? MainActivity)?.openInBrowser("https://tachiyomi.org/help/")
            },
        )
    }

    private fun openManga(mangaId: Long) {
        viewScope.launchUI {
            router.pushController(MangaDetailController(mangaId).withFadeTransaction())
        }
    }

    private fun openChapter(context: Context, mangaId: Long, chapterId: Long) {
        startActivity(ReaderActivity.newIntent(context, mangaId, chapterId))
    }

    private fun updateLibrary(start: Boolean, context: Context) {
        if (LibraryUpdateJob.isRunning(context) && !start) {
            presenter.refreshing(false)
            LibraryUpdateJob.stop(context)
        } else if (!LibraryUpdateJob.isRunning(context) && start) {
            presenter.refreshing(true)
            LibraryUpdateJob.startNow(context)
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        presenter.updateMangaForChanges()
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        presenter.updateMangaForChanges()
    }
}
