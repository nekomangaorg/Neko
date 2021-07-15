package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import androidx.annotation.ColorInt
import com.elvishew.xlog.XLog
import com.jakewharton.rxrelay.BehaviorRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.isLongStrip
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.jobs.tracking.DelayedTrackingUpdateJob
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.util.chapter.ChapterFilter
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.storage.DiskUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rx.Completable
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Presenter used by the activity to perform background operations.
 */
class ReaderPresenter(
    private val db: DatabaseHelper = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val chapterFilter: ChapterFilter = Injekt.get(),
) : BasePresenter<ReaderActivity>() {

    /**
     * The manga loaded in the reader. It can be null when instantiated for a short time.
     */
    var manga: Manga? = null
        private set

    /**
     * The chapter id of the currently loaded chapter. Used to restore from process kill.
     */
    private var chapterId = -1L

    /**
     * The chapter loader for the loaded manga. It'll be null until [manga] is set.
     */
    private var loader: ChapterLoader? = null

    /**
     * Subscription to prevent setting chapters as active from multiple threads.
     */
    private var activeChapterSubscription: Subscription? = null

    /**
     * Relay for currently active viewer chapters.
     */
    private val viewerChaptersRelay = BehaviorRelay.create<ViewerChapters>()

    val viewerChapters: ViewerChapters?
        get() = viewerChaptersRelay.value

    /**
     * Relay used when loading prev/next chapter needed to lock the UI (with a dialog).
     */
    private val isLoadingAdjacentChapterRelay = BehaviorRelay.create<Boolean>()

    /**
     * Chapter list for the active manga. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val chapterList by lazy {
        val manga = manga!!
        val dbChapters = db.getChapters(manga).executeAsBlocking()

        val selectedChapter = dbChapters.find { it.id == chapterId }
            ?: error("Requested chapter of id $chapterId not found in chapter list")

        val chaptersForReader =
            chapterFilter.filterChaptersForReader(dbChapters, manga, selectedChapter)

        when (manga.chapterOrder(preferences)) {
            Manga.CHAPTER_SORTING_SOURCE -> ChapterLoadBySource().get(chaptersForReader)
            Manga.CHAPTER_SORTING_NUMBER -> ChapterLoadByNumber().get(chaptersForReader,
                selectedChapter)
            Manga.CHAPTER_SORTING_UPLOAD_DATE -> ChapterLoadByDate().get(chaptersForReader)
            else -> error("Unknown sorting method")
        }.map(::ReaderChapter)
    }

    var chapterItems = emptyList<ReaderChapterItem>()

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    private val statusHandler: StatusHandler by injectLazy()

    private var hasTrackers: Boolean = false
    private val checkTrackers: (Manga) -> Unit = { manga ->
        val tracks = db.getTracks(manga).executeAsBlocking()

        hasTrackers = tracks.size > 0
    }

    /**
     * Called when the presenter is created. It retrieves the saved active chapter if the process
     * was restored.
     */
    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        if (savedState != null) {
            chapterId = savedState.getLong(::chapterId.name, -1)
        }
    }

    /**
     * Called when the presenter instance is being saved. It saves the currently active chapter
     * id and the last page read.
     */
    override fun onSave(state: Bundle) {
        super.onSave(state)
        val currentChapter = getCurrentChapter()
        if (currentChapter != null) {
            currentChapter.requestedPage = currentChapter.chapter.last_page_read
            state.putLong(::chapterId.name, currentChapter.chapter.id!!)
        }
    }

    /**
     * Called when the user pressed the back button and is going to leave the reader. Used to
     * trigger deletion of the downloaded chapters.
     */
    fun onBackPressed() {
        deletePendingChapters()
        val currentChapters = viewerChaptersRelay.value
        if (currentChapters != null) {
            currentChapters.unref()
            saveChapterProgress(currentChapters.currChapter)
            saveChapterHistory(currentChapters.currChapter)
        }
    }

    /**
     * Called when the activity is saved and not changing configurations. It updates the database
     * to persist the current progress of the active chapter.
     */
    fun onSaveInstanceStateNonConfigurationChange() {
        val currentChapter = getCurrentChapter() ?: return
        saveChapterProgress(currentChapter)
    }

    /**
     * Whether this presenter is initialized yet.
     */
    fun needsInit(): Boolean {
        return manga == null
    }

    /**
     * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
     * fetch the manga from the database and initialize the initial chapter.
     */
    fun init(mangaId: Long, initialChapterId: Long) {
        if (!needsInit()) return

        db.getManga(mangaId).asRxObservable()
            .first()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { init(it, initialChapterId) }
            .subscribeFirst(
                { _, _ ->

                    // Ignore onNext event
                },
                ReaderActivity::setInitialChapterError
            )
    }

    suspend fun getChapters(): List<ReaderChapterItem> {
        val manga = manga ?: return emptyList()
        chapterItems = withContext(Dispatchers.IO) {
            val chapterSort = ChapterSort(manga, chapterFilter, preferences)
            val dbChapters = db.getChapters(manga).executeAsBlocking()
            chapterSort.getChaptersSorted(dbChapters,
                filterForReader = true,
                currentChapter = getCurrentChapter()?.chapter).map {
                ReaderChapterItem(
                    it,
                    manga,
                    it.id == getCurrentChapter()?.chapter?.id ?: chapterId
                )
            }
        }

        return chapterItems
    }

    /**
     * Initializes this presenter with the given [manga] and [initialChapterId]. This method will
     * set the chapter loader, view subscriptions and trigger an initial load.
     */
    private fun init(manga: Manga, initialChapterId: Long) {
        if (!needsInit()) return

        this.manga = manga
        if (chapterId == -1L) chapterId = initialChapterId

        checkTrackers(manga)

        NotificationReceiver.dismissNotification(
            preferences.context,
            manga.id!!.hashCode(),
            Notifications.ID_NEW_CHAPTERS
        )

        loader = ChapterLoader(downloadManager, manga, sourceManager)

        Observable.just(manga).subscribeLatestCache(ReaderActivity::setManga)
        viewerChaptersRelay.subscribeLatestCache(ReaderActivity::setChapters)
        isLoadingAdjacentChapterRelay.subscribeLatestCache(ReaderActivity::setProgressDialog)

        // Read chapterList from an io thread because it's retrieved lazily and would block main.
        activeChapterSubscription?.unsubscribe()
        activeChapterSubscription = Observable
            .fromCallable { chapterList.first { chapterId == it.chapter.id } }
            .flatMap { getLoadObservable(loader!!, it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { _, _ ->
                    // Ignore onNext event
                },
                ReaderActivity::setInitialChapterError
            )
    }

    /**
     * Returns an observable that loads the given [chapter] with this [loader]. This observable
     * handles main thread synchronization and updating the currently active chapters on
     * [viewerChaptersRelay], however callers must ensure there won't be more than one
     * subscription active by unsubscribing any existing [activeChapterSubscription] before.
     * Callers must also handle the onError event.
     */
    private fun getLoadObservable(
        loader: ChapterLoader,
        chapter: ReaderChapter,
    ): Observable<ViewerChapters> {
        return loader.loadChapter(chapter)
            .andThen(
                Observable.fromCallable {
                    val chapterPos = chapterList.indexOf(chapter)

                    ViewerChapters(
                        chapter,
                        chapterList.getOrNull(chapterPos - 1),
                        chapterList.getOrNull(chapterPos + 1)
                    )
                }
            )
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { newChapters ->
                val oldChapters = viewerChaptersRelay.value

                // Add new references first to avoid unnecessary recycling
                newChapters.ref()
                oldChapters?.unref()

                viewerChaptersRelay.call(newChapters)
            }
    }

    suspend fun loadChapterURL(urlChapterId: String) {
        val dbChapter = db.getChapter(MdUtil.chapterSuffix + urlChapterId).executeAsBlocking()
        if (dbChapter?.manga_id != null) {
            val dbManga = db.getManga(dbChapter.manga_id!!).executeAsBlocking()
            if (dbManga != null) {
                withContext(Dispatchers.Main) {
                    init(dbManga, dbChapter.id!!)
                }
                return
            }
        }
        val mangaDex = sourceManager.getMangadex()
        val mangaId = mangaDex.getMangaIdFromChapterId(urlChapterId)
        val url = "/title/$mangaId"
        val dbManga = db.getMangadexManga(url).executeAsBlocking()
        val tempManga = dbManga ?: (
            MangaImpl().apply {
                this.url = url
                title = ""
            }
            )
        val (networkManga, chapters) = mangaDex.fetchMangaAndChapterDetails(tempManga)

        tempManga.copyFrom(networkManga)
        tempManga.title = networkManga.title

        db.insertManga(tempManga).executeAsBlocking()
        val manga = db.getMangadexManga(tempManga.url).executeAsBlocking()!!

        XLog.d("tempManga id ${tempManga.id}")
        XLog.d("Manga id ${manga.id}")

        if (chapters.isNotEmpty()) {
            val (newChapters, _) = syncChaptersWithSource(db, chapters, manga)
            val currentChapter = newChapters.find { it.url == MdUtil.chapterSuffix + urlChapterId }
            if (currentChapter?.id != null) {
                withContext(Dispatchers.Main) {
                    init(manga, currentChapter.id!!)
                }
            } else {
                throw Exception("Chapter not found")
            }
        }
    }

    /**
     * Called when the user changed to the given [chapter] when changing pages from the viewer.
     * It's used only to set this chapter as active.
     */
    private fun loadNewChapter(chapter: ReaderChapter) {
        val loader = loader ?: return

        XLog.d("loadNewChapter Loading %s - %s", chapter.chapter.url, chapter.chapter.name)

        activeChapterSubscription?.unsubscribe()
        activeChapterSubscription = getLoadObservable(loader, chapter)
            .toCompletable()
            .onErrorComplete()
            .subscribe()
            .also(::add)
    }

    fun loadChapter(chapter: Chapter) {
        val loader = loader ?: return

        viewerChaptersRelay.value?.currChapter?.let(::onChapterChanged)

        XLog.d("Loading ${chapter.url}")

        activeChapterSubscription?.unsubscribe()
        val lastPage = if (chapter.pages_left <= 1) 0 else chapter.last_page_read
        activeChapterSubscription = getLoadObservable(loader, ReaderChapter(chapter))
            .doOnSubscribe { isLoadingAdjacentChapterRelay.call(true) }
            .doOnUnsubscribe { isLoadingAdjacentChapterRelay.call(false) }
            .subscribeFirst(
                { view, _ ->
                    scope.launchUI {
                        view.moveToPageIndex(lastPage, false)
                    }
                    view.refreshChapters()
                },
                { _, _ ->
                    // Ignore onError event, viewers handle that state
                }
            )
    }

    fun toggleBookmark(chapter: Chapter) {
        chapter.bookmark = !chapter.bookmark
        db.updateChapterProgress(chapter).executeAsBlocking()
    }

    /**
     * Called when the viewers decide it's a good time to preload a [chapter] and improve the UX so
     * that the user doesn't have to wait too long to continue reading.
     */
    private fun preload(chapter: ReaderChapter) {
        if (chapter.state != ReaderChapter.State.Wait && chapter.state !is ReaderChapter.State.Error) {
            return
        }

        XLog.d("Preloading %s - %s", chapter.chapter.url, chapter.chapter.name)

        val loader = loader ?: return

        loader.loadChapter(chapter)
            .observeOn(AndroidSchedulers.mainThread())
            // Update current chapters whenever a chapter is preloaded
            .doOnCompleted { viewerChaptersRelay.value?.let(viewerChaptersRelay::call) }
            .onErrorComplete()
            .subscribe()
            .also(::add)
    }

    /**
     * Called from the activity to load and set the next chapter as active.
     */
    fun loadNextChapter(): Boolean {
        val nextChapter = viewerChaptersRelay.value?.nextChapter ?: return false
        loadChapter(nextChapter.chapter)
        return true
    }

    /**
     * Called from the activity to load and set the previous chapter as active.
     */
    fun loadPreviousChapter(): Boolean {
        val prevChapter = viewerChaptersRelay.value?.prevChapter ?: return false
        loadChapter(prevChapter.chapter)
        return true
    }

    /**
     * Called every time a page changes on the reader. Used to mark the flag of chapters being
     * read, update tracking services, enqueue downloaded chapter deletion, and updating the active chapter if this
     * [page]'s chapter is different from the currently active.
     */
    fun onPageSelected(page: ReaderPage, hasExtraPage: Boolean) {
        val currentChapters = viewerChaptersRelay.value ?: return

        val selectedChapter = page.chapter

        // Save last page read and mark as read if needed
        if (preferences.incognitoMode().get().not()) {
            selectedChapter.chapter.last_page_read = page.index
            selectedChapter.chapter.pages_left =
                (selectedChapter.pages?.size ?: page.index) - page.index
        }
        val shouldTrack =
            !preferences.incognitoMode().get() || hasTrackers || preferences.readingSync()
        if (shouldTrack &&
            // For double pages, check if the second to last page is doubled up
            (
                selectedChapter.pages?.lastIndex == page.index ||
                    (hasExtraPage && selectedChapter.pages?.lastIndex?.minus(1) == page.index)
                )
        ) {
            if (preferences.incognitoMode().get().not()) {
                selectedChapter.chapter.read = true
                updateTrackChapterRead(selectedChapter)
                updateReadingStatus(selectedChapter)
                deleteChapterIfNeeded(selectedChapter)
            }
        }

        if (selectedChapter != currentChapters.currChapter) {
            onChapterChanged(currentChapters.currChapter)
            loadNewChapter(selectedChapter)
        }
    }

    /**
     * Determines if deleting option is enabled and nth to last chapter actually exists.
     * If both conditions are satisfied enqueues chapter for delete
     * @param currentChapter current chapter, which is going to be marked as read.
     */
    private fun deleteChapterIfNeeded(currentChapter: ReaderChapter) {
        // Determine which chapter should be deleted and enqueue
        val currentChapterPosition = chapterList.indexOf(currentChapter)
        val removeAfterReadSlots = preferences.removeAfterReadSlots()
        val chapterToDelete = chapterList.getOrNull(currentChapterPosition - removeAfterReadSlots)
        // Check if deleting option is enabled and chapter exists
        if (removeAfterReadSlots != -1 && chapterToDelete != null && currentChapter.chapter.bookmark.not()) {
            enqueueDeleteReadChapters(chapterToDelete)
        }
    }

    /**
     * Called when a chapter changed from [fromChapter] to [toChapter]. It updates [fromChapter]
     * on the database.
     */
    private fun onChapterChanged(fromChapter: ReaderChapter) {
        saveChapterProgress(fromChapter)
        saveChapterHistory(fromChapter)
    }

    fun saveProgress() = getCurrentChapter()?.let { onChapterChanged(it) }

    /**
     * Saves this [chapter] progress (last read page and whether it's read).
     * If incognito mode isn't on or has at least 1 tracker
     */
    private fun saveChapterProgress(chapter: ReaderChapter) {
        db.getChapter(chapter.chapter.id!!).executeAsBlocking()?.let { dbChapter ->
            chapter.chapter.bookmark = dbChapter.bookmark
        }
        if (!preferences.incognitoMode().get() || hasTrackers) {
            db.updateChapterProgress(chapter.chapter).executeAsBlocking()
        }
    }

    /**
     * Saves this [chapter] last read history.
     */
    private fun saveChapterHistory(chapter: ReaderChapter) {
        if (!preferences.incognitoMode().get()) {
            val history = History.create(chapter.chapter).apply { last_read = Date().time }
            db.updateHistoryLastRead(history).executeAsBlocking()
        }
    }

    /**
     * Called from the activity to preload the given [chapter].
     */
    fun preloadChapter(chapter: ReaderChapter) {
        preload(chapter)
    }

    /**
     * Returns the currently active chapter.
     */
    fun getCurrentChapter(): ReaderChapter? {
        return viewerChaptersRelay.value?.currChapter
    }

    /**
     * Returns the viewer position used by this manga or the default one.
     */
    fun getMangaReadingMode(): Int {
        val default = preferences.defaultReadingMode()
        val manga = manga ?: return default
        val readerType = manga.defaultReaderType()
        if (manga.viewer_flags == -1 ||
            // Force webtoon mode
            (manga.isLongStrip() && readerType != manga.readingModeType)
        ) {
            val cantSwitchToLTR =
                (
                    readerType == ReadingModeType.LEFT_TO_RIGHT.flagValue &&
                        default != ReadingModeType.RIGHT_TO_LEFT.flagValue
                    )
            manga.readingModeType = if (cantSwitchToLTR) 0 else readerType
            db.updateViewerFlags(manga).asRxObservable().subscribe()
        }
        val viewer = if (manga.readingModeType == 0) default else manga.readingModeType

        return when {
            !manga.isLongStrip() && viewer == ReadingModeType.WEBTOON.flagValue -> ReadingModeType.CONTINUOUS_VERTICAL.flagValue
            else -> viewer
        }
    }

    /**
     * Updates the viewer position for the open manga.
     */
    fun setMangaReadingMode(readingModeType: Int) {
        val manga = manga ?: return
        manga.readingModeType = readingModeType
        db.updateViewerFlags(manga).executeAsBlocking()

        Observable.timer(250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .subscribeFirst({ view, _ ->
                val currChapters = viewerChaptersRelay.value
                if (currChapters != null) {
                    // Save current page
                    val currChapter = currChapters.currChapter
                    currChapter.requestedPage = currChapter.chapter.last_page_read

                    // Emit manga and chapters to the new viewer
                    view.setManga(manga)
                    view.setChapters(currChapters)
                }
            })
    }

    /**
     * Returns the orientation type used by this manga or the default one.
     */
    fun getMangaOrientationType(): Int {
        val default = preferences.defaultOrientationType().get()
        return when (manga?.orientationType) {
            OrientationType.DEFAULT.flagValue -> default
            else -> manga?.orientationType ?: default
        }
    }

    /**
     * Updates the orientation type for the open manga.
     */
    fun setMangaOrientationType(rotationType: Int) {
        val manga = manga ?: return
        manga.orientationType = rotationType
        db.updateViewerFlags(manga).executeAsBlocking()

        XLog.i("Manga orientation is ${manga.orientationType}")

        Observable.timer(250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
            .subscribeFirst({ view, _ ->
                val currChapters = viewerChaptersRelay.value
                if (currChapters != null) {
                    view.setOrientation(getMangaOrientationType())
                }
            })
    }

    /**
     * Saves the image of this [page] in the given [directory] and returns the file location.
     */
    private fun saveImage(
        page: ReaderPage,
        directory: File,
        manga: Manga,
        prefix: String = "",
    ): File {
        val stream = page.stream!!
        val type = ImageUtil.findImageType(stream) ?: throw Exception("Not an image")

        directory.mkdirs()

        val chapter = page.chapter.chapter

        // create chapter name so its always sorted correctly  max character is 75
        val pageName = parseChapterName(chapter.name, page.number.toString(), chapter.scanlator)
        // take only 150 characters so this file maxes at 225
        val trimmedTitle = (prefix + manga.title).take(150)

        // Build destination file
        val filename =
            DiskUtil.buildValidFilename("$trimmedTitle - $pageName") + ".${type.extension}"

        val destFile = File(directory, filename)
        stream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    private fun parseChapterName(
        chapterName: String,
        pageNumber: String,
        scanlator: String?,
    ): String {
        val builder = StringBuilder()
        var title = ""
        var vol = ""
        val list = chapterName.split(Regex(" "), 3)

        list.forEach {
            if (it.startsWith("vol.", true)) {
                vol = " Vol." + it.substringAfter(".").padStart(4, '0')
            } else if (it.startsWith("ch.", true)) {
                builder.append(" Ch.")
                builder.append(it.substringAfter(".").padStart(4, '0'))
            } else {
                title = " $it"
            }
        }

        if (vol.isNotBlank()) {
            builder.append(vol)
        }
        builder.append(" Pg.")
        builder.append(pageNumber.padStart(4, '0'))

        if (title.isNotEmpty()) {
            builder.append(title.take(200))
        }

        return builder.toString().trim()
    }

    /**
     * Saves the image of this [page] in the given [directory] and returns the file location.
     */
    private fun saveImages(
        page1: ReaderPage,
        page2: ReaderPage,
        isLTR: Boolean,
        @ColorInt bg: Int,
        directory: File,
        manga: Manga,
    ): File {
        val stream1 = page1.stream!!
        ImageUtil.findImageType(stream1) ?: throw Exception("Not an image")
        val stream2 = page2.stream!!
        ImageUtil.findImageType(stream2) ?: throw Exception("Not an image")
        val imageBytes = stream1().readBytes()
        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val imageBytes2 = stream2().readBytes()
        val imageBitmap2 = BitmapFactory.decodeByteArray(imageBytes2, 0, imageBytes2.size)

        val stream = ImageUtil.mergeBitmaps(imageBitmap, imageBitmap2, isLTR, bg)
        directory.mkdirs()

        val chapter = page1.chapter.chapter

        // Build destination file.
        val filename = DiskUtil.buildValidFilename(
            "${manga.title} - ${chapter.name}".take(225)
        ) + " - ${page1.number}-${page2.number}.jpg"

        val destFile = File(directory, filename)
        stream.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        stream.close()
        return destFile
    }

    /**
     * Saves the image of this [page] on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
    fun saveImage(page: ReaderPage) {
        if (page.status != Page.READY) return
        val manga = manga ?: return
        val context = Injekt.get<Application>()

        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        // Pictures directory.
        val baseDir = Environment.getExternalStorageDirectory().absolutePath +
            File.separator + Environment.DIRECTORY_PICTURES +
            File.separator + context.getString(R.string.app_name)
        val destDir = if (preferences.folderPerManga()) {
            File(baseDir + File.separator + manga.title)
        } else {
            File(baseDir)
        }

        // Copy file in background.
        Observable.fromCallable { saveImage(page, destDir, manga) }
            .doOnNext { file ->
                DiskUtil.scanMedia(context, file)
                notifier.onComplete(file)
            }
            .doOnError { notifier.onError(it.message) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, file -> view.onSaveImageResult(SaveImageResult.Success(file)) },
                { view, error -> view.onSaveImageResult(SaveImageResult.Error(error)) }
            )
    }

    fun saveImages(
        firstPage: ReaderPage,
        secondPage: ReaderPage,
        isLTR: Boolean,
        @ColorInt bg: Int,
    ) {
        scope.launch {
            if (firstPage.status != Page.READY) return@launch
            if (secondPage.status != Page.READY) return@launch
            val manga = manga ?: return@launch
            val context = Injekt.get<Application>()

            val notifier = SaveImageNotifier(context)
            notifier.onClear()

            // Pictures directory.
            val destDir = File(
                Environment.getExternalStorageDirectory().absolutePath +
                    File.separator + Environment.DIRECTORY_PICTURES +
                    File.separator + context.getString(R.string.app_name)
            )

            try {
                val file = saveImages(firstPage, secondPage, isLTR, bg, destDir, manga)
                DiskUtil.scanMedia(context, file)
                notifier.onComplete(file)
                withUIContext { view?.onSaveImageResult(SaveImageResult.Success(file)) }
            } catch (e: Exception) {
                withUIContext { view?.onSaveImageResult(SaveImageResult.Error(e)) }
            }
        }
    }

    /**
     * Shares the image of this [page] and notifies the UI with the path of the file to share.
     * The image must be first copied to the internal partition because there are many possible
     * formats it can come from, like a zipped chapter, in which case it's not possible to directly
     * get a path to the file and it has to be decompresssed somewhere first. Only the last shared
     * image will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(page: ReaderPage) {
        if (page.status != Page.READY) return
        val manga = manga ?: return
        val context = Injekt.get<Application>()

        val destDir = File(context.cacheDir, "shared_image")

        Observable.fromCallable { destDir.deleteRecursively() } // Keep only the last shared file
            .map { saveImage(page, destDir, manga, "SPOILER_") }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, file -> view.onShareImageResult(file, page) },
                { _, _ -> /* Empty */ }
            )
    }

    fun shareImages(
        firstPage: ReaderPage,
        secondPage: ReaderPage,
        isLTR: Boolean,
        @ColorInt bg: Int,
    ) {
        scope.launch {
            if (firstPage.status != Page.READY) return@launch
            if (secondPage.status != Page.READY) return@launch
            val manga = manga ?: return@launch
            val context = Injekt.get<Application>()

            val destDir = File(context.cacheDir, "shared_image")
            destDir.deleteRecursively()
            try {
                val file = saveImages(firstPage, secondPage, isLTR, bg, destDir, manga)
                withUIContext {
                    view?.onShareImageResult(file, firstPage, secondPage)
                }
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Sets the image of this [page] as cover and notifies the UI of the result.
     */
    fun setAsCover(page: ReaderPage) {
        if (page.status != Page.READY) return
        val manga = manga ?: return
        val stream = page.stream ?: return

        Observable
            .fromCallable {
                if (manga.favorite) {
                    coverCache.setCustomCoverToCache(manga, stream())
                    SetAsCoverResult.Success
                } else {
                    SetAsCoverResult.AddToLibraryFirst
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeFirst(
                { view, result -> view.onSetAsCoverResult(result) },
                { view, _ -> view.onSetAsCoverResult(SetAsCoverResult.Error) }
            )
    }

    /**
     * Results of the set as cover feature.
     */
    enum class SetAsCoverResult {
        Success, AddToLibraryFirst, Error
    }

    /**
     * Results of the save image feature.
     */
    sealed class SaveImageResult {
        class Success(val file: File) : SaveImageResult()
        class Error(val error: Throwable) : SaveImageResult()
    }

    private fun updateReadingStatus(readerChapter: ReaderChapter) {
        if (preferences.readingSync().not() && readerChapter.chapter.isMergedChapter().not()) return
        scope.launchIO {
            statusHandler.markChapterRead(readerChapter.chapter.mangadex_chapter_id)
        }
    }

    /**
     * Starts the service that updates the last chapter read in sync services. This operation
     * will run in a background thread and errors are ignored.
     */
    private fun updateTrackChapterRead(readerChapter: ReaderChapter) {
        if (!preferences.autoUpdateTrack()) return
        val manga = manga ?: return

        val chapterRead = readerChapter.chapter.chapter_number.toInt()

        val trackManager = Injekt.get<TrackManager>()

        // We wan't these to execute even if the presenter is destroyed so launch on GlobalScope
        launchIO {
            val trackList = db.getTracks(manga).executeAsBlocking()
            trackList.map { track ->
                val service = trackManager.getService(track.sync_id)
                if (service != null && service.isLogged && chapterRead > track.last_chapter_read) {
                    if (!preferences.context.isOnline()) {
                        XLog.d("offline adding tracker info to update later")
                        val mangaId = manga.id ?: return@map
                        val trackings = preferences.trackingsToAddOnline().get().toMutableSet()
                        val currentTracking =
                            trackings.find { it.startsWith("$mangaId:${track.sync_id}:") }
                        trackings.remove(currentTracking)
                        trackings.add("$mangaId:${track.sync_id}:$chapterRead")
                        preferences.trackingsToAddOnline().set(trackings)
                        DelayedTrackingUpdateJob.setupTask(preferences.context)
                    } else {
                        try {
                            track.last_chapter_read = chapterRead
                            service.update(track, true)
                            db.insertTrack(track).executeAsBlocking()
                        } catch (e: Exception) {
                            XLog.e(e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Enqueues this [chapter] to be deleted when [deletePendingChapters] is called. The download
     * manager handles persisting it across process deaths.
     */
    private fun enqueueDeleteReadChapters(chapter: ReaderChapter) {
        if (!chapter.chapter.read) return
        val manga = manga ?: return

        Completable
            .fromCallable {
                downloadManager.enqueueDeleteChapters(listOf(chapter.chapter), manga)
            }
            .onErrorComplete()
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    /**
     * Deletes all the pending chapters. This operation will run in a background thread and errors
     * are ignored.
     */
    private fun deletePendingChapters() {
        Completable.fromCallable { downloadManager.deletePendingChapters() }
            .onErrorComplete()
            .subscribeOn(Schedulers.io())
            .subscribe()
    }
}
