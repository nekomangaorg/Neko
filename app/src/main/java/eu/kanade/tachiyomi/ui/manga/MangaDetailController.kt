package eu.kanade.tachiyomi.ui.manga

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CategoryActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.ChapterFilterActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.CoverActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.DescriptionActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.InformationActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.MergeActions
import eu.kanade.tachiyomi.ui.manga.MangaConstants.TrackActions
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.recents.RecentsController
import eu.kanade.tachiyomi.ui.similar.SimilarController
import eu.kanade.tachiyomi.ui.source.browse.BrowseController
import eu.kanade.tachiyomi.ui.source.latest.DisplayController
import eu.kanade.tachiyomi.util.getSlug
import eu.kanade.tachiyomi.util.storage.getUriWithAuthority
import eu.kanade.tachiyomi.util.system.getBestColor
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.sharedCacheDir
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.system.withUIContext
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.presentation.screens.MangaScreen
import uy.kohesive.injekt.injectLazy

class MangaDetailController(private val mangaId: Long) :
    BaseComposeController<MangaDetailPresenter>(Bundle().apply { putLong(MANGA_EXTRA, mangaId) }) {

    constructor(bundle: Bundle) : this(bundle.getLong(MANGA_EXTRA)) {
        val notificationId = bundle.getInt("notificationId", -1)
        val context = applicationContext ?: return
        if (notificationId > -1) {
            NotificationReceiver.dismissNotification(
                context,
                notificationId,
                bundle.getInt("groupId", 0),
            )
        }
    }

    override val presenter = MangaDetailPresenter(mangaId)

    private val preferences: PreferencesHelper by injectLazy()

    @Composable
    override fun ScreenContent() {
        val windowSizeClass = calculateWindowSizeClass(this.activity!!)
        val context = LocalContext.current
        MangaScreen(
            generalState = presenter.generalState.collectAsStateWithLifecycle(),
            mangaState = presenter.mangaState.collectAsStateWithLifecycle(),
            trackMergeState = presenter.trackMergeState.collectAsStateWithLifecycle(),
            snackbar = presenter.snackBarState,
            windowSizeClass = windowSizeClass,
            isRefreshing = presenter.isRefreshing.collectAsStateWithLifecycle(),
            isSearching = presenter.isSearching.collectAsStateWithLifecycle(),
            onRefresh = presenter::onRefresh,
            onSearch = presenter::onSearch,
            categoryActions =
                CategoryActions(
                    set = { enabledCategories ->
                        presenter.updateMangaCategories(enabledCategories)
                    },
                    addNew = { newCategory -> presenter.addNewCategory(newCategory) },
                ),
            informationActions =
                InformationActions(
                    titleLongClick = {
                        presenter.copiedToClipboard(it)
                        copyToClipboard(context, it, R.string.title)
                    },
                    creatorCopy = {
                        presenter.copiedToClipboard(it)
                        copyToClipboard(context, it, R.string.creator)
                    },
                    creatorSearch = this::creatorClicked,
                ),
            descriptionActions =
                DescriptionActions(
                    genreSearch = this::genreSearch,
                    genreSearchLibrary = this::genreSearchLibrary,
                    altTitleClick = presenter::setAltTitle,
                    altTitleResetClick = { presenter.setAltTitle(null) },
                ),
            generatePalette = this::setPalette,
            toggleFavorite = presenter::toggleFavorite,
            dateFormat = preferences.dateFormat(),
            trackActions =
                TrackActions(
                    statusChange = { statusIndex, trackAndService ->
                        presenter.updateTrackStatus(statusIndex, trackAndService)
                    },
                    scoreChange = { statusIndex, trackAndService ->
                        presenter.updateTrackScore(statusIndex, trackAndService)
                    },
                    chapterChange = { newChapterNumber, trackAndService ->
                        presenter.updateTrackChapter(newChapterNumber, trackAndService)
                    },
                    search = { title, service -> presenter.searchTracker(title, service) },
                    searchItemClick = { trackAndService ->
                        presenter.registerTracking(trackAndService)
                    },
                    remove = { alsoRemoveFromTracker, service ->
                        presenter.removeTracking(alsoRemoveFromTracker, service)
                    },
                    dateChange = { trackDateChange -> presenter.updateTrackDate(trackDateChange) },
                ),
            mergeActions =
                MergeActions(
                    remove = presenter::removeMergedManga,
                    search = presenter::searchMergedManga,
                    add = presenter::addMergedManga,
                ),
            similarClick = {
                router.pushController(
                    SimilarController(presenter.manga.value!!.uuid()).withFadeTransaction()
                )
            },
            shareClick = { shareManga(context) },
            coverActions =
                CoverActions(
                    share = this::shareCover,
                    set = presenter::setCover,
                    save = presenter::saveCover,
                    reset = presenter::resetCover,
                ),
            chapterFilterActions =
                ChapterFilterActions(
                    changeSort = presenter::changeSortOption,
                    changeFilter = presenter::changeFilterOption,
                    changeScanlator = presenter::changeScanlatorOption,
                    changeLanguage = presenter::changeLanguageOption,
                    setAsGlobal = presenter::setGlobalOption,
                ),
            chapterActions =
                ChapterActions(
                    mark = presenter::markChapters,
                    download = { chapterItems, downloadAction ->
                        if (
                            chapterItems.size == 1 &&
                                MdConstants.UnsupportedOfficialGroupList.contains(
                                    chapterItems[0].chapter.scanlator
                                )
                        ) {
                            context.toast(
                                "${chapterItems[0].chapter.scanlator} not supported, try WebView"
                            )
                        } else {
                            presenter.downloadChapters(chapterItems, downloadAction)
                        }
                    },
                    delete = presenter::deleteChapters,
                    clearRemoved = presenter::clearRemovedChapters,
                    openNext = {
                        presenter.generalState.value.nextUnreadChapter.simpleChapter?.let {
                            openChapter(context, it.toDbChapter())
                        }
                    },
                    open = { chapterItem ->
                        openChapter(context, chapterItem.chapter.toDbChapter())
                    },
                    blockScanlator = presenter::blockScanlator,
                    openComment = { chapterId -> presenter.openComment(context, chapterId) },
                    openInBrowser = { chapterItem ->
                        val url = presenter.getChapterUrl(chapterItem.chapter)
                        context.openInBrowser(url)
                    },
                ),
            onBackPressed = {
                when (router.backstackSize > 1) {
                    true -> router.handleBack()
                    false -> activity?.onBackPressed()
                }
            },
        )
    }

    private fun openChapter(context: Context, chapter: Chapter) {
        if (
            chapter.scanlator != null &&
                MdConstants.UnsupportedOfficialGroupList.contains(chapter.scanlator)
        ) {
            context.toast("${chapter.scanlator} not supported, try WebView")
        } else {
            startActivity(ReaderActivity.newIntent(context, presenter.manga.value!!, chapter))
        }
    }

    /** Generate palette from the drawable */
    private fun setPalette(drawable: Drawable) {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
        Palette.from(bitmap).generate {
            it ?: return@generate
            viewScope.launchUI {
                val vibrantColor = it.getBestColor() ?: return@launchUI
                presenter.updateMangaColor(vibrantColor)
            }
        }
    }

    /** Copy the Device and App info to clipboard */
    private fun copyToClipboard(context: Context, content: String, @StringRes label: Int) {
        val clipboard = context.getSystemService<ClipboardManager>()!!
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(label), content))
    }

    /** Share a cover with the given url */
    fun shareCover(context: Context, artwork: Artwork) {
        viewScope.launch {
            val dir = context.sharedCacheDir() ?: throw Exception("Error accessing cache dir")
            val cover = presenter.shareMangaCover(dir, artwork)
            val sharableCover = cover?.getUriWithAuthority(context)
            withUIContext {
                try {
                    val intent =
                        Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, sharableCover)
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            clipData = ClipData.newRawUri(null, sharableCover)
                            type = "image/*"
                        }
                    startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                } catch (e: Exception) {
                    context.toast(e.message)
                }
            }
        }
    }

    /** Share the given manga */
    private fun shareManga(context: Context) {
        viewScope.launch {
            val dir = context.sharedCacheDir() ?: throw Exception("Error accessing cache dir")

            val cover = presenter.shareMangaCover(dir, presenter.mangaState.value.currentArtwork)
            val sharableCover = cover?.getUriWithAuthority(context)

            withUIContext {
                try {
                    val manga = presenter.manga.value!!
                    var url =
                        presenter.sourceManager.mangaDex.mangaDetailsRequest(manga).url.toString()
                    url = "$url/" + manga.getSlug()
                    val intent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/*"
                            putExtra(Intent.EXTRA_TEXT, url)
                            putExtra(Intent.EXTRA_TITLE, manga.title)
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            if (cover != null) {
                                clipData = ClipData.newRawUri(null, sharableCover)
                            }
                        }
                    startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                } catch (e: Exception) {
                    context.toast(e.message)
                }
            }
        }
    }

    /** Search by author on browse screen */
    private fun creatorClicked(text: String) {
        getBrowseController()?.searchByCreator(text)
    }

    /** Search by tag on browse screen */
    private fun genreSearch(text: String) {
        getBrowseController()?.searchByTag(text)
    }

    private fun getBrowseController(backstackNumber: Int = 2): BrowseController? {
        val position = router.backstackSize - backstackNumber
        if (position < 0) return null
        return when (val previousController = router.backstack[position].controller) {
            is LibraryController,
            is RecentsController,
            is DisplayController,
            is SimilarController -> {
                router.popToRoot()
                (activity as? MainActivity)?.goToTab(R.id.nav_browse)
                router.getControllerWithTag(R.id.nav_browse.toString()) as BrowseController
            }
            is BrowseController -> {
                router.popCurrentController()
                previousController
            }
            else -> {
                if (backstackNumber == 1) {
                    null
                } else {
                    getBrowseController(backstackNumber - 1)
                }
            }
        }
    }

    /** Navigate back to library when a tag is long clicked and search there */
    private fun genreSearchLibrary(text: String) {
        if (router.backstackSize < 2) {
            return
        }

        when (val previousController = router.backstack[router.backstackSize - 2].controller) {
            is LibraryController -> {
                router.handleBack()
                previousController.search(text)
            }
            is BrowseController,
            is RecentsController,
            is DisplayController -> {
                // Manually navigate to LibraryController
                router.handleBack()
                (activity as? MainActivity)?.goToTab(R.id.nav_library)
                val controller =
                    router.getControllerWithTag(R.id.nav_library.toString()) as LibraryController
                controller.search(text)
            }
        }
    }

    companion object {
        const val MANGA_EXTRA = "manga"
    }
}
