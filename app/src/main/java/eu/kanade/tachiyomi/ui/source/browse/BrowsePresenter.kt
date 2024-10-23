package eu.kanade.tachiyomi.ui.source.browse

import androidx.compose.ui.state.ToggleableState
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.BrowseFilterImpl
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.utils.MdSort
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.category.CategoryUtil
import eu.kanade.tachiyomi.util.filterVisibility
import eu.kanade.tachiyomi.util.lang.isUUID
import eu.kanade.tachiyomi.util.resync
import eu.kanade.tachiyomi.util.system.SideNavMode
import eu.kanade.tachiyomi.util.system.isOnline
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.unique
import eu.kanade.tachiyomi.util.updateVisibility
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.nekomanga.R
import org.nekomanga.core.preferences.toggle
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
import org.nekomanga.presentation.components.UiText
import org.nekomanga.util.paging.DefaultPaginator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowsePresenter(
    private val incomingQuery: String,
    private val browseRepository: BrowseRepository = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    val securityPreferences: SecurityPreferences = Injekt.get(),
    private val db: DatabaseHelper = Injekt.get(),
) : BaseCoroutinePresenter<BrowseController>() {

    private val _browseScreenState =
        MutableStateFlow(
            BrowseScreenState(
                isList = preferences.browseAsList().get(),
                hideFooterButton = true,
                showLibraryEntries = preferences.browseShowLibrary().get(),
                outlineCovers = libraryPreferences.outlineOnCovers().get(),
                isComfortableGrid = libraryPreferences.layout().get() == 2,
                rawColumnCount = libraryPreferences.gridSize().get(),
                filters = createInitialDexFilter(incomingQuery),
                defaultContentRatings =
                    preferences.contentRatingSelections().get().toImmutableSet(),
                screenType = BrowseScreenType.Homepage,
            )
        )
    val browseScreenState: StateFlow<BrowseScreenState> = _browseScreenState.asStateFlow()

    private fun createInitialDexFilter(incomingQuery: String): DexFilters {
        val enabledContentRatings = preferences.contentRatingSelections().get()
        val contentRatings =
            MangaContentRating.getOrdered()
                .map { Filter.ContentRating(it, enabledContentRatings.contains(it.key)) }
                .toImmutableList()

        return DexFilters(
            query = Filter.Query(incomingQuery, QueryType.Title),
            contentRatings = contentRatings,
            contentRatingVisible = preferences.showContentRatingFilter().get(),
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
                _browseScreenState.update { state ->
                    val allDisplayManga =
                        (state.displayMangaHolder.allDisplayManga + items).distinctBy { it.url }
                    state.copy(
                        screenType = BrowseScreenType.Filter,
                        displayMangaHolder =
                            DisplayMangaHolder(
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

        if (_browseScreenState.value.firstLoad) {
            if (browseScreenState.value.filters.query.text.isNotBlank()) {
                getSearchPage()
            } else {
                getHomepage()
            }
        }

        updateBrowseFilters(_browseScreenState.value.firstLoad)

        presenterScope.launch {
            _browseScreenState.update {
                it.copy(
                    sideNavMode = SideNavMode.findByPrefValue(preferences.sideNavMode().get()),
                    isLoggedIn = browseRepository.isLoggedIn(),
                    firstLoad = false,
                )
            }
        }

        presenterScope.launch {
            val categories =
                db.getCategories()
                    .executeAsBlocking()
                    .map { category -> category.toCategoryItem() }
                    .toPersistentList()

            _browseScreenState.update {
                it.copy(
                    categories = categories,
                    promptForCategories =
                        CategoryUtil.shouldShowCategoryPrompt(preferences, categories),
                )
            }
        }
        presenterScope.launch {
            preferences.browseAsList().changes().collectLatest {
                _browseScreenState.update { state -> state.copy(isList = it) }
            }
        }

        presenterScope.launch {
            securityPreferences.incognitoMode().changes().collectLatest {
                _browseScreenState.update { state -> state.copy(incognitoMode = it) }
            }
        }

        presenterScope.launch {
            preferences.browseShowLibrary().changes().collectLatest { bool ->
                _browseScreenState.update { it.copy(showLibraryEntries = bool) }
                presenterScope.launch {
                    _browseScreenState.update {
                        it.copy(homePageManga = it.homePageManga.updateVisibility(preferences))
                    }
                }
                presenterScope.launch {
                    _browseScreenState.update {
                        it.copy(
                            displayMangaHolder =
                                it.displayMangaHolder.copy(
                                    filteredDisplayManga =
                                        it.displayMangaHolder.allDisplayManga
                                            .filterVisibility(preferences)
                                            .toImmutableList()
                                )
                        )
                    }
                }
            }
        }
    }

    fun loadNextItems() {
        presenterScope.launch { paginator.loadNextItems() }
    }

    private fun getHomepage() {
        presenterScope.launchIO {
            if (!isOnline()) return@launchIO
            browseRepository
                .getHomePage()
                .onFailure { resultError ->
                    _browseScreenState.update { state ->
                        state.copy(
                            error = UiText.String(resultError.message()),
                            initialLoading = false,
                            hideFooterButton = false,
                        )
                    }
                }
                .onSuccess {
                    _browseScreenState.update { state ->
                        state.copy(
                            homePageManga = it.updateVisibility(preferences),
                            initialLoading = false,
                            hideFooterButton = false,
                        )
                    }
                }
        }
    }

    private fun getFollows(forceUpdate: Boolean) {
        presenterScope.launchIO {
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
                        _browseScreenState.update { state ->
                            state.copy(
                                displayMangaHolder =
                                    DisplayMangaHolder(
                                        BrowseScreenType.Follows,
                                        it.distinctBy { it.url }.toImmutableList(),
                                        it.filterVisibility(preferences).toImmutableList(),
                                    ),
                                initialLoading = false,
                            )
                        }
                    }
            }
        }
    }

    fun retry() {
        presenterScope.launchIO {
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
        presenterScope.launchIO {
            if (!isOnline()) return@launchIO

            _browseScreenState.update { state ->
                state.copy(initialLoading = true, error = null, page = 1)
            }

            val currentQuery = browseScreenState.value.filters.query.text
            val deepLinkType = DeepLinkType.getDeepLinkType(currentQuery)
            val uuid = DeepLinkType.removePrefix(currentQuery, deepLinkType)

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
                                            otherResults = dr.toImmutableList(),
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
                                                            .toImmutableList(),
                                                        allDisplayManga
                                                            .distinctBy { it.url }
                                                            .filterVisibility(preferences)
                                                            .toImmutableList(),
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
                            isDeepLink = true,
                            title = UiText.String(""),
                            initialLoading = false,
                            error = UiText.String(uuid),
                        )
                    }
                }
                DeepLinkType.Manga -> {
                    _browseScreenState.update {
                        it.copy(isDeepLink = true, title = UiText.String(""))
                    }
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
                        .onSuccess { dm ->
                            if (
                                incomingQuery.isNotBlank() &&
                                    !_browseScreenState.value.handledIncomingQuery
                            ) {
                                _browseScreenState.update {
                                    it.copy(
                                        filters =
                                            it.filters.copy(
                                                query = Filter.Query("", QueryType.Title)
                                            ),
                                        handledIncomingQuery = true,
                                    )
                                }
                            }
                            view?.openManga(dm.mangaId, true)
                        }
                }
                DeepLinkType.List -> {
                    _browseScreenState.update {
                        it.copy(isDeepLink = true, title = UiText.StringResource(R.string.list))
                    }
                    val searchFilters =
                        createInitialDexFilter("")
                            .copy(
                                queryMode = QueryType.List,
                                query = Filter.Query(text = uuid, type = QueryType.List),
                            )
                    _browseScreenState.update { it.copy(filters = searchFilters) }
                    if (!_browseScreenState.value.handledIncomingQuery) {
                        _browseScreenState.update { it.copy(handledIncomingQuery = true) }
                    }
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
                                            allDisplayManga.toImmutableList(),
                                            allDisplayManga
                                                .filterVisibility(preferences)
                                                .toImmutableList(),
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
                        it.copy(isDeepLink = true, title = UiText.StringResource(R.string.author))
                    }
                    val searchFilters =
                        createInitialDexFilter("").copy(authorId = Filter.AuthorId(uuid = uuid))
                    _browseScreenState.update { it.copy(filters = searchFilters) }
                    if (!_browseScreenState.value.handledIncomingQuery) {
                        _browseScreenState.update { it.copy(handledIncomingQuery = true) }
                    }
                    paginator.loadNextItems()
                }
                DeepLinkType.Group -> {
                    _browseScreenState.update {
                        it.copy(
                            isDeepLink = true,
                            title = UiText.StringResource(R.string.scanlator_group),
                        )
                    }
                    val searchFilters =
                        createInitialDexFilter("").copy(groupId = Filter.GroupId(uuid = uuid))
                    _browseScreenState.update { it.copy(filters = searchFilters) }
                    if (!_browseScreenState.value.handledIncomingQuery) {
                        _browseScreenState.update { it.copy(handledIncomingQuery = true) }
                    }
                    paginator.loadNextItems()
                }
            }
        }
    }

    fun otherClick(uuid: String) {
        presenterScope.launch {
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

    fun toggleIncognitoMode() {
        presenterScope.launch { securityPreferences.incognitoMode().toggle() }
    }

    fun randomManga() {
        presenterScope.launch {
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
                    view?.openManga(displayManga.mangaId)
                }
        }
    }

    fun toggleFavorite(mangaId: Long, categoryItems: List<CategoryItem>) {
        presenterScope.launch {
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
                val defaultCategory = preferences.defaultCategory().get()

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
        presenterScope.launch {
            val tempList =
                _browseScreenState.value.homePageManga
                    .map { homePageManga ->
                        val index =
                            homePageManga.displayManga.indexOfFirst { it.mangaId == mangaId }
                        if (index == -1) {
                            homePageManga
                        } else {
                            val tempMangaList = homePageManga.displayManga.toMutableList()
                            val tempDisplayManga = tempMangaList[index].copy(inLibrary = favorite)
                            tempMangaList[index] = tempDisplayManga
                            homePageManga.copy(displayManga = tempMangaList.toImmutableList())
                        }
                    }
                    .toImmutableList()
            _browseScreenState.update { it.copy(homePageManga = tempList) }
        }
        presenterScope.launch {
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
        presenterScope.launch {
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
        presenterScope.launch {
            val dexFilters = Json.decodeFromString<DexFilters>(browseFilterImpl.dexFilters)
            _browseScreenState.update { it.copy(filters = dexFilters) }
        }
    }

    fun markFilterAsDefault(name: String, makeDefault: Boolean) {
        presenterScope.launch {
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

    fun searchTag(tag: String) {
        presenterScope.launch {
            val blankFilter = createInitialDexFilter("")

            val filters =
                if (tag.startsWith("Content rating: ")) {
                    val rating =
                        MangaContentRating.getContentRating(tag.substringAfter("Content rating: "))
                    blankFilter.copy(
                        contentRatings =
                            blankFilter.contentRatings.map {
                                if (it.rating == rating) it.copy(state = true)
                                else it.copy(state = false)
                            }
                    )
                } else {
                    blankFilter.copy(
                        tags =
                            blankFilter.tags.map {
                                if (it.tag.prettyPrint.equals(tag, true))
                                    it.copy(state = ToggleableState.On)
                                else it
                            }
                    )
                }
            _browseScreenState.update { it.copy(filters = filters) }
            getSearchPage()
        }
    }

    fun searchCreator(creator: String) {
        presenterScope.launch {
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
        presenterScope.launch {
            val updatedFilters =
                when (newFilter) {
                    is Filter.ContentRating -> {
                        val list =
                            lookupAndReplaceEntry(
                                browseScreenState.value.filters.contentRatings,
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
                                browseScreenState.value.filters.originalLanguage,
                                { it.language == newFilter.language },
                                newFilter,
                            )
                        browseScreenState.value.filters.copy(originalLanguage = list)
                    }
                    is Filter.PublicationDemographic -> {
                        val list =
                            lookupAndReplaceEntry(
                                browseScreenState.value.filters.publicationDemographics,
                                { it.demographic == newFilter.demographic },
                                newFilter,
                            )
                        browseScreenState.value.filters.copy(publicationDemographics = list)
                    }
                    is Filter.Status -> {
                        val list =
                            lookupAndReplaceEntry(
                                browseScreenState.value.filters.statuses,
                                { it.status == newFilter.status },
                                newFilter,
                            )
                        browseScreenState.value.filters.copy(statuses = list)
                    }
                    is Filter.Tag -> {
                        val list =
                            lookupAndReplaceEntry(
                                browseScreenState.value.filters.tags,
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
                            sort = Filter.Sort.getSortList(filterMode)
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
        list: List<T>,
        indexMethod: (T) -> Boolean,
        newEntry: T,
    ): ImmutableList<T> {
        val index = list.indexOfFirst { indexMethod(it) }
        val mutableList = list.toMutableList()
        mutableList[index] = newEntry
        return mutableList.toImmutableList()
    }

    /** Add New Category */
    fun addNewCategory(newCategory: String) {
        presenterScope.launchIO {
            val category = Category.create(newCategory)
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
        presenterScope.launch {
            when (browseScreenType) {
                BrowseScreenType.Follows -> getFollows(forceUpdate)
                BrowseScreenType.Filter -> getSearchPage()
                else -> Unit
            }
            _browseScreenState.update { it.copy(screenType = browseScreenType, error = null) }
        }
    }

    fun switchLibraryVisibility() {
        preferences.browseShowLibrary().set(!browseScreenState.value.showLibraryEntries)
    }

    private fun updateBrowseFilters(initialLoad: Boolean = false) {
        presenterScope.launch {
            val filters = db.getBrowseFilters().executeAsBlocking().toImmutableList()
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
            presenterScope.launch {
                val newHomePageManga =
                    _browseScreenState.value.homePageManga.resync(db).updateVisibility(preferences)
                _browseScreenState.update { it.copy(homePageManga = newHomePageManga) }
            }
            presenterScope.launch {
                val allDisplayManga =
                    _browseScreenState.value.displayMangaHolder.allDisplayManga.resync(db).unique()
                _browseScreenState.update {
                    it.copy(
                        displayMangaHolder =
                            it.displayMangaHolder.copy(
                                allDisplayManga = allDisplayManga.toImmutableList(),
                                filteredDisplayManga =
                                    allDisplayManga.filterVisibility(preferences).toImmutableList(),
                            )
                    )
                }
            }
        }
    }

    /** Check if can access internet */
    private fun isOnline(): Boolean {
        return if (view?.activity?.isOnline() == true) {
            true
        } else {
            presenterScope.launch {
                _browseScreenState.update {
                    it.copy(
                        initialLoading = false,
                        error = UiText.StringResource(R.string.no_network_connection),
                    )
                }
            }
            false
        }
    }
}
