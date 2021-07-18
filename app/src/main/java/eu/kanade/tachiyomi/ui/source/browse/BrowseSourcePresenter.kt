package eu.kanade.tachiyomi.ui.source.browse

import android.os.Bundle
import com.elvishew.xlog.XLog
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import eu.kanade.tachiyomi.ui.source.filter.CheckboxItem
import eu.kanade.tachiyomi.ui.source.filter.CheckboxSectionItem
import eu.kanade.tachiyomi.ui.source.filter.GroupItem
import eu.kanade.tachiyomi.ui.source.filter.HeaderItem
import eu.kanade.tachiyomi.ui.source.filter.SelectItem
import eu.kanade.tachiyomi.ui.source.filter.SelectSectionItem
import eu.kanade.tachiyomi.ui.source.filter.SeparatorItem
import eu.kanade.tachiyomi.ui.source.filter.SortGroup
import eu.kanade.tachiyomi.ui.source.filter.SortItem
import eu.kanade.tachiyomi.ui.source.filter.TextItem
import eu.kanade.tachiyomi.ui.source.filter.TextSectionItem
import eu.kanade.tachiyomi.ui.source.filter.TriStateItem
import eu.kanade.tachiyomi.ui.source.filter.TriStateSectionItem
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.runAsObservable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [BrowseSourceController].
 */
