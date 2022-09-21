package eu.kanade.tachiyomi.ui.source.browse

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elvishew.xlog.XLog
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.BrowseSourceControllerBinding
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.follows.FollowsController
import eu.kanade.tachiyomi.ui.main.FloatingSearchInterface
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.manga.MangaDetailController
import eu.kanade.tachiyomi.ui.source.latest.LatestController
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.addOrRemoveToFavorites
import eu.kanade.tachiyomi.util.system.connectivityManager
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.applyBottomAnimatedInsets
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.util.view.isControllerVisible
import eu.kanade.tachiyomi.util.view.requestFilePermissionsSafe
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import eu.kanade.tachiyomi.widget.EmptyView
import kotlin.math.max
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

/**
 * Controller to manage the catalogues available in the app.
 */
open class BrowseSourceController(bundle: Bundle) :
    NucleusController<BrowseSourceControllerBinding, BrowseSourcePresenter>(bundle),
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    RootSearchInterface,
    FloatingSearchInterface,
    FlexibleAdapter.EndlessScrollListener {

    constructor(
        searchQuery: String? = null,
        applyInset: Boolean = true,
        deepLink: Boolean = false,
    ) : this(
        Bundle().apply
        {
            putBoolean(APPLY_INSET, applyInset)
            putBoolean(DEEP_LINK, deepLink)

            if (searchQuery != null) {
                putString(SEARCH_QUERY_KEY, searchQuery)
            }
        },
    )

    constructor(applyInset: Boolean = true) : this(
        Bundle().apply {
            putBoolean(APPLY_INSET, applyInset)
        },
    )

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * Adapter containing the list of manga from the catalogue.
     */
    private var adapter: FlexibleAdapter<IFlexible<*>>? = null

    /**
     * Snackbar containing an error message when a request fails.
     */
    private var snack: Snackbar? = null

    /**
     * Recycler view with the list of results.
     */
    private var recycler: RecyclerView? = null

    /**
     * Endless loading item.
     */
    private var progressItem: ProgressItem? = null

    /** Current filter sheet */
    var filterSheet: SourceFilterSheet? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return searchTitle(presenter.source.name)
    }

    override fun getSearchTitle(): String? {
        return searchTitle(presenter.source.name)
    }

    override fun createPresenter(): BrowseSourcePresenter {
        return BrowseSourcePresenter(
            args.getString(SEARCH_QUERY_KEY) ?: "",
            args.getBoolean(DEEP_LINK),
        )
    }

    override fun createBinding(inflater: LayoutInflater) =
        BrowseSourceControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Initialize adapter, scroll listener and recycler views
        adapter = FlexibleAdapter(null, this)
        setupRecycler(view)

        //

        binding.fab.setOnClickListener { showFilters() }
        binding.swipeRefresh.isEnabled = false

        updateFab()
        binding.progress.isVisible = true
        requestFilePermissionsSafe(301, preferences)
    }

    override fun onDestroyView(view: View) {
        adapter = null
        snack = null
        recycler = null
        super.onDestroyView(view)
    }

    private fun setupRecycler(view: View) {
        var oldPosition = RecyclerView.NO_POSITION
        val oldRecycler = binding.catalogueView.getChildAt(1)
        if (oldRecycler is RecyclerView) {
            oldPosition =
                (oldRecycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            oldRecycler.adapter = null

            binding.catalogueView.removeView(oldRecycler)
        }

        val recycler = if (presenter.prefs.browseAsList().get()) {
            RecyclerView(view.context).apply {
                id = R.id.recycler
                layoutManager = LinearLayoutManager(context)
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        } else {
            (binding.catalogueView.inflate(R.layout.manga_recycler_autofit) as AutofitRecyclerView).apply {
                setGridSize(preferences)

                (layoutManager as androidx.recyclerview.widget.GridLayoutManager).spanSizeLookup =
                    object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when (adapter?.getItemViewType(position)) {
                                R.layout.manga_grid_item, null -> 1
                                else -> spanCount
                            }
                        }
                    }
            }
        }
        recycler.clipToPadding = false
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter
        var handleInsets = true

        scrollViewWith(
            recycler,
            true,
            afterInsets = { insets ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    binding.fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = insets.getInsets(systemBars() or ime()).bottom + 16.dpToPx
                    }
                }
            },
            onBottomNavUpdate = {
                updateFab()
            },
        )
        binding.fab.applyBottomAnimatedInsets(16.dpToPx)

        recycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0 && !binding.fab.isExtended) {
                        binding.fab.extend()
                    } else if (dy > 0 && binding.fab.isExtended) {
                        binding.fab.shrink()
                    }
                }
            },
        )

        binding.catalogueView.addView(recycler, 1)
        if (oldPosition != RecyclerView.NO_POSITION) {
            recycler.layoutManager?.scrollToPosition(oldPosition)
        }
        this.recycler = recycler
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.browse_source, menu)

        // Initialize search menu
        val searchItem = activityBinding?.searchToolbar?.searchItem
        val searchView = activityBinding?.searchToolbar?.searchView

        activityBinding?.searchToolbar?.setQueryHint("")

        val query = presenter.query
        if (query.isNotBlank()) {
            searchItem?.expandActionView()
            searchView?.setQuery(query, true)
            searchView?.clearFocus()
        } else if (activityBinding?.searchToolbar?.isSearchExpanded == true) {
            searchItem?.collapseActionView()
            searchView?.setQuery("", true)
        }

        setOnQueryTextChangeListener(searchView, onlyOnSubmit = true, hideKbOnSubmit = true) {
            searchWithQuery(it ?: "")
            true
        }

        // Show next display mode
        updateDisplayMenuItem(menu)
    }

    private fun updateDisplayMenuItem(
        menu: Menu?,
        isListMode: Boolean? = null,
        hideLibraryManga: Boolean? = null,
    ) {
        menu?.findItem(R.id.action_display_mode)?.apply {
            val icon = if (isListMode ?: presenter.prefs.browseAsList().get()) {
                R.drawable.ic_view_module_24dp
            } else {
                R.drawable.ic_view_list_24dp
            }
            setIcon(icon)
        }
        menu?.findItem(R.id.action_toggle_have_already)?.apply {
            val icon = if (hideLibraryManga ?: presenter.prefs.browseShowLibrary().get()) {
                R.drawable.ic_eye_off_24dp
            } else {
                R.drawable.ic_eye_24dp
            }
            setIcon(icon)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_display_mode -> swapDisplayMode()
            R.id.action_toggle_have_already -> swapLibraryVisibility()
            /*   R.id.action_open_in_web_view -> openInWebView()
               R.id.action_open_merged_source_in_web_view -> openInWebView(false)*/

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showFilters() {
        if (filterSheet != null) return
        val sheet = SourceFilterSheet(activity!!)
        filterSheet = sheet
        sheet.setFilters(presenter.filterItems)
        presenter.filtersChanged = false
        val oldFilters = mutableListOf<Any?>()
        for (i in presenter.sourceFilters) {
            if (i is Filter.Group<*>) {
                val subFilters = mutableListOf<Any?>()
                for (j in i.state) {
                    subFilters.add((j as Filter<*>).state)
                }
                oldFilters.add(subFilters)
            } else {
                oldFilters.add(i.state)
            }
        }
        sheet.onSearchClicked = {
            var matches = true
            for (i in presenter.sourceFilters.indices) {
                val filter = oldFilters[i]
                if (filter is List<*>) {
                    for (j in filter.indices) {
                        if (filter[j] !=
                            (
                                (presenter.sourceFilters[i] as Filter.Group<*>).state[j] as
                                    Filter<*>
                                ).state
                        ) {
                            matches = false
                            break
                        }
                    }
                } else if (oldFilters[i] != presenter.sourceFilters[i].state) {
                    matches = false
                    break
                }
            }
            if (!matches) {
                showProgressBar()
                adapter?.clear()
                presenter.setSourceFilter(presenter.sourceFilters)
            }
        }

        sheet.onResetClicked = {
            presenter.appliedFilters = FilterList()
            val newFilters = presenter.source.getFilterList()
            sheet.setFilters(presenter.filterItems)
            sheet.dismiss()
            presenter.sourceFilters = newFilters
            adapter?.clear()
            presenter.setSourceFilter(FilterList())
        }

        sheet.onRandomClicked = {
            viewScope.launch {
                sheet.dismiss()
                showProgressBar()
                adapter?.clear()
                val result = presenter.searchRandomManga()
                result.onSuccess {
                    openManga(it.mangaId)
                }.onFailure {
                    onAddPageError(Exception("Error opening random manga"))
                }
            }
        }
        sheet.setOnDismissListener {
            filterSheet = null
        }
        sheet.setOnCancelListener {
            filterSheet = null
        }

        sheet.onFollowsClicked = {
            sheet.dismiss()
            if (presenter.source.isLogged().not()) {
                view?.context?.toast("Please login to view follows")
            } else {
                adapter?.clear()
                router.pushController(FollowsController().withFadeTransaction())
            }
        }

        sheet.onLatestChapterClicked = {
            sheet.dismiss()
            adapter?.clear()
            router.pushController(LatestController().withFadeTransaction())
        }

        sheet.show()
    }

    /**
     * Attempts to restart the request with a new genre-filtered query.
     * If the genre name can't be found the filters,
     * the standard searchWithQuery search method is used instead.
     *
     * @param genreName the name of the genre
     */
    fun searchWithGenre(genreName: String, useContains: Boolean = false) {
        presenter.sourceFilters = presenter.source.getFilterList()

        var filterList: FilterList? = null

        filter@ for (sourceFilter in presenter.sourceFilters) {
            if (sourceFilter is Filter.Group<*>) {
                for (filter in sourceFilter.state) {
                    if (filter is Filter<*> &&
                        if (useContains) {
                            filter.name.contains(genreName, true)
                        } else {
                            filter.name.equals(genreName, true)
                        }
                    ) {
                        when (filter) {
                            is Filter.TriState -> filter.state = 1
                            is Filter.CheckBox -> filter.state = true
                            else -> break
                        }
                        filterList = presenter.sourceFilters
                        break@filter
                    }
                }
            } else if (sourceFilter is Filter.Select<*>) {
                val index = sourceFilter.values.filterIsInstance<String>()
                    .indexOfFirst {
                        if (useContains) {
                            it.contains(genreName, true)
                        } else {
                            it.equals(genreName, true)
                        }
                    }

                if (index != -1) {
                    sourceFilter.state = index
                    filterList = presenter.sourceFilters
                    break
                }
            }
        }

        if (filterList != null) {
            filterSheet?.setFilters(presenter.filterItems)

            showProgressBar()

            adapter?.clear()
            presenter.restartPager("", filterList)
        } else {
            if (!useContains) {
                searchWithGenre(genreName, true)
                return
            }
            searchWithQuery(genreName)
        }
    }

    private fun openInWebView(dex: Boolean = true) {
        val intent = if (dex) {
            val source = presenter.source as? HttpSource ?: return
            val activity = activity ?: return
            WebViewActivity.newIntent(
                activity,
                source.id,
                source.baseUrl,
                presenter.source.name,
            )
        } else {
            val source = presenter.sourceManager.getMergeSource() as? HttpSource ?: return
            val activity = activity ?: return
            WebViewActivity.newIntent(
                activity,
                source.id,
                source.baseUrl,
                source.name,
            )
        }

        startActivity(intent)
    }

    /**
     * Restarts the request with a new query.
     *
     * @param newQuery the new query.
     */
    private fun searchWithQuery(newQuery: String) {
        // If text didn't change, do nothing
        if (presenter.query == newQuery) {
            return
        }

        showProgressBar()
        adapter?.clear()

        presenter.restartPager(newQuery, presenter.sourceFilters)
    }

    fun goDirectlyForDeepLink(manga: Manga) {
        router.replaceTopController(MangaDetailController(manga.id!!).withFadeTransaction())
    }

    /**
     * Called from the presenter when the network request is received.
     *
     * @param page the current page.
     * @param mangaList the list of manga of the page.
     */
    fun onAddPage(page: Int, mangaList: List<BrowseSourceItem>) {
        val adapter = adapter ?: return
        hideProgressBar()
        if (page == 1) {
            adapter.clear()
            resetProgressItem()
        }
        adapter.onLoadMoreComplete(mangaList)
        if (isControllerVisible) {
            activityBinding?.appBar?.lockYPos = false
        }
    }

    /**
     * Called from the presenter when the network request fails.
     *
     * @param error the error received.
     */
    open fun onAddPageError(error: Throwable) {
        XLog.e(error)
        val adapter = adapter ?: return
        adapter.onLoadMoreComplete(null)
        hideProgressBar()

        snack?.dismiss()

        val message = getErrorMessage(error)
        val retryAction = View.OnClickListener {
            // If not the first page, show bottom binding.progress bar.
            if (adapter.mainItemCount > 0 && progressItem != null) {
                adapter.addScrollableFooterWithDelay(progressItem!!, 0, true)
            } else {
                showProgressBar()
            }
            presenter.requestNext()
        }

        if (adapter.isEmpty) {
            val actions = emptyList<EmptyView.Action>().toMutableList()

            actions += EmptyView.Action(R.string.retry, retryAction)
            actions += EmptyView.Action(
                R.string.open_in_webview,
                View.OnClickListener { openInWebView() },
            )

            binding.emptyView.show(
                CommunityMaterial.Icon.cmd_compass_off,
                message,
                actions,
            )
        } else {
            snack = binding.sourceLayout.snack(message, Snackbar.LENGTH_INDEFINITE) {
                setAction(R.string.retry, retryAction)
            }
        }
    }

    private fun getErrorMessage(error: Throwable): String {
        if (error is NoResultsException) {
            return activity!!.getString(R.string.no_results_found)
        }

        return when {
            error.message == null -> ""
            error.message!!.startsWith("HTTP error") -> "${error.message}: ${activity!!.getString(R.string.check_site_in_web)}"
            else -> error.message!!
        }
    }

    /**
     * Sets a new binding.progress item and reenables the scroll listener.
     */
    private fun resetProgressItem() {
        progressItem = ProgressItem()
        adapter?.endlessTargetCount = 0
        adapter?.setEndlessScrollListener(this, progressItem!!)
    }

    /**
     * Called by the adapter when scrolled near the bottom.
     */
    override fun onLoadMore(lastPosition: Int, currentPage: Int) {
        if (presenter.hasNextPage()) {
            presenter.requestNext()
        } else {
            adapter?.onLoadMoreComplete(null)
            adapter?.endlessTargetCount = 1
        }
    }

    override fun noMoreLoad(newItemsSize: Int) {
    }

    /**
     * Called from the presenter when a manga is initialized.
     *
     * @param manga the manga initialized
     */
    fun onMangaInitialized(manga: Manga) {
        getHolder(manga)?.setImage(manga)
    }

    /**
     * Swaps the current display mode.
     */
    fun swapDisplayMode() {
        val view = view ?: return
        val adapter = adapter ?: return

        val isListMode = !presenter.prefs.browseAsList().get()
        presenter.prefs.browseAsList().set(isListMode)
        listOf(activityBinding?.toolbar?.menu, activityBinding?.searchToolbar?.menu).forEach {
            updateDisplayMenuItem(it, isListMode)
        }
        setupRecycler(view)
        // Initialize manga if not on a metered connection
        if (!view.context.connectivityManager.isActiveNetworkMetered) {
            val mangaList = (0 until adapter.itemCount).mapNotNull {
                (adapter.getItem(it) as? BrowseSourceItem)?.manga
            }
            presenter.initializeMangaList(mangaList)
        }
    }

    /**
     * Toggle if our library is already seen
     */
    fun swapLibraryVisibility() {
        val showLibraryManga = !presenter.prefs.browseShowLibrary().get()
        presenter.prefs.browseShowLibrary().set(showLibraryManga)
        listOf(activityBinding?.toolbar?.menu, activityBinding?.searchToolbar?.menu).forEach {
            updateDisplayMenuItem(it, null, showLibraryManga)
        }
        // Initialize manga if not on a metered connection
        presenter.restartPager()
    }

    /**
     * Returns the view holder for the given manga.
     *
     * @param manga the manga to find.
     * @return the holder of the manga or null if it's not bound.
     */
    private fun getHolder(manga: Manga): BrowseSourceHolder? {
        val adapter = adapter ?: return null

        adapter.allBoundViewHolders.forEach { holder ->
            val item = adapter.getItem(holder.flexibleAdapterPosition) as? BrowseSourceItem
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as BrowseSourceHolder
            }
        }

        return null
    }

    /**
     * Shows the binding.progress bar.
     */
    private fun showProgressBar() {
        binding.emptyView.isVisible = false
        binding.progress.isVisible = true
        snack?.dismiss()
        snack = null
    }

    /**
     * Hides active binding.progress bars.
     */
    private fun hideProgressBar() {
        binding.emptyView.isVisible = false
        binding.progress.isVisible = false
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onItemClick(view: View?, position: Int): Boolean {
        val item = adapter?.getItem(position) as? BrowseSourceItem ?: return false
        openManga(item.manga)
        return false
    }

    /**
     * opens a manga
     */
    private fun openManga(manga: Manga) {
        openManga(manga.id!!)
    }

    /**
     * opens a manga
     */
    private fun openManga(mangaId: Long) {
        router.pushController(MangaDetailController(mangaId).withFadeTransaction())
    }

    /**
     * Called when a manga is long clicked.
     *
     * Adds the manga to the default category if none is set it shows a list of categories for the user to put the manga
     * in, the list consists of the default category plus the user's categories. The default category is preselected on
     * new manga, and on already favorited manga the manga's categories are preselected.
     *
     * @param position the position of the element clicked.
     */
    override fun onItemLongClick(position: Int) {
        val manga = (adapter?.getItem(position) as? BrowseSourceItem?)?.manga ?: return
        val view = view ?: return
        val activity = activity ?: return
        snack?.dismiss()
        snack = manga.addOrRemoveToFavorites(
            presenter.db,
            preferences,
            view,
            activity,
            onMangaAdded = {
                adapter?.notifyItemChanged(position)
                snack = view.snack(R.string.added_to_library)
            },
            onMangaMoved = { adapter?.notifyItemChanged(position) },
            onMangaDeleted = { presenter.confirmDeletion(manga) },
        )
        if (snack?.duration == Snackbar.LENGTH_INDEFINITE) {
            (activity as? MainActivity)?.setUndoSnackBar(snack)
        }
    }

    fun updateFab(windowInsets: WindowInsets? = null) {
        val view = view ?: return
        val insets = windowInsets ?: view.rootWindowInsets
        binding.fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = max(
                (
                    activityBinding!!.bottomNav?.height
                        ?: 0
                    ) - (activityBinding!!.bottomNav?.translationY ?: 0f).toInt(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    insets?.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())?.bottom
                        ?: 0
                } else {
                    insets?.systemWindowInsetBottom ?: 0
                },
            ) + 16.dpToPx
        }
        binding.fab.isVisible =
            presenter.sourceFilters.isNotEmpty() && presenter.shouldHideFab.not()
    }

    companion object {
        const val APPLY_INSET = "applyInset"
        const val DEEP_LINK = "deepLink"
        const val FOLLOWS = "follows"
        const val MANGA_ID = "mangaId"

        const val SEARCH_QUERY_KEY = "searchQuery"
    }
}
