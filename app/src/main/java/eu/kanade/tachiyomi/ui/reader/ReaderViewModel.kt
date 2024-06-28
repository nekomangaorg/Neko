package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.graphics.BitmapFactory
import androidx.annotation.ColorInt
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.isLongStrip
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.getHttpSource
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.loader.DownloadPageLoader
import eu.kanade.tachiyomi.ui.reader.loader.HttpPageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.util.chapter.ChapterFilter
import eu.kanade.tachiyomi.util.chapter.ChapterItemFilter
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.chapter.updateTrackChapterRead
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.sharedCacheDir
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.system.withUIContext
import java.util.Date
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.chapter.ChapterItem as DomainChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.network.message
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.domain.storage.StorageManager
import org.nekomanga.logging.TimberKt
import tachiyomi.core.util.storage.DiskUtil
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/** Presenter used by the activity to perform background operations. */
class ReaderViewModel(
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val db: DatabaseHelper = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
    private val readerPreferences: ReaderPreferences = Injekt.get(),
    private val securityPreferences: SecurityPreferences = Injekt.get(),
    private val chapterFilter: ChapterFilter = Injekt.get(),
    private val chapterItemFilter: ChapterItemFilter = Injekt.get(),
    private val storageManager: StorageManager = Injekt.get(),
) : ViewModel() {

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    private val downloadProvider = DownloadProvider(preferences.context)

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    /** The manga loaded in the reader. It can be null when instantiated for a short time. */
    val manga: Manga?
        get() = state.value.manga

    val source: MangaDex
        get() = sourceManager.mangaDex

    /** The chapter id of the currently loaded chapter. Used to restore from process kill. */
    private var chapterId = savedState.get<Long>("chapter_id") ?: -1L
        set(value) {
            savedState["chapter_id"] = value
            field = value
        }

    /** The chapter loader for the loaded manga. It'll be null until [manga] is set. */
    private var loader: ChapterLoader? = null

    /** The time the chapter was started reading */
    private var chapterReadStartTime: Long? = null

    /** Relay used when loading prev/next chapter needed to lock the UI (with a dialog). */
    private var finished = false
    private var chapterToDownload: Download? = null

    /**
     * Chapter list for the active manga. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val chapterList by lazy {
        val manga = manga!!
        val dbChapters = db.getChapters(manga).executeAsBlocking()

        val selectedChapter =
            dbChapters.find { it.id == chapterId }
                ?: error("Requested chapter of id $chapterId not found in chapter list")

        val chaptersForReader =
            chapterFilter.filterChaptersForReader(dbChapters, manga, selectedChapter)
        val chapterSort = ChapterSort(manga, chapterFilter, preferences)
        chaptersForReader.sortedWith(chapterSort.sortComparator(true)).map(::ReaderChapter)
    }

    var chapterItems = emptyList<ReaderChapterItem>()

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    private val statusHandler: StatusHandler by injectLazy()

    private var hasTrackers: Boolean = false
    private val checkTrackers: (Manga) -> Unit = { manga ->
        val tracks = db.getTracks(manga).executeAsBlocking()

        hasTrackers = tracks.size > 0
    }

    init {
        var secondRun = false
        // To save state
        state
            .map { it.viewerChapters?.currChapter }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { currentChapter ->
                chapterId = currentChapter.chapter.id!!
                if (secondRun || !currentChapter.chapter.read) {
                    currentChapter.requestedPage = currentChapter.chapter.last_page_read
                }
                secondRun = true
            }
            .launchIn(viewModelScope)
    }

    /**
     * Called when the user pressed the back button and is going to leave the reader. Used to
     * trigger deletion of the downloaded chapters.
     */
    fun onBackPressed() {
        if (finished) return
        finished = true
        deletePendingChapters()
        val currentChapters = state.value.viewerChapters
        if (currentChapters != null) {
            currentChapters.unref()
            saveReadingProgress(currentChapters.currChapter)
            chapterToDownload?.let { downloadManager.addDownloadsToStartOfQueue(listOf(it)) }
        }
    }

    /**
     * Called when the activity is saved and not changing configurations. It updates the database to
     * persist the current progress of the active chapter.
     */
    fun onSaveInstanceState() {
        val currentChapter = getCurrentChapter() ?: return
        saveChapterProgress(currentChapter)
    }

    /** Whether this presenter is initialized yet. */
    fun needsInit(): Boolean {
        return manga == null
    }

    /**
     * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
     * fetch the manga from the database and initialize the initial chapter.
     */
    suspend fun init(mangaId: Long, initialChapterId: Long): Result<Boolean> {
        if (!needsInit()) return Result.success(true)
        return withIOContext {
            try {
                val manga = db.getManga(mangaId).executeAsBlocking()
                if (manga != null) {
                    mutableState.update { it.copy(manga = manga) }
                    if (chapterId == -1L) {
                        chapterId = initialChapterId
                    }

                    checkTrackers(manga)

                    NotificationReceiver.dismissNotification(
                        preferences.context,
                        manga.id!!.hashCode(),
                        Notifications.ID_NEW_CHAPTERS,
                    )

                    val context = Injekt.get<Application>()
                    loader =
                        ChapterLoader(
                            context,
                            downloadManager,
                            downloadProvider,
                            manga,
                            sourceManager
                        )

                    loadChapter(loader!!, chapterList.first { chapterId == it.chapter.id })
                    Result.success(true)
                } else {
                    // Unlikely but okay
                    Result.success(false)
                }
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                Result.failure(e)
            }
        }
    }

    suspend fun getChapters(): List<ReaderChapterItem> {
        val manga = manga ?: return emptyList()
        chapterItems =
            withContext(Dispatchers.IO) {
                val chapterSort = ChapterSort(manga, chapterFilter, preferences)
                val dbChapters = db.getChapters(manga).executeAsBlocking()
                chapterSort
                    .getChaptersSorted(
                        dbChapters,
                        filterForReader = true,
                        currentChapter = getCurrentChapter()?.chapter,
                    )
                    .map {
                        ReaderChapterItem(
                            it,
                            manga,
                            it.id == (getCurrentChapter()?.chapter?.id ?: chapterId),
                        )
                    }
            }

        return chapterItems
    }

    suspend fun loadChapterURL(urlChapterId: String) {
        val dbChapter = db.getChapter(MdConstants.chapterSuffix + urlChapterId).executeAsBlocking()
        if (dbChapter?.manga_id != null) {
            val dbManga = db.getManga(dbChapter.manga_id!!).executeAsBlocking()
            if (dbManga != null) {
                withContext(Dispatchers.Main) { init(dbManga.id!!, dbChapter.id!!) }
                return
            }
        }
        val mangaDex = sourceManager.mangaDex
        val mangaId = mangaDex.getMangaIdFromChapterId(urlChapterId)
        val url = "/title/$mangaId"
        val dbManga = db.getMangadexManga(url).executeAsBlocking()
        val tempManga =
            dbManga
                ?: (MangaImpl().apply {
                    this.source = mangaDex.id
                    this.url = url
                    title = ""
                })
        mangaDex.fetchMangaAndChapterDetails(tempManga, false).onSuccess { fetchedInfo ->
            val networkManga = fetchedInfo.sManga!!
            val chapters = fetchedInfo.sChapters

            tempManga.copyFrom(networkManga)
            tempManga.title = networkManga.title

            db.insertManga(tempManga).executeAsBlocking()
            val manga = db.getMangadexManga(tempManga.url).executeAsBlocking()!!

            TimberKt.d { "tempManga id ${tempManga.id}" }
            TimberKt.d { "Manga id ${manga.id}" }

            if (chapters.isNotEmpty()) {
                val (newChapters, _) = syncChaptersWithSource(db, chapters, manga)
                val currentChapter =
                    newChapters.find { it.url == MdConstants.chapterSuffix + urlChapterId }
                if (currentChapter?.id != null) {
                    withContext(Dispatchers.Main) { init(manga.id!!, currentChapter.id!!) }
                } else {
                    throw Exception("Chapter not found")
                }
            }
        }
    }

    /**
     * Called when the user changed to the given [chapter] when changing pages from the viewer. It's
     * used only to set this chapter as active.
     */
    private suspend fun loadNewChapter(chapter: ReaderChapter) {
        val loader = loader ?: return

        TimberKt.d { "loadNewChapter Loading ${chapter.chapter.url} - ${chapter.chapter.name}" }

        withIOContext {
            try {
                loadChapter(loader, chapter)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                TimberKt.e(e) { "Error loading new chapter ${chapter.chapter.url}" }
            }
        }
    }

    /**
     * Loads the given [chapter] with this [loader] and updates the currently active chapters.
     * Callers must handle errors.
     */
    private suspend fun loadChapter(
        loader: ChapterLoader,
        chapter: ReaderChapter,
    ): ViewerChapters {
        TimberKt.d { "Loading ${chapter.chapter.url}" }

        loader.loadChapter(chapter)

        val chapterPos = chapterList.indexOf(chapter)
        val newChapters =
            ViewerChapters(
                chapter,
                chapterList.getOrNull(chapterPos - 1),
                chapterList.getOrNull(chapterPos + 1),
            )

        withUIContext {
            mutableState.update {
                // Add new references first to avoid unnecessary recycling
                newChapters.ref()
                it.viewerChapters?.unref()

                chapterToDownload = deleteChapterFromDownloadQueue(newChapters.currChapter)
                it.copy(viewerChapters = newChapters)
            }
        }
        return newChapters
    }

    /** Called when the user is going to load the prev/next chapter through the menu button. */
    suspend fun loadChapter(chapter: ReaderChapter): Int? {
        val loader = loader ?: return -1

        TimberKt.d { "Loading adjacent ${chapter.chapter.url}" }
        var lastPage: Int? =
            if (chapter.chapter.pages_left <= 1) 0 else chapter.chapter.last_page_read
        mutableState.update { it.copy(isLoadingAdjacentChapter = true) }
        try {
            withIOContext { loadChapter(loader, chapter) }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            TimberKt.e(e) { "Error Loading adjacent chapter ${chapter.chapter.url}" }
            lastPage = null
        } finally {
            mutableState.update { it.copy(isLoadingAdjacentChapter = false) }
        }
        return lastPage
    }

    fun toggleBookmark(chapter: Chapter) {
        chapter.bookmark = !chapter.bookmark
        db.updateChapterProgress(chapter).executeAsBlocking()
    }

    /**
     * Called when the viewers decide it's a good time to preload a [chapter] and improve the UX so
     * that the user doesn't have to wait too long to continue reading.
     */
    private suspend fun preload(chapter: ReaderChapter) {
        if (chapter.pageLoader is HttpPageLoader) {
            val manga = manga ?: return
            val isDownloaded = downloadManager.isChapterDownloaded(chapter.chapter, manga)
            if (isDownloaded) {
                chapter.state = ReaderChapter.State.Wait
            }
        }

        if (
            chapter.state != ReaderChapter.State.Wait && chapter.state !is ReaderChapter.State.Error
        ) {
            return
        }

        TimberKt.d { "Preloading ${chapter.chapter.url} - ${chapter.chapter.name}" }

        val loader = loader ?: return
        withIOContext {
            try {
                loader.loadChapter(chapter)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                return@withIOContext
            }
            eventChannel.trySend(Event.ReloadViewerChapters)
        }
    }

    fun adjacentChapter(next: Boolean): ReaderChapter? {
        val chapters = state.value.viewerChapters
        return if (next) chapters?.nextChapter else chapters?.prevChapter
    }

    /**
     * Called every time a page changes on the reader. Used to mark the flag of chapters being read,
     * update tracking services, enqueue downloaded chapter deletion, and updating the active
     * chapter if this [page]'s chapter is different from the currently active.
     */
    fun onPageSelected(page: ReaderPage, hasExtraPage: Boolean) {
        val currentChapters = state.value.viewerChapters ?: return

        val selectedChapter = page.chapter

        // Save last page read and mark as read if needed
        if (!securityPreferences.incognitoMode().get()) {
            selectedChapter.chapter.last_page_read = page.index
            selectedChapter.chapter.pages_left =
                (selectedChapter.pages?.size ?: page.index) - page.index
        }
        val shouldTrack =
            !securityPreferences.incognitoMode().get() ||
                hasTrackers ||
                preferences.readingSync().get()
        if (
            shouldTrack &&
                // For double pages, check if the second to last page is doubled up
                ((selectedChapter.pages?.lastIndex == page.index && page.firstHalf != true) ||
                    (hasExtraPage && selectedChapter.pages?.lastIndex?.minus(1) == page.index))
        ) {
            if (!securityPreferences.incognitoMode().get()) {
                selectedChapter.chapter.read = true
                updateTrackChapterAfterReading(selectedChapter)
                updateReadingStatus(selectedChapter)
                deleteChapterIfNeeded(selectedChapter)
            }
        }

        if (selectedChapter != currentChapters.currChapter) {
            TimberKt.d { "Setting ${selectedChapter.chapter.url} as active" }
            saveReadingProgress(currentChapters.currChapter)
            setReadStartTime()
            scope.launch { loadNewChapter(selectedChapter) }
        }
        val pages = page.chapter.pages ?: return
        val inDownloadRange = page.number.toDouble() / pages.size > 0.2
        if (inDownloadRange) {
            downloadNextChapters()
        }
    }

    private fun downloadNextChapters() {
        val manga = manga ?: return
        if (getCurrentChapter()?.pageLoader !is DownloadPageLoader) return
        val nextChapter = state.value.viewerChapters?.nextChapter?.chapter ?: return
        val chaptersNumberToDownload = preferences.autoDownloadWhileReading().get()
        if (chaptersNumberToDownload == 0 || !manga.favorite) return
        scope.launchIO {
            val isNextChapterDownloaded = downloadManager.isChapterDownloaded(nextChapter, manga)
            if (isNextChapterDownloaded) {
                downloadAutoNextChapters(chaptersNumberToDownload, nextChapter.id)
            }
        }
    }

    private fun downloadAutoNextChapters(choice: Int, nextChapterId: Long?) {
        val chaptersToDownload = getNextUnreadChaptersSorted(nextChapterId).take(choice - 1)
        if (chaptersToDownload.isNotEmpty()) {
            downloadChapters(chaptersToDownload)
        }
    }

    private fun getNextUnreadChaptersSorted(nextChapterId: Long?): List<DomainChapterItem> {
        val chapterSort = ChapterItemSort(chapterItemFilter, preferences)
        return chapterList
            .asSequence()
            .map { DomainChapterItem(it.chapter.toSimpleChapter()!!) }
            .filter { !it.chapter.read || it.chapter.id == nextChapterId }
            .distinctBy { it.chapter.name }
            .sortedWith(chapterSort.sortComparator(manga!!, true))
            .toList()
            .takeLastWhile { it.chapter.id != nextChapterId }
    }

    /**
     * Downloads the given list of chapters with the manager.
     *
     * @param chapters the list of chapters to download.
     */
    private fun downloadChapters(chapters: List<DomainChapterItem>) {
        downloadManager.downloadChapters(
            manga!!,
            chapters.filter { !it.isDownloaded }.map { it.chapter.toDbChapter() }
        )
    }

    /**
     * Removes [currentChapter] from download queue if setting is enabled and [currentChapter] is
     * queued for download
     */
    private fun deleteChapterFromDownloadQueue(currentChapter: ReaderChapter): Download? {
        return downloadManager.getQueuedDownloadOrNull(currentChapter.chapter.id!!)?.apply {
            downloadManager.deletePendingDownloads(listOf(this))
        }
    }

    /**
     * Determines if deleting option is enabled and nth to last chapter actually exists. If both
     * conditions are satisfied enqueues chapter for delete
     *
     * @param currentChapter current chapter, which is going to be marked as read.
     */
    private fun deleteChapterIfNeeded(currentChapter: ReaderChapter) {
        // Determine which chapter should be deleted and enqueue
        val currentChapterPosition = chapterList.indexOf(currentChapter)
        val removeAfterReadSlots = preferences.removeAfterReadSlots().get()
        val chapterToDelete = chapterList.getOrNull(currentChapterPosition - removeAfterReadSlots)

        if (removeAfterReadSlots != 0 && chapterToDownload != null) {
            downloadManager.addDownloadsToStartOfQueue(listOf(chapterToDownload!!))
        } else {
            chapterToDownload = null
        }
        // Check if deleting option is enabled and chapter exists
        if (
            removeAfterReadSlots != -1 &&
                chapterToDelete != null &&
                !currentChapter.chapter.bookmark
        ) {
            enqueueDeleteReadChapters(chapterToDelete)
        }
    }

    /** Called when reader chapter is changed in reader or when activity is paused. */
    private fun saveReadingProgress(readerChapter: ReaderChapter) {
        db.inTransaction {
            saveChapterProgress(readerChapter)
            saveChapterHistory(readerChapter)
        }
    }

    fun saveCurrentChapterReadingProgress() = getCurrentChapter()?.let { saveReadingProgress(it) }

    /**
     * Saves this [readerChapter]'s progress (last read page and whether it's read). If incognito
     * mode isn't on or has at least 1 tracker
     */
    private fun saveChapterProgress(readerChapter: ReaderChapter) {
        readerChapter.requestedPage = readerChapter.chapter.last_page_read
        db.getChapter(readerChapter.chapter.id!!).executeAsBlocking()?.let { dbChapter ->
            readerChapter.chapter.bookmark = dbChapter.bookmark
        }
        if (!securityPreferences.incognitoMode().get() || hasTrackers) {
            db.updateChapterProgress(readerChapter.chapter).executeAsBlocking()
        }
    }

    /** Saves this [readerChapter] last read history. */
    private fun saveChapterHistory(readerChapter: ReaderChapter) {
        if (!securityPreferences.incognitoMode().get()) {
            val readAt = Date().time
            val sessionReadDuration = chapterReadStartTime?.let { readAt - it } ?: 0
            val oldTimeRead =
                db.getHistoryByChapterUrl(readerChapter.chapter.url).executeAsBlocking()?.time_read
                    ?: 0
            val history =
                History.create(readerChapter.chapter).apply {
                    last_read = readAt
                    time_read = sessionReadDuration + oldTimeRead
                }
            db.upsertHistoryLastRead(history).executeAsBlocking()
            chapterReadStartTime = null
        }
    }

    fun setReadStartTime() {
        chapterReadStartTime = Date().time
    }

    /** Called from the activity to preload the given [chapter]. */
    suspend fun preloadChapter(chapter: ReaderChapter) {
        preload(chapter)
    }

    /** Returns the currently active chapter. */
    fun getCurrentChapter(): ReaderChapter? {
        return state.value.viewerChapters?.currChapter
    }

    fun getChapterUrl(): String? {
        val chapter = getCurrentChapter()?.chapter ?: return null
        val source = chapter.getHttpSource(sourceManager)
        return source.getChapterUrl(chapter.toSimpleChapter()!!)
    }

    /** Returns the viewer position used by this manga or the default one. */
    fun getMangaReadingMode(): Int {
        val default = readerPreferences.defaultReadingMode().get()
        val manga = manga ?: return default
        val readerType = manga.defaultReaderType()
        if (manga.viewer_flags == -1) {
            val cantSwitchToLTR =
                (readerType == ReadingModeType.LEFT_TO_RIGHT.flagValue &&
                    default != ReadingModeType.RIGHT_TO_LEFT.flagValue)
            if (manga.viewer_flags == -1) {
                manga.viewer_flags = 0
            }
            manga.readingModeType = if (cantSwitchToLTR) 0 else readerType
            db.updateViewerFlags(manga).asRxObservable().subscribe()
        }
        val viewer = if (manga.readingModeType == 0) default else manga.readingModeType

        return when {
            !manga.isLongStrip() && viewer == ReadingModeType.WEBTOON.flagValue ->
                ReadingModeType.CONTINUOUS_VERTICAL.flagValue
            else -> viewer
        }
    }

    /** Updates the viewer position for the open manga. */
    fun setMangaReadingMode(readingModeType: Int) {
        val manga = manga ?: return

        runBlocking(Dispatchers.IO) {
            manga.readingModeType = readingModeType
            db.updateViewerFlags(manga).executeAsBlocking()
            val currChapters = state.value.viewerChapters
            if (currChapters != null) {
                // Save current page
                val currChapter = currChapters.currChapter
                currChapter.requestedPage = currChapter.chapter.last_page_read

                mutableState.update {
                    it.copy(
                        manga = db.getManga(manga.id!!).executeAsBlocking(),
                        viewerChapters = currChapters,
                    )
                }
                eventChannel.send(Event.ReloadMangaAndChapters)
            }
        }
    }

    /** Returns the orientation type used by this manga or the default one. */
    fun getMangaOrientationType(): Int {
        val default = readerPreferences.defaultOrientationType().get()
        return when (manga?.orientationType) {
            OrientationType.DEFAULT.flagValue -> default
            else -> manga?.orientationType ?: default
        }
    }

    /** Updates the orientation type for the open manga. */
    fun setMangaOrientationType(rotationType: Int) {
        val manga = manga ?: return
        this.manga?.orientationType = rotationType
        db.updateViewerFlags(manga).executeAsBlocking()

        TimberKt.i { "Manga orientation is ${manga.orientationType}" }

        viewModelScope.launchIO {
            db.updateViewerFlags(manga).executeAsBlocking()
            val currChapters = state.value.viewerChapters
            if (currChapters != null) {
                mutableState.update {
                    it.copy(
                        manga = db.getManga(manga.id!!).executeAsBlocking(),
                        viewerChapters = currChapters,
                    )
                }
                eventChannel.send(Event.SetOrientation(getMangaOrientationType()))
                eventChannel.send(Event.ReloadViewerChapters)
            }
        }
    }

    /** Saves the image of this [page] in the given [directory] and returns the file location. */
    private fun saveImage(
        page: ReaderPage,
        directory: UniFile,
        manga: Manga,
        prefix: String = "",
    ): UniFile {
        val stream = page.stream!!
        val type = ImageUtil.findImageType(stream) ?: throw Exception("Not an image")

        val chapter = page.chapter.chapter

        // create chapter name so its always sorted correctly  max character is 75
        val pageName = parseChapterName(chapter.name, page.number.toString())
        // take only 150 characters so this file maxes at 225
        val trimmedTitle = (prefix + manga.title).take(150)

        // Build destination file
        val filename =
            DiskUtil.buildValidFilename("$trimmedTitle - $pageName") + ".${type.extension}"

        val destFile = directory.createFile(filename)!!

        stream().use { input -> destFile.openOutputStream().use { output -> input.copyTo(output) } }
        return destFile
    }

    private fun parseChapterName(
        chapterName: String,
        pageNumber: String,
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
     * Saves the image of [page1] and [page2] in the given [directory] and returns the file
     * location.
     */
    private fun saveImages(
        page1: ReaderPage,
        page2: ReaderPage,
        isLTR: Boolean,
        @ColorInt bg: Int,
        directory: UniFile,
        manga: Manga,
    ): UniFile {
        val stream1 = page1.stream!!
        ImageUtil.findImageType(stream1) ?: throw Exception("Not an image")
        val stream2 = page2.stream!!
        ImageUtil.findImageType(stream2) ?: throw Exception("Not an image")
        val imageBytes = stream1().readBytes()
        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val imageBytes2 = stream2().readBytes()
        val imageBitmap2 = BitmapFactory.decodeByteArray(imageBytes2, 0, imageBytes2.size)

        val stream = ImageUtil.mergeBitmaps(imageBitmap, imageBitmap2, isLTR, bg)

        val chapter = page1.chapter.chapter

        // Build destination file.
        val filename =
            DiskUtil.buildValidFilename(
                "${manga.title} - ${chapter.name}".take(225),
            ) + " - ${page1.number}-${page2.number}.jpg"

        val destFile = directory.createFile(filename)!!
        stream.use { input -> destFile.openOutputStream().use { output -> input.copyTo(output) } }
        stream.close()
        return destFile
    }

    /**
     * Saves the image of this [page] on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
    fun saveImage(page: ReaderPage) {
        if (page.status != Page.State.READY) return
        val manga = manga ?: return
        val context = Injekt.get<Application>()

        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        // Copy file in background.
        viewModelScope.launchNonCancellable {
            try {
                var directory = storageManager.getPagesDirectory()

                if (preferences.folderPerManga().get() && directory != null) {
                    directory =
                        directory.createDirectory(DiskUtil.buildValidFilename(manga.title))!!
                }
                directory ?: throw Exception("Error creating directory to save page")

                val file = saveImage(page, directory, manga)
                DiskUtil.scanMedia(context, file.uri)
                notifier.onComplete(file)
                eventChannel.send(Event.SavedImage(SaveImageResult.Success(file)))
            } catch (e: Exception) {
                notifier.onError(e.message)
                eventChannel.send(Event.SavedImage(SaveImageResult.Error(e)))
            }
        }
    }

    fun saveImages(
        firstPage: ReaderPage,
        secondPage: ReaderPage,
        isLTR: Boolean,
        @ColorInt bg: Int
    ) {
        viewModelScope.launchNonCancellable {
            if (firstPage.status != Page.State.READY) return@launchNonCancellable
            if (secondPage.status != Page.State.READY) return@launchNonCancellable
            val manga = manga ?: return@launchNonCancellable
            val context = Injekt.get<Application>()

            val notifier = SaveImageNotifier(context)
            notifier.onClear()

            try {
                var directory = storageManager.getPagesDirectory()!!

                if (preferences.folderPerManga().get()) {
                    directory =
                        directory.createDirectory(DiskUtil.buildValidFilename(manga.title))!!
                }
                val file = saveImages(firstPage, secondPage, isLTR, bg, directory, manga)
                DiskUtil.scanMedia(context, file.uri)
                notifier.onComplete(file)
                eventChannel.send(Event.SavedImage(SaveImageResult.Success(file)))
            } catch (e: Exception) {
                eventChannel.send(Event.SavedImage(SaveImageResult.Error(e)))
            }
        }
    }

    /**
     * Shares the image of this [page] and notifies the UI with the path of the file to share. The
     * image must be first copied to the internal partition because there are many possible formats
     * it can come from, like a zipped chapter, in which case it's not possible to directly get a
     * path to the file and it has to be decompresssed somewhere first. Only the last shared image
     * will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(page: ReaderPage) {
        if (page.status != Page.State.READY) return
        val manga = manga ?: return
        val context = Injekt.get<Application>()

        val dir = context.sharedCacheDir() ?: throw Exception("Error accessing cache dir")

        viewModelScope.launchNonCancellable {
            val file = saveImage(page, dir, manga, "SPOILER_")
            eventChannel.send(Event.ShareImage(file, page))
        }
    }

    fun shareImages(
        firstPage: ReaderPage,
        secondPage: ReaderPage,
        isLTR: Boolean,
        @ColorInt bg: Int
    ) {
        scope.launch {
            if (firstPage.status != Page.State.READY) return@launch
            if (secondPage.status != Page.State.READY) return@launch
            val manga = manga ?: return@launch
            val context = Injekt.get<Application>()

            try {
                val destDir =
                    context.sharedCacheDir() ?: throw Exception("failed to open share image cache")
                val file = saveImages(firstPage, secondPage, isLTR, bg, destDir, manga)
                eventChannel.send(Event.ShareImage(file, firstPage, secondPage))
            } catch (e: Exception) {
                TimberKt.e(e) { "Error sharing image" }
            }
        }
    }

    /** Sets the image of this [page] as cover and notifies the UI of the result. */
    fun setAsCover(page: ReaderPage) {
        if (page.status != Page.State.READY) return
        val manga = manga ?: return
        val stream = page.stream ?: return

        viewModelScope.launchNonCancellable {
            val result =
                try {
                    if (manga.favorite) {
                        coverCache.setCustomCoverToCache(manga, stream())
                        SetAsCoverResult.Success
                    } else {
                        SetAsCoverResult.AddToLibraryFirst
                    }
                } catch (e: Exception) {
                    SetAsCoverResult.Error
                }
            eventChannel.send(Event.SetCoverResult(result))
        }
    }

    /** Results of the set as cover feature. */
    enum class SetAsCoverResult {
        Success,
        AddToLibraryFirst,
        Error
    }

    /** Results of the save image feature. */
    sealed class SaveImageResult {
        class Success(val file: UniFile) : SaveImageResult()

        class Error(val error: Throwable) : SaveImageResult()
    }

    private fun updateReadingStatus(readerChapter: ReaderChapter) {
        manga ?: return

        if (!preferences.readingSync().get() && !readerChapter.chapter.isMergedChapter()) return
        scope.launchIO {
            statusHandler.marksChaptersStatus(
                manga!!.uuid(),
                listOf(readerChapter.chapter.mangadex_chapter_id)
            )
        }
    }

    /**
     * Starts the service that updates the last chapter read in sync services. This operation will
     * run in a background thread and errors are ignored.
     */
    private fun updateTrackChapterAfterReading(readerChapter: ReaderChapter) {
        if (!preferences.autoUpdateTrack().get()) return
        viewModelScope.launchIO {
            val newChapterRead = readerChapter.chapter.chapter_number
            updateTrackChapterRead(
                manga?.id,
                newChapterRead,
                true,
                onError = { service, message ->
                    launchIO {
                        eventChannel.send(Event.ShareTrackingError(listOf(service to message)))
                    }
                }
            )
        }
    }

    /**
     * Enqueues this [chapter] to be deleted when [deletePendingChapters] is called. The download
     * manager handles persisting it across process deaths.
     */
    private fun enqueueDeleteReadChapters(chapter: ReaderChapter) {
        if (!chapter.chapter.read) return
        val manga = manga ?: return
        scope.launchNonCancellable {
            downloadManager.enqueueDeleteChapters(listOf(chapter.chapter), manga)
        }
    }

    /**
     * Deletes all the pending chapters. This operation will run in a background thread and errors
     * are ignored.
     */
    private fun deletePendingChapters() {
        scope.launchNonCancellable { downloadManager.deletePendingChapters() }
    }

    suspend fun lookupComment(chapterId: String): String? {
        val threadId =
            sourceManager.mangaDex
                .getChapterCommentId(chapterId)
                .onFailure { TimberKt.e { it.message() } }
                .getOrElse { null }

        return threadId
    }

    data class State(
        val manga: Manga? = null,
        val viewerChapters: ViewerChapters? = null,
        val isLoadingAdjacentChapter: Boolean = false,
        val lastPage: Int? = null,
    )

    sealed class Event {
        object ReloadViewerChapters : Event()

        object ReloadMangaAndChapters : Event()

        data class SetOrientation(val orientation: Int) : Event()

        data class SetCoverResult(val result: SetAsCoverResult) : Event()

        data class SavedImage(val result: SaveImageResult) : Event()

        data class ShareImage(
            val file: UniFile,
            val page: ReaderPage,
            val extraPage: ReaderPage? = null
        ) : Event()

        data class ShareTrackingError(val errors: List<Pair<TrackService, String?>>) : Event()
    }
}