open class BrowseSourcePresenter(
    searchQuery: String = "",
    private var isDeepLink: Boolean = false,
    val sourceManager: SourceManager = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    private val prefs: PreferencesHelper = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
) : BasePresenter<BrowseSourceController>() {

    /**
     * Selected source.
     */
    val source = sourceManager.getMangadex()

    /**
     * Query from the view.
     */
    var query = ""
        private set

    var filtersChanged = false

    var isFollows = false

    /**
     * Modifiable list of filters.
     */
    var sourceFilters = FilterList()
        set(value) {
            field = value
            filtersChanged = true
            filterItems = value.toItems()
        }

    var filterItems: List<IFlexible<*>> = emptyList()

    /**
     * List of filters used by the [Pager]. If empty alongside [query], the popular query is used.
     */
    var appliedFilters = FilterList()

    /**
     * Pager containing a list of manga results.
     */
    private lateinit var pager: Pager

    /**
     * Subject that initializes a list of manga.
     */
    private val mangaDetailSubject = PublishSubject.create<List<Manga>>()

    /**
     * Whether the view is in list mode or not.
     */
    var isListMode: Boolean = false

    /**
     * Whether the view shows manga that are already in the library
     */
    var isLibraryVisible: Boolean = true

    /**
     * Subscription for the pager.
     */
    private var pagerSubscription: Subscription? = null

    /**
     * Subscription for one request from the pager.
     */
    private var pageSubscription: Subscription? = null

    /**
     * Subscription to initialize manga details.
     */
    private var initializerSubscription: Subscription? = null

    private var scope = CoroutineScope(Job() + Dispatchers.IO)

    init {
        query = searchQuery
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)

        sourceFilters = source.getFilterList()

        if (savedState != null) {
            query = savedState.getString(::query.name, "")
        }

        prefs.browseAsList().asFlow()
            .onEach { setDisplayMode(it) }
            .launchIn(scope)

        prefs.browseShowLibrary().asFlow()
            .onEach { setShowLibrary(it) }
            .launchIn(scope)

        restartPager()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onSave(state: Bundle) {
        state.putString(::query.name, query)
        super.onSave(state)
    }

    /**
     * Restarts the pager for the active source with the provided query and filters.
     *
     * @param query the query.
     * @param filters the current state of the filters (for search mode).
     */
    fun restartPager(query: String = this.query, filters: FilterList = this.appliedFilters) {
        this.query = query
        if (this.query.startsWith("neko://")) {
            this.query = this.query.substringAfter("neko://")
            isDeepLink = true
        }
        this.appliedFilters = filters

        // subscribeToMangaInitializer()

        // Create a new pager.
        pager = createPager(query, filters)

        val sourceId = source.id

        val browseAsList = prefs.browseAsList()
        val sourceListType = prefs.libraryLayout()

        // Prepare the pager.
        pagerSubscription?.let { remove(it) }
        pagerSubscription = pager.results()
            .observeOn(Schedulers.io())
            .map { it.first to it.second.map { sManga -> networkToLocalManga(sManga, sourceId) } }
            .doOnNext { initializeMangaList(it.second) }
            .map {
                it.first to it.second.map { manga ->
                    BrowseSourceItem(
                        manga,
                        browseAsList,
                        sourceListType,
                        isFollows
                    )
                }
                    .filter { manga -> isDeepLink || isLibraryVisible || !manga.manga.favorite }
            }.observeOn(AndroidSchedulers.mainThread())
            .subscribeReplay(
                { view, (page, mangaList) ->
                    if (isDeepLink) {
                        view.goDirectlyForDeepLink(mangaList.first().manga)
                    } else {
                        view.onAddPage(page, mangaList)
                    }
                },
                { _, error ->
                    XLog.e(error)
                }
            )

        // Request first page.
        requestNext()
    }

    /**
     * Requests the next page for the active pager.
     */
    fun requestNext() {
        if (!hasNextPage()) return

        pageSubscription?.let { remove(it) }
        pageSubscription = Observable.defer { pager.requestNext() }
            .subscribeFirst(
                { _, _ ->
                    // Nothing to do when onNext is emitted.
                },
                BrowseSourceController::onAddPageError
            )
    }

    /**
     * Returns true if the last fetched page has a next page.
     */
    fun hasNextPage(): Boolean {
        return pager.hasNextPage
    }

    /**
     * Sets the display mode.
     *
     * @param asList whether the current mode is in list or not.
     */
    private fun setDisplayMode(asList: Boolean) {
        isListMode = asList
        subscribeToMangaInitializer()
    }

    /**
     * Sets the library shown mode.
     *
     * @param showLibrary whether mangaList in library should be shown
     */
    private fun setShowLibrary(showLibrary: Boolean) {
        isLibraryVisible = showLibrary
        restartPager()
    }

    /**
     * Subscribes to the initializer of manga details and updates the view if needed.
     */
    private fun subscribeToMangaInitializer() {
        initializerSubscription?.let { remove(it) }
        initializerSubscription = mangaDetailSubject.observeOn(Schedulers.io())
            .flatMap { Observable.from(it) }
            .filter { it.thumbnail_url == null && !it.initialized }
            .filter { isDeepLink || isLibraryVisible || !it.favorite }
            .concatMap {
                getMangaDetailsObservable(it)
            }
            .onBackpressureBuffer()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { manga ->
                    @Suppress("DEPRECATION")
                    view?.onMangaInitialized(manga)
                },
                { error ->
                    XLog.e(error)
                }
            )
            .apply { add(this) }
    }

    /**
     * Returns a manga from the database for the given manga from network. It creates a new entry
     * if the manga is not yet in the database.
     *
     * @param sManga the manga from the source.
     * @return a manga from the database.
     */
    private fun networkToLocalManga(sManga: SManga, sourceId: Long): Manga {
        var localManga = db.getManga(sManga.url, sourceId).executeAsBlocking()
        if (localManga == null) {
            val newManga = Manga.create(sManga.url, sManga.title, sourceId)
            newManga.copyFrom(sManga)
            val result = db.insertManga(newManga).executeAsBlocking()
            newManga.id = result.insertedId()
            localManga = newManga
        } else if (localManga.title.isBlank()) {
            localManga.title = sManga.title
            db.insertManga(localManga).executeAsBlocking()
        }

        return localManga
    }

    /**
     * Initialize a list of manga.
     *
     * @param mangaList the list of manga to initialize.
     */
    fun initializeMangaList(mangaList: List<Manga>) {
        mangaDetailSubject.onNext(mangaList)
    }

    /**
     * Returns an observable of manga that initializes the given manga.
     *
     * @param manga the manga to initialize.
     * @return an observable of the manga to initialize
     */
    private fun getMangaDetailsObservable(manga: Manga): Observable<Manga> {
        return runAsObservable(scope) {
            val networkManga = source.fetchMangaDetails(manga)
            manga.copyFrom(networkManga)
            manga.initialized = true
            db.insertManga(manga).executeAsBlocking()
            manga
        }
    }

    fun confirmDeletion(manga: Manga) {
        launchIO {
            coverCache.deleteFromCache(manga)
            val downloadManager: DownloadManager = Injekt.get()
            downloadManager.deleteManga(manga, source)
        }
    }

    /**
     * Changes the active display mode.
     */
    fun swapDisplayMode() {
        prefs.browseAsList().set(!isListMode)
    }

    /**
     * Changes the active library show mode mode.
     */
    fun swapLibraryVisibility() {
        prefs.browseShowLibrary().set(!isLibraryVisible)
    }

    /**
     * Search for manga based off of a random manga id by utilizing the [query] and the [restartPager].
     */
    fun searchRandomManga(): Flow<Manga?> {
        return source.getRandomManga().map { smanga ->
            if (smanga == null) {
                null
            } else {
                networkToLocalManga(smanga, source.id)
            }
        }
    }

    /**
     * Set the filter states for the current source.
     *
     * @param filters a list of active filters.
     */
    fun setSourceFilter(filters: FilterList) {
        restartPager(filters = filters)
    }

    open fun createPager(query: String, filters: FilterList): Pager {
        return BrowseSourcePager(scope, source, query, filters)
    }

    private fun FilterList.toItems(): List<IFlexible<*>> {
        return mapNotNull { filterItem ->
            when (filterItem) {
                is Filter.Header -> HeaderItem(filterItem)
                is Filter.Separator -> SeparatorItem(filterItem)
                is Filter.CheckBox -> CheckboxItem(filterItem)
                is Filter.TriState -> TriStateItem(filterItem)
                is Filter.Text -> TextItem(filterItem)
                is Filter.Select<*> -> SelectItem(filterItem)
                is Filter.Group<*> -> {
                    val group = GroupItem(filterItem)
                    val subItems = filterItem.state.mapNotNull { filterSubItem ->
                        when (filterSubItem) {
                            is Filter.CheckBox -> CheckboxSectionItem(filterSubItem)
                            is Filter.TriState -> TriStateSectionItem(filterSubItem)
                            is Filter.Text -> TextSectionItem(filterSubItem)
                            is Filter.Select<*> -> SelectSectionItem(filterSubItem)
                            else -> null
                        }
                    }
                    subItems.forEach { it.header = group }
                    group.subItems = subItems
                    group
                }
                is Filter.Sort -> {
                    val group = SortGroup(filterItem)
                    val subItems = filterItem.values.map { filterSubItem ->
                        SortItem(filterSubItem, group)
                    }
                    group.subItems = subItems
                    group
                }
            }
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    fun getCategories(): List<Category> {
        return db.getCategories().executeAsBlocking()
    }
}
