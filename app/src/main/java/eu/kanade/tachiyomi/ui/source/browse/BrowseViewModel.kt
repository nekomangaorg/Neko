package eu.kanade.tachiyomi.ui.source.browse

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.utils.MdSort
import eu.kanade.tachiyomi.util.category.CategoryUtil
import eu.kanade.tachiyomi.util.filterVisibility
import eu.kanade.tachiyomi.util.lang.isUUID
import eu.kanade.tachiyomi.util.resync
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.unique
import eu.kanade.tachiyomi.util.updateVisibility
import java.util.Date
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.category.toCategoryItem
import org.nekomanga.domain.category.toDbCategory
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.filter.Filter
import org.nekomanga.domain.filter.QueryType
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.manga.MangaContentRating
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.network.message
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.components.UiText
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseViewModel() : ViewModel() {
    private val browseRepository: BrowseRepository = Injekt.get()
    val preferences: PreferencesHelper = Injekt.get()
    private val libraryPreferences: LibraryPreferences = Injekt.get()
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get()
    val securityPreferences: SecurityPreferences = Injekt.get()
    private val db: DatabaseHelper = Injekt.get()

    private val _browseScreenState =
        MutableStateFlow(
            BrowseScreenState(
                isList = preferences.browseAsList().get(),
                libraryEntryVisibility = preferences.browseDisplayMode().get(),
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                isComfortableGrid =
                    libraryPreferences.layout().get() != LibraryDisplayMode.CompactGrid,
                rawColumnCount = libraryPreferences.gridSize().get(),
                filters = createInitialDexFilter(""),
                defaultContentRatings =
                    mangaDexPreferences.visibleContentRatings().get().toImmutableSet(),
                screenType = BrowseScreenType.Homepage,
            )
        )
    val browseScreenState: StateFlow<BrowseScreenState> = _browseScreenState.asStateFlow()

    private val _deepLinkManga = MutableStateFlow<Long?>(null)
    val deepLinkMangaFlow = _deepLinkManga.asStateFlow()

    fun deepLinkQuery(searchBrowse: SearchBrowse) {
        TimberKt.tag("DeepLink").d("Creating filter for ${searchBrowse.query}")
        val filters =
            when (searchBrowse.type) {
                SearchType.Tag -> {
                    val blankFilter = createInitialDexFilter("")
                    if (searchBrowse.query.startsWith("Content rating: ")) {
                        val rating =
                            MangaContentRating.getContentRating(
                                searchBrowse.query.substringAfter("Content rating: ")
                            )
                        blankFilter.copy(
                            contentRatings =
                                blankFilter.contentRatings
                                    .map {
                                        if (it.rating == rating) it.copy(state = true)
                                        else it.copy(state = false)
                                    }
                                    .toPersistentList()
                        )
                    } else {
                        blankFilter.copy(
                            tags =
                                blankFilter.tags
                                    .map {
                                        if (it.tag.prettyPrint.equals(searchBrowse.query, true))
                                            it.copy(state = ToggleableState.On)
                                        else it
                                    }
                                    .toPersistentList()
                        )
                    }
                }

                SearchType.Title -> createInitialDexFilter(searchBrowse.query)
                SearchType.Creator ->
                    createInitialDexFilter("")
                        .copy(
                            queryMode = QueryType.Author,
                            query = Filter.Query(searchBrowse.query, QueryType.Author),
                        )
            }

        _browseScreenState.update { it.copy(filters = filters) }

        getSearchPage()
    }

    private fun createInitialDexFilter(incomingQuery: String): DexFilters {
        val enabledContentRatings = mangaDexPreferences.visibleContentRatings().get()
        val contentRatings =
            MangaContentRating.getOrdered()
                .map { Filter.ContentRating(it, enabledContentRatings.contains(it.key)) }
                .toPersistentList()

        return DexFilters(
            query = Filter.Query(incomingQuery, QueryType.Title),
            contentRatings = contentRatings,
            contentRatingVisible = mangaDexPreferences.showContentRatingFilter().get(),
        )
    }

    private val paginator =
        DefaultPaginator(
            initialKey = _browseScreenState.value.page,
            onLoadUpdated = { _browseScreenState.update { state -> state.copy(pageLoading = it) } },
            onRequest = {
                browseRepository.getSearchPage(
                    browseScreenState.value.page,
                    browseScreenState.value.filters,
                )
            },
            getNextKey = { _browseScreenState.value.page + 1 },
            onError = { resultError ->
                _browseScreenState.update {
                    it.copy(
                        initialLoading = false,
                        pageLoading = false,
                        error =
                            UiText.String(
                                when (resultError) {
                                    is ResultError.Generic -> resultError.errorString
                                    else -> (resultError as ResultError.HttpError).message
                                }
                            ),
                    )
                }
            },
            onSuccess = { hasNextPage, items, nextKey ->
                val allDisplayManga =
                    (_browseScreenState.value.displayMangaHolder.allDisplayManga + items)
                        .distinctBy { it.url }

                val filteredDisplayManga =
                    allDisplayManga.filterVisibility(preferences).toPersistentList()
                _browseScreenState.update { state ->
                    state.copy(
                        screenType = BrowseScreenType.Filter,
                        displayMangaHolder =
                            DisplayMangaHolder(
                                BrowseScreenType.Filter,
                                allDisplayManga.toPersistentList(),
                                filteredDisplayManga.toPersistentList(),
                            ),
                        initialLoading = false,
                        pageLoading = false,
                        page = nextKey,
                        endReached = !hasNextPage,
                    )
                }
                if (filteredDisplayManga.isEmpty()) {
                    loadNextItems()
                }
            },
        )

    init {
        isOnline()

        if (_browseScreenState.value.firstLoad) {
            if (browseScreenState.value.filters.query.text.isNotBlank()) {
                getSearchPage()
            } else {
                getHomepage()
            }
        }

        updateBrowseFilters(_browseScreenState.value.firstLoad)

        viewModelScope.launch {
            _browseScreenState.update {
                it.copy(
                    sideNavMode = SideNavMode.findByPrefValue(preferences.sideNavMode().get()),
                    isLoggedIn = browseRepository.isLoggedIn(),
                    firstLoad = false,
                )
            }
        }

        viewModelScope.launch {
            val categories =
                db.getCategories()
                    .executeAsBlocking()
                    .map { category -> category.toCategoryItem() }
                    .toPersistentList()

            _browseScreenState.update {
                it.copy(
                    categories = categories,
                    promptForCategories =
                        CategoryUtil.shouldShowCategoryPrompt(libraryPreferences, categories),
                )
            }
        }
        viewModelScope.launch {
            preferences.browseAsList().changes().collectLatest {
                _browseScreenState.update { state -> state.copy(isList = it) }
            }
        }

        viewModelScope.launchIO {
            preferences.useVividColorHeaders().changes().distinctUntilChanged().collectLatest {
                enabled ->
                _browseScreenState.update { it.copy(useVividColorHeaders = enabled) }
            }
        }

        viewModelScope.launch {
            securityPreferences.incognitoMode().changes().collectLatest {
                _browseScreenState.update { state -> state.copy(incognitoMode = it) }
            }
        }

        viewModelScope.launch {
            preferences.browseDisplayMode().changes().collectLatest { visibility ->
                _browseScreenState.update { it.copy(libraryEntryVisibility = visibility) }
                viewModelScope.launch {
                    _browseScreenState.update {
                        it.copy(homePageManga = it.homePageManga.updateVisibility(preferences))
                    }
                }
                viewModelScope.launch {
                    _browseScreenState.update {
                        it.copy(
                            displayMangaHolder =
                                it.displayMangaHolder.copy(
                                    filteredDisplayManga =
                                        it.displayMangaHolder.allDisplayManga
                                            .filterVisibility(preferences)
                                            .toPersistentList()
                                )
                        )
                    }
                }
            }
        }
    }

    fun loadNextItems() {
        viewModelScope.launch { paginator.loadNextItems() }
    }

    private fun getHomepage() {
        viewModelScope.launchIO {
            if (!isOnline()) return@launchIO
            browseRepository
                .getHomePage()
                .onFailure { resultError ->
                    _browseScreenState.update { state ->
                        state.copy(
                            error = UiText.String(resultError.message()),
                            initialLoading = false,
                        )
                    }
                }
                .onSuccess {
                    _browseScreenState.update { state ->
                        state.copy(
                            homePageManga = it.updateVisibility(preferences),
                            initialLoading = false,
                        )
                    }
                }
        }
    }

    private fun getFollows(forceUpdate: Boolean) {
        viewModelScope.launchIO {
            if (!isOnline()) return@launchIO
            if (
                forceUpdate ||
                    _browseScreenState.value.displayMangaHolder.resultType !=
                        BrowseScreenType.Follows
            ) {
                _browseScreenState.update { state -> state.copy(initialLoading = true) }
                browseRepository
                    .getFollows()
                    .onFailure {
                        _browseScreenState.update { state ->
                            state.copy(error = UiText.String(it.message()), initialLoading = false)
                        }
                    }
                    .onSuccess {
                        val groupedManga =
                            it.groupBy { manga -> manga.displayTextRes!! }
                                .map { entry ->
                                    entry.key to
                                        entry.value
                                            .map { manga -> manga.copy(displayTextRes = null) }
                                            .toPersistentList()
                                }
                                .toMap()
                                .toImmutableMap()

                        _browseScreenState.update { state ->
                            state.copy(
                                displayMangaHolder =
                                    DisplayMangaHolder(
                                        resultType = BrowseScreenType.Follows,
                                        allDisplayManga =
                                            it.distinctBy { manga -> manga.url }.toPersistentList(),
                                        filteredDisplayManga =
                                            it.filterVisibility(preferences).toPersistentList(),
                                        groupedDisplayManga = groupedManga,
                                    ),
                                initialLoading = false,
                            )
                        }
                    }
            }
        }
    }

    fun retry() {
        viewModelScope.launchIO {
            val initialLoading = _browseScreenState.value.page == 1
            _browseScreenState.update { state ->
                state.copy(initialLoading = initialLoading, error = null)
            }
            when (browseScreenState.value.screenType) {
                BrowseScreenType.Homepage -> getHomepage()
                else -> getSearchPage()
            }
        }
    }

    fun getSearchPage() {
        viewModelScope.launchIO {
            if (!isOnline()) return@launchIO

            _browseScreenState.update { state ->
                state.copy(initialLoading = true, error = null, page = 1)
            }

            val currentQuery = browseScreenState.value.filters.query.text
            val deepLinkType = DeepLinkType.getDeepLinkType(currentQuery)
            val uuid = DeepLinkType.removePrefix(currentQuery, deepLinkType)

            TimberKt.tag("DeepLink").d("DeepLinkType: $deepLinkType")

            if (deepLinkType != DeepLinkType.None) {
                _browseScreenState.update { it.copy(filters = createInitialDexFilter("")) }
            }

            when (deepLinkType) {
                DeepLinkType.None -> {
                    _browseScreenState.update {
                        it.copy(
                            pageLoading = false,
                            initialLoading = true,
                            screenType = BrowseScreenType.Filter,
                            displayMangaHolder =
                                DisplayMangaHolder(
                                    resultType = BrowseScreenType.Filter,
                                    allDisplayManga = persistentListOf(),
                                    filteredDisplayManga = persistentListOf(),
                                ),
                        )
                    }

                    when (val queryMode = browseScreenState.value.filters.queryMode) {
                        QueryType.Author,
                        QueryType.Group -> {
                            when (queryMode) {
                                    QueryType.Author -> browseRepository.getAuthors(currentQuery)
                                    else -> browseRepository.getGroups(currentQuery)
                                }
                                .onFailure {
                                    _browseScreenState.update { state ->
                                        state.copy(
                                            error = UiText.String(it.message()),
                                            initialLoading = false,
                                        )
                                    }
                                }
                                .onSuccess { dr ->
                                    _browseScreenState.update {
                                        it.copy(
                                            otherResults = dr.toPersistentList(),
                                            screenType = BrowseScreenType.Other,
                                            initialLoading = false,
                                        )
                                    }
                                }
                        }
                        QueryType.List -> {
                            if (!currentQuery.isUUID()) {
                                _browseScreenState.update { state ->
                                    state.copy(
                                        error = UiText.String("Invalid List UUID"),
                                        initialLoading = false,
                                    )
                                }
                            } else {
                                browseRepository
                                    .getList(uuid)
                                    .onFailure {
                                        _browseScreenState.update { state ->
                                            state.copy(
                                                error = UiText.String(it.message()),
                                                initialLoading = false,
                                            )
                                        }
                                    }
                                    .onSuccess { allDisplayManga ->
                                        _browseScreenState.update { state ->
                                            state.copy(
                                                screenType = BrowseScreenType.Filter,
                                                displayMangaHolder =
                                                    DisplayMangaHolder(
                                                        BrowseScreenType.Filter,
                                                        allDisplayManga
                                                            .distinctBy { it.url }
                                                            .toPersistentList(),
                                                        allDisplayManga
                                                            .distinctBy { it.url }
                                                            .filterVisibility(preferences)
                                                            .toPersistentList(),
                                                    ),
                                                initialLoading = false,
                                                pageLoading = false,
                                                endReached = true,
                                            )
                                        }
                                    }
                            }
                        }
                        else -> {
                            if (
                                _browseScreenState.value.filters.authorId.isNotBlankAndInvalidUUID()
                            ) {
                                _browseScreenState.update { state ->
                                    state.copy(
                                        error = UiText.String("Invalid Author UUID"),
                                        initialLoading = false,
                                    )
                                }
                            } else if (
                                _browseScreenState.value.filters.groupId.isNotBlankAndInvalidUUID()
                            ) {
                                _browseScreenState.update { state ->
                                    state.copy(
                                        error = UiText.String("Invalid Group UUID"),
                                        initialLoading = false,
                                    )
                                }
                            } else {
                                paginator.loadNextItems()
                            }
                        }
                    }
                }
                DeepLinkType.Error -> {
                    _browseScreenState.update {
                        it.copy(
                            title = UiText.String(""),
                            initialLoading = false,
                            error = UiText.String(uuid),
                        )
                    }
                }
                DeepLinkType.Manga -> {
                    browseRepository
                        .getDeepLinkManga(uuid)
                        .onFailure {
                            _browseScreenState.update { state ->
                                state.copy(
                                    error = UiText.String(it.message()),
                                    initialLoading = false,
                                )
                            }
                        }
                        .onSuccess { displayManga -> _deepLinkManga.value = displayManga.mangaId }
                }
                DeepLinkType.List -> {
                    _browseScreenState.update {
                        it.copy(title = UiText.StringResource(R.string.list))
                    }
                    val searchFilters =
                        createInitialDexFilter("")
                            .copy(
                                queryMode = QueryType.List,
                                query = Filter.Query(text = uuid, type = QueryType.List),
                            )
                    _browseScreenState.update { it.copy(filters = searchFilters) }

                    browseRepository
                        .getList(uuid)
                        .onFailure {
                            _browseScreenState.update { state ->
                                state.copy(
                                    error = UiText.String(it.message()),
                                    initialLoading = false,
                                )
                            }
                        }
                        .onSuccess { allDisplayManga ->
                            _browseScreenState.update { state ->
                                state.copy(
                                    screenType = BrowseScreenType.Filter,
                                    displayMangaHolder =
                                        DisplayMangaHolder(
                                            BrowseScreenType.Filter,
                                            allDisplayManga.toPersistentList(),
                                            allDisplayManga
                                                .filterVisibility(preferences)
                                                .toPersistentList(),
                                        ),
                                    initialLoading = false,
                                    pageLoading = false,
                                    endReached = true,
                                )
                            }
                        }
                }
                DeepLinkType.Author -> {
                    _browseScreenState.update {
                        it.copy(title = UiText.StringResource(R.string.author))
                    }
                    val searchFilters =
                        createInitialDexFilter("").copy(authorId = Filter.AuthorId(uuid = uuid))
                    _browseScreenState.update { it.copy(filters = searchFilters) }

                    paginator.loadNextItems()
                }
                DeepLinkType.Group -> {
                    _browseScreenState.update {
                        it.copy(title = UiText.StringResource(R.string.scanlator_group))
                    }
                    val searchFilters =
                        createInitialDexFilter("").copy(groupId = Filter.GroupId(uuid = uuid))
                    _browseScreenState.update { it.copy(filters = searchFilters) }

                    paginator.loadNextItems()
                }
            }
        }
    }

    fun otherClick(uuid: String) {
        viewModelScope.launch {
            if (browseScreenState.value.filters.queryMode == QueryType.Author) {
                _browseScreenState.update {
                    it.copy(
                        filters = createInitialDexFilter("").copy(authorId = Filter.AuthorId(uuid))
                    )
                }
                getSearchPage()
            } else if (browseScreenState.value.filters.queryMode == QueryType.Group) {
                _browseScreenState.update {
                    it.copy(
                        filters = createInitialDexFilter("").copy(groupId = Filter.GroupId(uuid))
                    )
                }
                getSearchPage()
            }
        }
    }

    fun onDeepLinkMangaHandled() {
        _deepLinkManga.value = null
    }

    fun randomManga() {
        viewModelScope.launch {
            _browseScreenState.update { it.copy(initialLoading = true) }
            browseRepository
                .getRandomManga()
                .onFailure { error ->
                    _browseScreenState.update {
                        it.copy(initialLoading = false, error = UiText.String(error.message()))
                    }
                }
                .onSuccess { displayManga ->
                    _browseScreenState.update { it.copy(initialLoading = false) }
                    _deepLinkManga.value = displayManga.mangaId
                }
        }
    }

    fun toggleFavorite(mangaId: Long, categoryItems: List<CategoryItem>) {
        viewModelScope.launch {
            val editManga = db.getManga(mangaId).executeAsBlocking()!!
            editManga.apply {
                favorite = !favorite
                date_added =
                    when (favorite) {
                        true -> Date().time
                        false -> 0
                    }
            }
            db.insertManga(editManga).executeAsBlocking()

            updateDisplayManga(mangaId, editManga.favorite)

            if (editManga.favorite) {
                val defaultCategory = libraryPreferences.defaultCategory().get()

                if (categoryItems.isEmpty() && defaultCategory != -1) {
                    _browseScreenState.value.categories
                        .firstOrNull { defaultCategory == it.id }
                        ?.let {
                            val categories =
                                listOf(MangaCategory.create(editManga, it.toDbCategory()))
                            db.setMangaCategories(categories, listOf(editManga))
                        }
                } else if (categoryItems.isNotEmpty()) {
                    val categories =
                        categoryItems.map { MangaCategory.create(editManga, it.toDbCategory()) }
                    db.setMangaCategories(categories, listOf(editManga))
                }
            }
        }
    }

    private fun updateDisplayManga(mangaId: Long, favorite: Boolean) {
        viewModelScope.launch {
            val tempList =
                _browseScreenState.value.homePageManga
                    .map { homePageManga ->
                        val list = homePageManga.displayManga
                        val index = list.indexOfFirst { it.mangaId == mangaId }
                        if (index == -1) {
                            homePageManga
                        } else {
                            val updatedList =
                                list.set(index, list[index].copy(inLibrary = favorite))
                            homePageManga.copy(displayManga = updatedList)
                        }
                    }
                    .toPersistentList()
            _browseScreenState.update { it.copy(homePageManga = tempList) }
        }
        viewModelScope.launch {
            val index =
                _browseScreenState.value.displayMangaHolder.allDisplayManga.indexOfFirst {
                    it.mangaId == mangaId
                }
            if (index >= 0) {
                val tempList =
                    _browseScreenState.value.displayMangaHolder.allDisplayManga.toMutableList()
                val tempDisplayManga = tempList[index].copy(inLibrary = favorite)
                tempList[index] = tempDisplayManga
                _browseScreenState.update {
                    it.copy(
                        displayMangaHolder =
                            it.displayMangaHolder.copy(
                                allDisplayManga = tempList.toPersistentList()
                            )
                    )
                }

                val filteredIndex =
                    _browseScreenState.value.displayMangaHolder.filteredDisplayManga.indexOfFirst {
                        it.mangaId == mangaId
                    }
                if (filteredIndex >= 0) {
                    val tempFilterList =
                        _browseScreenState.value.displayMangaHolder.filteredDisplayManga
                            .toMutableList()
                    tempFilterList[filteredIndex] = tempDisplayManga
                    _browseScreenState.update {
                        it.copy(
                            displayMangaHolder =
                                it.displayMangaHolder.copy(
                                    filteredDisplayManga = tempFilterList.toPersistentList()
                                )
                        )
                    }
                }
            }
        }
    }

    fun saveFilter(name: String) {
        viewModelScope.launch {
            val browseFilter =
                BrowseFilterImpl(
                    name = name,
                    dexFilters = Json.encodeToString(browseScreenState.value.filters),
                )
            db.insertBrowseFilter(browseFilter).executeAsBlocking()
            updateBrowseFilters()
        }
    }

    fun loadFilter(browseFilterImpl: BrowseFilterImpl) {
        viewModelScope.launch {
            val dexFilters = Json.decodeFromString<DexFilters>(browseFilterImpl.dexFilters)
            _browseScreenState.update { it.copy(filters = dexFilters) }
        }
    }

    fun markFilterAsDefault(name: String, makeDefault: Boolean) {
        viewModelScope.launch {
            val updatedFilters =
                browseScreenState.value.savedFilters.map {
                    if (it.name == name) {
                        it.copy(default = makeDefault)
                    } else {
                        it.copy(default = false)
                    }
                }
            db.insertBrowseFilters(updatedFilters).executeAsBlocking()
            updateBrowseFilters()
        }
    }

    fun deleteFilter(name: String) {
        viewModelScope.launch {
            db.deleteBrowseFilter(name).executeAsBlocking()
            updateBrowseFilters()
        }
    }

    fun resetFilter() {
        viewModelScope.launch {
            val resetFilters = createInitialDexFilter("")
            _browseScreenState.update { it.copy(filters = resetFilters) }
        }
    }

    fun searchTag(tag: String) {
        viewModelScope.launch {
            val blankFilter = createInitialDexFilter("")

            val filters =
                if (tag.startsWith("Content rating: ")) {
                    val rating =
                        MangaContentRating.getContentRating(tag.substringAfter("Content rating: "))
                    blankFilter.copy(
                        contentRatings =
                            blankFilter.contentRatings
                                .map {
                                    if (it.rating == rating) it.copy(state = true)
                                    else it.copy(state = false)
                                }
                                .toPersistentList()
                    )
                } else {
                    blankFilter.copy(
                        tags =
                            blankFilter.tags
                                .map {
                                    if (it.tag.prettyPrint.equals(tag, true))
                                        it.copy(state = ToggleableState.On)
                                    else it
                                }
                                .toPersistentList()
                    )
                }
            _browseScreenState.update { it.copy(filters = filters) }
            getSearchPage()
        }
    }

    fun searchCreator(creator: String) {
        viewModelScope.launch {
            val blankFilter = createInitialDexFilter("")
            _browseScreenState.update {
                it.copy(
                    filters =
                        blankFilter.copy(
                            queryMode = QueryType.Author,
                            query = Filter.Query(creator, QueryType.Author),
                        )
                )
            }

            getSearchPage()
        }
    }

    fun filterChanged(newFilter: Filter) {
        viewModelScope.launch {
            val updatedFilters =
                when (newFilter) {
                    is Filter.ContentRating -> {
                        val list =
                            lookupAndReplaceEntry(
                                browseScreenState.value.filters.contentRatings.toPersistentList(),
                                { it.rating == newFilter.rating },
                                newFilter,
                            )
                        if (list.none { it.state }) {
                            val default =
                                lookupAndReplaceEntry(
                                    list,
                                    { it.rating == MangaContentRating.Safe },
                                    Filter.ContentRating(MangaContentRating.Safe, true),
                                )
                            browseScreenState.value.filters.copy(contentRatings = default)
                        } else {
                            browseScreenState.value.filters.copy(contentRatings = list)
                        }
                    }
                    is Filter.OriginalLanguage -> {
                        val list =
                            lookupAndReplaceEntry(
                                browseScreenState.value.filters.originalLanguage.toPersistentList(),
                                { it.language == newFilter.language },
                                newFilter,
                            )
                        browseScreenState.value.filters.copy(originalLanguage = list)
                    }
                    is Filter.PublicationDemographic -> {
                        val list =
                            lookupAndReplaceEntry(
                                browseScreenState.value.filters.publicationDemographics
                                    .toPersistentList(),
                                { it.demographic == newFilter.demographic },
                                newFilter,
                            )
                        browseScreenState.value.filters.copy(publicationDemographics = list)
                    }
                    is Filter.Status -> {
                        val list =
                            lookupAndReplaceEntry(
                                browseScreenState.value.filters.statuses.toPersistentList(),
                                { it.status == newFilter.status },
                                newFilter,
                            )
                        browseScreenState.value.filters.copy(statuses = list)
                    }
                    is Filter.Tag -> {
                        val list =
                            lookupAndReplaceEntry(
                                browseScreenState.value.filters.tags.toPersistentList(),
                                { it.tag == newFilter.tag },
                                newFilter,
                            )
                        browseScreenState.value.filters.copy(tags = list)
                    }
                    is Filter.Sort -> {
                        val filterMode =
                            when (newFilter.state) {
                                true -> newFilter.sort
                                false -> MdSort.Best
                            }

                        browseScreenState.value.filters.copy(
                            sort = Filter.Sort.getSortList(filterMode).toPersistentList()
                        )
                    }
                    is Filter.HasAvailableChapters -> {
                        browseScreenState.value.filters.copy(hasAvailableChapters = newFilter)
                    }
                    is Filter.TagInclusionMode -> {
                        browseScreenState.value.filters.copy(tagInclusionMode = newFilter)
                    }
                    is Filter.TagExclusionMode -> {
                        browseScreenState.value.filters.copy(tagExclusionMode = newFilter)
                    }
                    is Filter.Query -> {
                        when (newFilter.type) {
                            QueryType.Title -> {
                                browseScreenState.value.filters.copy(
                                    queryMode = QueryType.Title,
                                    query = newFilter,
                                )
                            }
                            QueryType.Author -> {
                                browseScreenState.value.filters.copy(
                                    queryMode = QueryType.Author,
                                    query = newFilter,
                                )
                            }
                            QueryType.Group -> {
                                browseScreenState.value.filters.copy(
                                    queryMode = QueryType.Group,
                                    query = newFilter,
                                )
                            }
                            QueryType.List -> {
                                browseScreenState.value.filters.copy(
                                    queryMode = QueryType.List,
                                    query = newFilter,
                                )
                            }
                        }
                    }
                    is Filter.AuthorId -> {
                        browseScreenState.value.filters.copy(authorId = newFilter)
                    }
                    is Filter.GroupId -> {
                        browseScreenState.value.filters.copy(groupId = newFilter)
                    }
                }

            _browseScreenState.update { it.copy(filters = updatedFilters) }
        }
    }

    private fun <T> lookupAndReplaceEntry(
        list: PersistentList<T>,
        indexMethod: (T) -> Boolean,
        newEntry: T,
    ): PersistentList<T> {
        val index = list.indexOfFirst { indexMethod(it) }
        return list.set(index, newEntry)
    }

    /** Add New Category */
    fun addNewCategory(newCategory: String) {
        viewModelScope.launchIO {
            val category = Category.create(newCategory)
            category.order = (_browseScreenState.value.categories.maxOfOrNull { it.order } ?: 0) + 1
            db.insertCategory(category).executeAsBlocking()
            _browseScreenState.update {
                it.copy(
                    categories =
                        db.getCategories()
                            .executeAsBlocking()
                            .map { category -> category.toCategoryItem() }
                            .toPersistentList()
                )
            }
        }
    }

    fun switchDisplayMode() {
        preferences.browseAsList().set(!browseScreenState.value.isList)
    }

    fun changeScreenType(browseScreenType: BrowseScreenType, forceUpdate: Boolean = false) {
        viewModelScope.launch {
            when (browseScreenType) {
                BrowseScreenType.Follows -> getFollows(forceUpdate)
                BrowseScreenType.Filter -> getSearchPage()
                else -> Unit
            }
            _browseScreenState.update { it.copy(screenType = browseScreenType, error = null) }
        }
    }

    fun switchLibraryEntryVisibility(visibility: Int) {
        preferences.browseDisplayMode().set(visibility)
    }

    private fun updateBrowseFilters(initialLoad: Boolean = false) {
        viewModelScope.launch {
            val filters = db.getBrowseFilters().executeAsBlocking().toPersistentList()
            _browseScreenState.update { it.copy(savedFilters = filters) }
            if (initialLoad) {
                filters
                    .firstOrNull { it.default }
                    ?.let { filter ->
                        val dexFilters = Json.decodeFromString<DexFilters>(filter.dexFilters)
                        _browseScreenState.update { it.copy(filters = dexFilters) }
                    }
            }
        }
    }

    fun updateMangaForChanges() {
        if (!_browseScreenState.value.firstLoad) {
            viewModelScope.launch {
                val newHomePageManga =
                    _browseScreenState.value.homePageManga.resync(db).updateVisibility(preferences)
                _browseScreenState.update { it.copy(homePageManga = newHomePageManga) }
            }
            viewModelScope.launch {
                val allDisplayManga =
                    _browseScreenState.value.displayMangaHolder.allDisplayManga.resync(db).unique()
                _browseScreenState.update {
                    it.copy(
                        displayMangaHolder =
                            it.displayMangaHolder.copy(
                                allDisplayManga = allDisplayManga.toPersistentList(),
                                filteredDisplayManga =
                                    allDisplayManga.filterVisibility(preferences).toPersistentList(),
                            )
                    )
                }
            }
        }
    }

    /** Check if can access internet */
    private fun isOnline(): Boolean {
        // TODO use networkstate  return if (view?.activity?.isOnline() == true) {
        _browseScreenState.update { it.copy(hideFooterButton = false) }
        return true
        /* } else {
            viewModelScope.launch {
                _browseScreenState.update {
                    it.copy(
                        initialLoading = false,
                        hideFooterButton = true,
                        error = UiText.StringResource(R.string.no_network_connection),
                    )
                }
            }
            false
        }*/
    }
}
