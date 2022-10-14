package eu.kanade.tachiyomi.ui.source.browse

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.filterVisibility
import eu.kanade.tachiyomi.util.resync
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.updateVisibility
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.NewFilter
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.network.message
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseComposePresenter(
    private val incomingQuery: String,
    private val browseRepository: BrowseRepository = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
) : BaseCoroutinePresenter<BrowseComposeController>() {

    private val _browseScreenState = MutableStateFlow(
        BrowseScreenState(
            isList = preferences.browseAsList().get(),
            showLibraryEntries = preferences.browseShowLibrary().get(),
            outlineCovers = preferences.outlineOnCovers().get(),
            isComfortableGrid = preferences.libraryLayout().get() == 2,
            rawColumnCount = preferences.gridSize().get(),
            promptForCategories = preferences.defaultCategory() == -1,
            filters = createInitialDexFilter(incomingQuery),
            screenType = BrowseScreenType.Homepage,
        ),
    )
    val browseScreenState: StateFlow<BrowseScreenState> = _browseScreenState.asStateFlow()

    private fun createInitialDexFilter(incomingQuery: String): DexFilters {
        val enabledContentRatings = preferences.contentRatingSelections()
        val contentRatings = MangaContentRating.getOrdered().map { NewFilter.ContentRating(it, enabledContentRatings.contains(it.key)) }

        return DexFilters(
            titleQuery = NewFilter.TitleQuery(incomingQuery),
            contentRatings = contentRatings,
        )
    }

    private val paginator = DefaultPaginator(
        initialKey = _browseScreenState.value.page,
        onLoadUpdated = {
            _browseScreenState.update { state ->
                state.copy(pageLoading = it)
            }
        },
        onRequest = {
            browseRepository.getSearchPage(browseScreenState.value.page, browseScreenState.value.filters)
        },
        getNextKey = {
            _browseScreenState.value.page + 1
        },
        onError = { resultError ->
            _browseScreenState.update {
                it.copy(
                    initialLoading = false,
                    pageLoading = false,
                    error = when (resultError) {
                        is ResultError.Generic -> resultError.errorString
                        else -> (resultError as ResultError.HttpError).message
                    },
                )
            }
        },
        onSuccess = { hasNextPage, items, nextKey ->
            _browseScreenState.update { state ->
                val allDisplayManga = state.displayMangaHolder.allDisplayManga + items
                state.copy(
                    screenType = BrowseScreenType.Filter,
                    displayMangaHolder = DisplayMangaHolder(
                        BrowseScreenType.Filter,
                        allDisplayManga.toImmutableList(),
                        allDisplayManga.filterVisibility(preferences).toImmutableList(),
                    ),
                    initialLoading = false,
                    pageLoading = false,
                    page = nextKey,
                    endReached = !hasNextPage,
                )
            }
        },
    )

    override fun onCreate() {
        super.onCreate()

        if (browseScreenState.value.filters.titleQuery.query.isNotBlank()) {
            getSearchPage()
        } else {
            getHomepage()
        }

        presenterScope.launch {
            _browseScreenState.update {
                it.copy(sideNavMode = SideNavMode.findByPrefValue(preferences.sideNavMode().get()), isLoggedIn = browseRepository.isLoggedIn())
            }
        }

        updateBrowseFilters()


        presenterScope.launch {
            if (browseScreenState.value.promptForCategories) {
                val categories = db.getCategories().executeAsBlocking()
                _browseScreenState.update {
                    it.copy(
                        categories = categories.map { category -> category.toCategoryItem() }.toPersistentList(),
                    )
                }
            }
        }
        presenterScope.launch {
            preferences.browseAsList().asFlow().collectLatest {
                _browseScreenState.update { state ->
                    state.copy(isList = it)
                }
            }
        }
        presenterScope.launch {
            preferences.browseShowLibrary().asFlow().collectLatest { bool ->
                _browseScreenState.update {
                    it.copy(showLibraryEntries = bool)
                }
                presenterScope.launch {
                    _browseScreenState.update {
                        it.copy(homePageManga = it.homePageManga.updateVisibility(preferences))
                    }
                }
                presenterScope.launch {
                    _browseScreenState.update {
                        it.copy(displayMangaHolder = it.displayMangaHolder.copy(filteredDisplayManga = it.displayMangaHolder.allDisplayManga.filterVisibility(preferences).toImmutableList()))
                    }
                }
            }
        }
    }

    fun loadNextItems() {
        presenterScope.launch {
            paginator.loadNextItems()
        }
    }

    private fun getHomepage() {
        presenterScope.launch {
            browseRepository.getHomePage().onFailure {
                _browseScreenState.update { state ->
                    state.copy(error = it.message(), initialLoading = false)
                }
            }.onSuccess {
                _browseScreenState.update { state ->
                    state.copy(homePageManga = it.updateVisibility(preferences), initialLoading = false)
                }
            }
        }
    }

    private fun getFollows(forceUpdate: Boolean) {
        presenterScope.launch {
            if (forceUpdate || _browseScreenState.value.displayMangaHolder.resultType != BrowseScreenType.Follows) {
                _browseScreenState.update { state ->
                    state.copy(initialLoading = true)
                }
                browseRepository.getFollows().onFailure {
                    _browseScreenState.update { state ->
                        state.copy(error = it.message(), initialLoading = false)
                    }
                }.onSuccess {
                    _browseScreenState.update { state ->
                        state.copy(displayMangaHolder = DisplayMangaHolder(BrowseScreenType.Follows, it.toImmutableList(), it.filterVisibility(preferences).toImmutableList()), initialLoading = false)
                    }
                }
            }
        }
    }

    fun getSearchPage() {
        presenterScope.launchIO {
            _browseScreenState.update { state ->
                state.copy(initialLoading = true, page = 1)
            }

            val currentQuery = browseScreenState.value.filters.titleQuery.query
            val deepLinkType = DeepLinkType.getDeepLinkType(currentQuery)
            val uuid = DeepLinkType.removePrefix(currentQuery, deepLinkType)

            when (deepLinkType) {
                DeepLinkType.None -> {
                    _browseScreenState.update {
                        it.copy(
                            pageLoading = false,
                            initialLoading = true,
                            screenType = BrowseScreenType.Filter,
                            displayMangaHolder = DisplayMangaHolder(resultType = BrowseScreenType.Filter, allDisplayManga = persistentListOf(), filteredDisplayManga = persistentListOf()),
                        )
                    }

                    val authorQuery = browseScreenState.value.filters.authorQuery.query.isNotBlank()
                    val groupQuery = browseScreenState.value.filters.groupQuery.query.isNotBlank()

                    if (authorQuery || groupQuery) {
                        when {
                            authorQuery -> browseRepository.getAuthors(browseScreenState.value.filters.authorQuery.query)
                            else -> browseRepository.getGroups(browseScreenState.value.filters.groupQuery.query)
                        }.onFailure {
                            _browseScreenState.update { state ->
                                state.copy(error = it.message(), initialLoading = false)
                            }
                        }.onSuccess { dr ->
                            _browseScreenState.update {
                                it.copy(otherResults = dr.toImmutableList(), screenType = BrowseScreenType.Other, initialLoading = false)
                            }
                        }
                    } else {
                        paginator.loadNextItems()
                    }
                }
                DeepLinkType.Manga -> {
                    browseRepository.getDeepLinkManga(uuid).onFailure {
                        _browseScreenState.update { state ->
                            state.copy(error = it.message(), initialLoading = false)
                        }
                    }.onSuccess { dm ->
                        if (incomingQuery.isNotBlank() && !_browseScreenState.value.handledIncomingQuery) {
                            _browseScreenState.update { it.copy(filters = it.filters.copy(titleQuery = NewFilter.TitleQuery("")), handledIncomingQuery = true) }
                        }
                        controller?.openManga(dm.mangaId)
                    }
                }
                else -> Unit
            }
        }
    }

    fun otherClick(uuid: String) {
        presenterScope.launch {
            if (browseScreenState.value.filters.authorQuery.query.isNotBlank()) {
                _browseScreenState.update {
                    it.copy(
                        filters = createInitialDexFilter("").copy(authorId = NewFilter.AuthorId(uuid)),
                    )
                }
                getSearchPage()
            } else if (browseScreenState.value.filters.groupQuery.query.isNotBlank()) {
                _browseScreenState.update {
                    it.copy(
                        filters = createInitialDexFilter("").copy(groupId = NewFilter.GroupId(uuid)),
                    )
                }
                getSearchPage()
            }

        }
    }

    fun toggleFavorite(mangaId: Long, categoryItems: List<CategoryItem>) {
        presenterScope.launch {
            val editManga = db.getManga(mangaId).executeAsBlocking()!!
            editManga.apply {
                favorite = !favorite
                date_added = when (favorite) {
                    true -> Date().time
                    false -> 0
                }
            }
            db.insertManga(editManga).executeAsBlocking()

            updateDisplayManga(mangaId, editManga.favorite)

            if (editManga.favorite) {
                val defaultCategory = preferences.defaultCategory()

                if (categoryItems.isEmpty() && defaultCategory != -1) {
                    _browseScreenState.value.categories.firstOrNull {
                        defaultCategory == it.id
                    }?.let {
                        val categories = listOf(MangaCategory.create(editManga, it.toDbCategory()))
                        db.setMangaCategories(categories, listOf(editManga))
                    }
                } else if (categoryItems.isNotEmpty()) {
                    val categories = categoryItems.map { MangaCategory.create(editManga, it.toDbCategory()) }
                    db.setMangaCategories(categories, listOf(editManga))
                }
            }
        }
    }

    private fun updateDisplayManga(mangaId: Long, favorite: Boolean) {
        presenterScope.launch {
            val tempList = _browseScreenState.value.homePageManga.map { homePageManga ->
                val index = homePageManga.displayManga.indexOfFirst { it.mangaId == mangaId }
                if (index == -1) {
                    homePageManga
                } else {
                    val tempMangaList = homePageManga.displayManga.toMutableList()
                    val tempDisplayManga = tempMangaList[index].copy(inLibrary = favorite)
                    tempMangaList[index] = tempDisplayManga
                    homePageManga.copy(displayManga = tempMangaList.toImmutableList())
                }
            }.toImmutableList()
            _browseScreenState.update {
                it.copy(homePageManga = tempList)
            }
        }
        presenterScope.launch {
            val index = _browseScreenState.value.displayMangaHolder.allDisplayManga.indexOfFirst { it.mangaId == mangaId }
            if (index >= 0) {
                val tempList = _browseScreenState.value.displayMangaHolder.allDisplayManga.toMutableList()
                val tempDisplayManga = tempList[index].copy(inLibrary = favorite)
                tempList[index] = tempDisplayManga
                _browseScreenState.update {
                    it.copy(
                        displayMangaHolder = it.displayMangaHolder.copy(allDisplayManga = tempList.toPersistentList()),
                    )
                }

                val filteredIndex = _browseScreenState.value.displayMangaHolder.filteredDisplayManga.indexOfFirst { it.mangaId == mangaId }
                if (filteredIndex >= 0) {
                    val tempFilterList = _browseScreenState.value.displayMangaHolder.filteredDisplayManga.toMutableList()
                    tempFilterList[filteredIndex] = tempDisplayManga
                    _browseScreenState.update {
                        it.copy(
                            displayMangaHolder = it.displayMangaHolder.copy(filteredDisplayManga = tempFilterList.toPersistentList()),
                        )
                    }
                }
            }
        }
    }

    fun saveFilter(name: String) {
        presenterScope.launch {
            val browseFilter = BrowseFilterImpl(
                name = name,
                dexFilters = Json.encodeToString(browseScreenState.value.filters),
            )
            db.insertBrowseFilter(browseFilter).executeAsBlocking()
            updateBrowseFilters()
        }
    }

    fun loadFilter(browseFilterImpl: BrowseFilterImpl) {
        presenterScope.launch {
            val dexFilters = Json.decodeFromString<DexFilters>(browseFilterImpl.dexFilters)
            _browseScreenState.update { it.copy(filters = dexFilters) }
        }
    }

    fun markFilterAsDefault(name: String) {
        presenterScope.launch {
            val updatedFilters = browseScreenState.value.savedFilters.map {
                if (it.name == name) {
                    it.copy(default = true)
                } else {
                    it.copy(default = false)
                }
            }
            db.insertBrowseFilters(updatedFilters).executeAsBlocking()
            updateBrowseFilters()
        }
    }

    fun deleteFilter(name: String) {
        presenterScope.launch {
            db.deleteBrowseFilter(name).executeAsBlocking()
            updateBrowseFilters()
        }
    }

    fun resetFilter() {
        presenterScope.launch {
            val resetFilters = createInitialDexFilter("")
            _browseScreenState.update { it.copy(filters = resetFilters) }
        }
    }

    fun filterChanged(newFilter: NewFilter) {
        presenterScope.launch {
            val updatedFilters = when (newFilter) {
                is NewFilter.ContentRating -> {
                    val list = lookupAndReplaceEntry(browseScreenState.value.filters.contentRatings, { it.rating == newFilter.rating }, newFilter)
                    browseScreenState.value.filters.copy(contentRatings = list)
                }
                is NewFilter.OriginalLanguage -> {
                    val list = lookupAndReplaceEntry(browseScreenState.value.filters.originalLanguage, { it.language == newFilter.language }, newFilter)
                    browseScreenState.value.filters.copy(originalLanguage = list)
                }
                is NewFilter.PublicationDemographic -> {
                    val list = lookupAndReplaceEntry(browseScreenState.value.filters.publicationDemographics, { it.demographic == newFilter.demographic }, newFilter)
                    browseScreenState.value.filters.copy(publicationDemographics = list)
                }
                is NewFilter.Status -> {
                    val list = lookupAndReplaceEntry(browseScreenState.value.filters.statuses, { it.status == newFilter.status }, newFilter)
                    browseScreenState.value.filters.copy(statuses = list)
                }
                is NewFilter.Tag -> {
                    val list = lookupAndReplaceEntry(browseScreenState.value.filters.tags, { it.tag == newFilter.tag }, newFilter)
                    browseScreenState.value.filters.copy(tags = list)
                }
                is NewFilter.Sort -> {
                    browseScreenState.value.filters.copy(sort = NewFilter.Sort.getSortList(newFilter.sort, newFilter.state))
                }
                is NewFilter.HasAvailableChapters -> {
                    browseScreenState.value.filters.copy(hasAvailableChapters = newFilter)
                }
                is NewFilter.TagInclusionMode -> {
                    browseScreenState.value.filters.copy(tagInclusionMode = newFilter)
                }
                is NewFilter.TagExclusionMode -> {
                    browseScreenState.value.filters.copy(tagExclusionMode = newFilter)
                }
                is NewFilter.TitleQuery -> {
                    if (newFilter.query.isNotBlank()) {
                        DexFilters.disableQueries(browseScreenState.value.filters).copy(titleQuery = newFilter)
                    } else {
                        DexFilters.enableAll(browseScreenState.value.filters).copy(titleQuery = newFilter)
                    }
                }

                is NewFilter.AuthorQuery -> {
                    if (newFilter.query.isNotBlank()) {
                        DexFilters.disableAll(browseScreenState.value.filters).copy(authorQuery = newFilter)
                    } else {
                        DexFilters.enableAll(browseScreenState.value.filters).copy(authorQuery = newFilter)
                    }
                }
                is NewFilter.AuthorId -> {
                    if (newFilter.uuid.isNotBlank()) {
                        DexFilters.disableQueries(browseScreenState.value.filters).copy(authorId = newFilter)
                    } else {
                        DexFilters.enableAll(browseScreenState.value.filters).copy(authorId = newFilter)
                    }
                }
                is NewFilter.GroupQuery -> {
                    if (newFilter.query.isNotBlank()) {
                        DexFilters.disableAll(browseScreenState.value.filters).copy(groupQuery = newFilter)
                    } else {
                        DexFilters.enableAll(browseScreenState.value.filters).copy(groupQuery = newFilter)
                    }
                }
                is NewFilter.GroupId -> {
                    if (newFilter.uuid.isNotBlank()) {
                        DexFilters.disableQueries(browseScreenState.value.filters).copy(groupId = newFilter, titleQuery = browseScreenState.value.filters.titleQuery)
                    } else {
                        DexFilters.enableQueries(browseScreenState.value.filters).copy(groupId = newFilter)
                    }
                }

            }

            _browseScreenState.update {
                it.copy(filters = updatedFilters)
            }
        }
    }

    private fun <T> lookupAndReplaceEntry(list: List<T>, indexMethod: (T) -> Boolean, newEntry: T): ImmutableList<T> {
        val index = list.indexOfFirst { indexMethod(it) }
        val mutableList = list.toMutableList()
        mutableList[index] = newEntry
        return mutableList.toImmutableList()
    }

    /**
     * Add New Category
     */
    fun addNewCategory(newCategory: String) {
        presenterScope.launchIO {
            val category = Category.create(newCategory)
            db.insertCategory(category).executeAsBlocking()
            _browseScreenState.update {
                it.copy(categories = db.getCategories().executeAsBlocking().map { category -> category.toCategoryItem() }.toPersistentList())
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!browseScreenState.value.isList)
    }

    fun changeScreenType(browseScreenType: BrowseScreenType, forceUpdate: Boolean = false) {
        presenterScope.launch {
            when (browseScreenType) {
                BrowseScreenType.Follows -> getFollows(forceUpdate)
                BrowseScreenType.Filter -> getSearchPage()
                else -> Unit
            }
            _browseScreenState.update {
                it.copy(screenType = browseScreenType, error = null)
            }
        }
    }

    fun retry() {
        changeScreenType(browseScreenState.value.screenType, true)
    }

    fun switchLibraryVisibility() {
        preferences.browseShowLibrary().set(!browseScreenState.value.showLibraryEntries)
    }

    fun updateBrowseFilters() {
        presenterScope.launch {
            _browseScreenState.update { it.copy(savedFilters = db.getBrowseFilters().executeAsBlocking().toImmutableList()) }
        }
    }

    fun updateMangaForChanges() {
        if (isScopeInitialized) {
            presenterScope.launch {
                val newHomePageManga = _browseScreenState.value.homePageManga.resync(db).updateVisibility(preferences)
                _browseScreenState.update {
                    it.copy(homePageManga = newHomePageManga)
                }
            }
            presenterScope.launch {
                val allDisplayManga = _browseScreenState.value.displayMangaHolder.allDisplayManga.resync(db)
                _browseScreenState.update {
                    it.copy(
                        displayMangaHolder = it.displayMangaHolder.copy(
                            allDisplayManga = allDisplayManga.toImmutableList(),
                            filteredDisplayManga = allDisplayManga.filterVisibility(preferences).toImmutableList(),
                        ),
                    )
                }
            }
        }
    }
}


