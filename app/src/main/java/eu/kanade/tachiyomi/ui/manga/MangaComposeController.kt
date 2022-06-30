package eu.kanade.tachiyomi.ui.manga

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
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.getBestColor
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CategoryActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.TrackActions
import eu.kanade.tachiyomi.ui.similar.SimilarController
import eu.kanade.tachiyomi.util.getSlug
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.sharedCacheDir
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.launch
import org.nekomanga.presentation.screens.MangaScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MangaComposeController(val manga: Manga) : BaseComposeController<MangaComposePresenter>() {

    constructor(mangaId: Long) : this(
        Injekt.get<DatabaseHelper>().getManga(mangaId).executeAsBlocking()!!,
    )

    constructor(bundle: Bundle) : this(bundle.getLong(MangaDetailsController.MANGA_EXTRA)) {
        val notificationId = bundle.getInt("notificationId", -1)
        val context = applicationContext ?: return
        if (notificationId > -1) NotificationReceiver.dismissNotification(
            context,
            notificationId,
            bundle.getInt("groupId", 0),
        )
    }

    override val presenter = MangaComposePresenter(manga)

    private val preferences: PreferencesHelper by injectLazy()

    @Composable
    override fun ScreenContent() {
        MangaScreen(
            manga = manga,
            isRefreshing = presenter.isRefreshing.collectAsState(),
            onRefresh = presenter::onRefresh,
            categories = presenter.allCategories.collectAsState(),
            mangaCategories = presenter.mangaCategories.collectAsState(),
            categoryActions = CategoryActions(
                setCategories = { enabledCategories -> presenter.updateMangaCategories(manga, enabledCategories) },
                addNewCategory = { newCategory -> presenter.addNewCategory(newCategory) },
            ),
            generatePalette = { setPalette(it) },
            themeBasedOffCover = preferences.themeMangaDetails(),
            titleLongClick = { context, content -> copyToClipboard(context, content, R.string.title) },
            creatorLongClick = { context, content -> copyToClipboard(context, content, R.string.creator) },
            toggleFavorite = { presenter.toggleFavorite() },
            loggedInTrackingServices = presenter.loggedInTrackingService.collectAsState(),
            trackServiceCount = presenter.trackServiceCount.collectAsState(),
            tracks = presenter.tracks.collectAsState(),
            trackSuggestedDates = presenter.trackSuggestedDates.collectAsState(),
            dateFormat = preferences.dateFormat(),
            trackActions = TrackActions(
                trackStatusChanged = { statusIndex, trackAndService -> presenter.updateTrackStatus(statusIndex, trackAndService) },
                trackScoreChanged = { statusIndex, trackAndService -> presenter.updateTrackScore(statusIndex, trackAndService) },
                trackChapterChanged = { newChapterNumber, trackAndService -> presenter.updateTrackChapter(newChapterNumber, trackAndService) },
                searchTracker = { title, service -> presenter.searchTracker(title, service) },
                trackSearchItemClick = { trackAndService -> presenter.registerTracking(trackAndService) },
                trackingRemoved = { alsoRemoveFromTracker, service -> presenter.removeTracking(alsoRemoveFromTracker, service) },
                trackingDateChanged = { trackDateChange -> presenter.updateTrackDate(trackDateChange) },
            ),
            trackSearchResult = presenter.trackSearchResult.collectAsState(),
            artworkClick = { },
            isMerged = presenter.isMerged.collectAsState(),
            similarClick = { router.pushController(SimilarController(manga).withFadeTransaction()) },
            externalLinks = presenter.externalLinks.collectAsState(),
            shareClick = { context -> viewScope.launch { shareManga(context) } },
            genreClick = {},
            genreLongClick = {},
            quickReadText = "",
            quickReadClick = {},
            numberOfChapters = 0,
            chapterHeaderClick = {},
            chapterFilterText = "",
        ) { activity?.onBackPressed() }
    }

    private fun setPalette(drawable: Drawable) {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
        // Generate the Palette on a background thread.

        Palette.from(bitmap).generate {
            it ?: return@generate
            viewScope.launchUI {
                val vibrantColor = it.getBestColor() ?: return@launchUI
                manga.vibrantCoverColor = vibrantColor
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

    suspend fun shareManga(context: Context) {
        val cover = presenter.shareMangaCover(context.sharedCacheDir())
        withUIContext {
            val stream = cover?.getUriCompat(context)
            try {
                var url = presenter.sourceManager.getMangadex().mangaDetailsRequest(manga).url.toString()
                url = "$url/" + manga.getSlug()
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
}
