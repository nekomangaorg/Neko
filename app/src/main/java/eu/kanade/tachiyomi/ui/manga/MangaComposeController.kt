package eu.kanade.tachiyomi.ui.manga

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.content.getSystemService
import androidx.palette.graphics.Palette
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CategoryActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CoverActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergeActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.TrackActions
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.similar.SimilarController
import eu.kanade.tachiyomi.util.getSlug
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.getBestColor
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.sharedCacheDir
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.launch
import org.nekomanga.presentation.screens.MangaScreen
import uy.kohesive.injekt.injectLazy

class MangaComposeController(val mangaId: Long) : BaseComposeController<MangaComposePresenter>() {

    constructor(bundle: Bundle) : this(bundle.getLong(MangaDetailsController.MANGA_EXTRA)) {
        val notificationId = bundle.getInt("notificationId", -1)
        val context = applicationContext ?: return
        if (notificationId > -1) NotificationReceiver.dismissNotification(
            context,
            notificationId,
            bundle.getInt("groupId", 0),
        )
    }

    override val presenter = MangaComposePresenter(mangaId)

    private val preferences: PreferencesHelper by injectLazy()

    @Composable
    override fun ScreenContent() {
        MangaScreen(
            manga = presenter.manga.collectAsState().value,
            altTitles = presenter.altTitles.collectAsState(),
            artwork = presenter.currentArtwork.collectAsState(),
            isRefreshing = presenter.isRefreshing.collectAsState(),
            onRefresh = presenter::onRefresh,
            categories = presenter.allCategories.collectAsState(),
            mangaCategories = presenter.mangaCategories.collectAsState(),
            categoryActions = CategoryActions(
                set = { enabledCategories -> presenter.updateMangaCategories(enabledCategories) },
                addNew = { newCategory -> presenter.addNewCategory(newCategory) },
            ),
            generatePalette = this::setPalette,
            themeBasedOffCover = preferences.themeMangaDetails(),
            titleLongClick = { context, content -> copyToClipboard(context, content, R.string.title) },
            creatorLongClick = { context, content -> copyToClipboard(context, content, R.string.creator) },
            toggleFavorite = presenter::toggleFavorite,
            loggedInTrackingServices = presenter.loggedInTrackingService.collectAsState(),
            trackServiceCount = presenter.trackServiceCount.collectAsState(),
            tracks = presenter.tracks.collectAsState(),
            trackSuggestedDates = presenter.trackSuggestedDates.collectAsState(),
            dateFormat = preferences.dateFormat(),
            trackActions = TrackActions(
                statusChange = { statusIndex, trackAndService -> presenter.updateTrackStatus(statusIndex, trackAndService) },
                scoreChange = { statusIndex, trackAndService -> presenter.updateTrackScore(statusIndex, trackAndService) },
                chapterChange = { newChapterNumber, trackAndService -> presenter.updateTrackChapter(newChapterNumber, trackAndService) },
                search = { title, service -> presenter.searchTracker(title, service) },
                searchItemClick = { trackAndService -> presenter.registerTracking(trackAndService) },
                remove = { alsoRemoveFromTracker, service -> presenter.removeTracking(alsoRemoveFromTracker, service) },
                dateChange = { trackDateChange -> presenter.updateTrackDate(trackDateChange) },
            ),
            trackSearchResult = presenter.trackSearchResult.collectAsState(),
            alternativeArtwork = presenter.alternativeArtwork.collectAsState(),
            isMergedManga = presenter.isMerged.collectAsState(),
            mergeActions = MergeActions(
                remove = presenter::removeMergedManga,
                search = presenter::searchMergedManga,
                add = presenter::addMergedManga,
            ),
            mergeSearchResult = presenter.mergeSearchResult.collectAsState(),
            similarClick = { router.pushController(SimilarController(presenter.manga.value).withFadeTransaction()) },
            externalLinks = presenter.externalLinks.collectAsState(),
            shareClick = { context -> viewScope.launch { shareManga(context) } },
            coverActions = CoverActions(
                share = { context, url -> viewScope.launch { shareCover(context, url) } },
                set = { url -> viewScope.launch { setCover(url) } },
                save = { url -> viewScope.launch { saveCover(url) } },
                reset = { viewScope.launch { resetCover() } },
            ),
            genreClick = {},
            genreLongClick = {},
            quickReadText = presenter.quickReadText.collectAsState(),
            quickReadClick = {},
            chapterHeaderClick = {},
            chapterFilterText = "",
            chapters = presenter.activeChapters.collectAsState(),
            removedChapters = presenter.removedChapters.collectAsState(),
            chapterActions = ChapterActions(
                deleteChapters = { chapterItems -> presenter.deleteChapters(chapterItems) },
                clearRemovedChapters = presenter::clearRemovedChapters,
                openChapter = { context, chapterItem -> startActivity(ReaderActivity.newIntent(context, presenter.manga.value, chapterItem.chapter.toDbChapter())) },
            ),
        ) { activity?.onBackPressed() }
    }

    private fun setPalette(drawable: Drawable) {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
        // Generate the Palette on a background thread.

        Palette.from(bitmap).generate {
            it ?: return@generate
            viewScope.launchUI {
                val vibrantColor = it.getBestColor() ?: return@launchUI
                presenter.updateMangaColor(vibrantColor)
            }
        }
    }

    /**
     * Copy the Device and App info to clipboard
     */
    private fun copyToClipboard(context: Context, content: String, @StringRes label: Int) {
        val clipboard = context.getSystemService<ClipboardManager>()!!
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(label), content))
    }

    /**
     * Save the image of the given image
     */
    suspend fun saveCover(urlOfCover: String) {
        presenter.saveCover(urlOfCover)
    }

    /**
     * Share a cover with the given url
     */
    suspend fun shareCover(context: Context, urlOfCover: String) {
        val cover = presenter.shareMangaCover(context.sharedCacheDir(), urlOfCover)
        withUIContext {
            val stream = cover?.getUriCompat(context)
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, stream)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    clipData = ClipData.newRawUri(null, stream)
                    type = "image/*"
                }
                startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
            } catch (e: Exception) {
                context.toast(e.message)
            }
        }
    }

    suspend fun setCover(urlOfCover: String) {
        withUIContext {
            presenter.setCover(urlOfCover)
        }
    }

    suspend fun resetCover() {
        withUIContext {
            presenter.resetCover()
        }
    }

    /**
     * Share the given manga
     */
    suspend fun shareManga(context: Context) {
        val cover = presenter.shareMangaCover(context.sharedCacheDir())
        withUIContext {
            val stream = cover?.getUriCompat(context)
            try {
                val manga = presenter.manga.value
                var url = presenter.sourceManager.getMangadex().mangaDetailsRequest(manga).url.toString()
                url = "$url/" + presenter.manga.value.getSlug()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/*"
                    putExtra(Intent.EXTRA_TEXT, url)
                    putExtra(Intent.EXTRA_TITLE, manga.title)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    if (stream != null) {
                        clipData = ClipData.newRawUri(null, stream)
                    }
                }
                startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
            } catch (e: Exception) {
                context.toast(e.message)
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (presenter.isScopeInitialized) {
            presenter.resume()
        }
    }
}
