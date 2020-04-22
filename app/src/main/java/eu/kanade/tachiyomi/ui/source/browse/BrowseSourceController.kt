package eu.kanade.tachiyomi.ui.source.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding.support.v7.widget.queryTextChangeEvents
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.library.ChangeMangaCategoriesDialog
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.system.connectivityManager
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.applyWindowInsetsForRootController
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.util.view.visibleIf
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.browse_source_controller.*
import kotlinx.android.synthetic.main.main_activity.*
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit

/**
 * Controller to manage the catalogues available in the app.
 */
open class BrowseSourceController(bundle: Bundle) :
        NucleusController<BrowseSourcePresenter>(bundle),
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        FlexibleAdapter.EndlessScrollListener,
        ChangeMangaCategoriesDialog.Listener {

    constructor(
        source: Source,
        searchQuery: String? = null,
        applyInset: Boolean = true
    ) : this(Bundle().apply {
        putLong(SOURCE_ID_KEY, source.id)
        putBoolean(APPLY_INSET, applyInset)

        if (searchQuery != null)
            putString(SEARCH_QUERY_KEY, searchQuery)
    })

    constructor(source: Source, applyInset: Boolean = true) : this(Bundle().apply {
        putLong(SOURCE_ID_KEY, source.id)
        putBoolean(APPLY_INSET, applyInset)
    })

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
     * Subscription for the search view.
     */
    private var searchViewSubscription: Subscription? = null

    /**
     * Endless loading item.
     */
    private var progressItem: ProgressItem? = null

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        if (bundle?.getBoolean(APPLY_INSET) == true) {
            return presenter.source.name
        } else {
            return "Similar"
        }
    }

    override fun createPresenter(): BrowseSourcePresenter {
        return BrowseSourcePresenter(args.getLong(SOURCE_ID_KEY))
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.browse_source_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        if (bundle?.getBoolean(APPLY_INSET) == true) {
            view.applyWindowInsetsForRootController(activity!!.bottom_nav)
        }

        // Initialize adapter, scroll listener and recycler views
        adapter = FlexibleAdapter(null, this)
        setupRecycler(view)

        fab.visibleIf(presenter.sourceFilters.isNotEmpty())
        fab.setOnClickListener { showFilters() }
        progress?.visible()
    }

    override fun onDestroyView(view: View) {
        searchViewSubscription?.unsubscribe()
        searchViewSubscription = null
        adapter = null
        snack = null
        recycler = null
        super.onDestroyView(view)
    }

    private fun setupRecycler(view: View) {
        var oldPosition = RecyclerView.NO_POSITION
        val oldRecycler = catalogue_view?.getChildAt(1)
        if (oldRecycler is RecyclerView) {
            oldPosition = (oldRecycler.layoutManager as androidx.recyclerview.widget.LinearLayoutManager).findFirstVisibleItemPosition()
            oldRecycler.adapter = null

            catalogue_view?.removeView(oldRecycler)
        }

        val recycler = if (presenter.isListMode) {
            RecyclerView(view.context).apply {
                id = R.id.recycler
                layoutManager = LinearLayoutManager(context)
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        } else {
            (catalogue_view.inflate(R.layout.manga_recycler_autofit) as AutofitRecyclerView).apply {
                columnWidth = when (preferences.gridSize().getOrDefault()) {
                    0 -> 1f
                    2 -> 1.66f
                    else -> 1.25f
                }

                (layoutManager as androidx.recyclerview.widget.GridLayoutManager).spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
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

        scrollViewWith(recycler, true) { insets ->
            fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.systemWindowInsetBottom + 16.dpToPx
            }
        }

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0)
                    fab.extend()
                else
                    fab.shrink()
            }
        })

        catalogue_view.addView(recycler, 1)
        if (oldPosition != RecyclerView.NO_POSITION) {
            recycler.layoutManager?.scrollToPosition(oldPosition)
        }
        this.recycler = recycler
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (bundle?.getBoolean(APPLY_INSET) == true) {
            (activity as? MainActivity)?.hideNavigationIcon()
        } else {
            (activity as? MainActivity)?.showNavigationArrow()
        }
        inflater.inflate(R.menu.catalogue_list, menu)

        // Initialize search menu
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        val query = presenter.query
        if (!query.isBlank()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }

        val searchEventsObservable = searchView.queryTextChangeEvents()
            .skip(1)
            .filter { router.backstack.lastOrNull()?.controller() == this@BrowseSourceController }
            .share()
        val writingObservable = searchEventsObservable
            .filter { !it.isSubmitted }
            .debounce(1250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
        val submitObservable = searchEventsObservable
            .filter { it.isSubmitted }

        searchViewSubscription?.unsubscribe()
        searchViewSubscription = Observable.merge(writingObservable, submitObservable)
            .map { it.queryText().toString() }
            .subscribeUntilDestroy { searchWithQuery(it) }

        searchItem.fixExpand(
            onExpand = { invalidateMenuOnExpand() },
            onCollapse = {
                searchWithQuery("")
                true
            }
        )

        // Show next display mode
        menu.findItem(R.id.action_display_mode).apply {
            val icon = if (presenter.isListMode)
                R.drawable.ic_view_module_white_24dp
            else
                R.drawable.ic_view_list_white_24dp
            setIcon(icon)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val isHttpSource = presenter.source is HttpSource
        menu.findItem(R.id.action_open_in_web_view).isVisible = isHttpSource
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_display_mode -> swapDisplayMode()
            R.id.action_open_in_web_view -> openInWebView()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showFilters() {
        val sheet = SourceSearchSheet(activity!!)
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
                            ((presenter.sourceFilters[i] as Filter.Group<*>).state[j] as
                                Filter<*>).state) {
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
                val allDefault = presenter.sourceFilters == presenter.source.getFilterList()
                showProgressBar()
                adapter?.clear()
                presenter.setSourceFilter(if (allDefault) FilterList() else presenter.sourceFilters)
            }
        }

        sheet.onResetClicked = {
            presenter.appliedFilters = FilterList()
            val newFilters = presenter.source.getFilterList()
            presenter.sourceFilters = newFilters
            sheet.setFilters(presenter.filterItems)
        }

        sheet.onRandomClicked = {
            sheet.dismiss()
            showProgressBar()
            adapter?.clear()
            presenter.searchRandomManga()
        }
        sheet.onFollowsClicked = {
            sheet.dismiss()
            adapter?.clear()
            presenter.restartPager("", followsPager = true)
        }

        sheet.show()
    }

    private fun openInWebView() {
        val source = presenter.source as? HttpSource ?: return
        val activity = activity ?: return
        val intent = WebViewActivity.newIntent(activity, source.id, source.baseUrl, presenter
            .source.name)
        startActivity(intent)
    }

    /**
     * Restarts the request with a new query.
     *
     * @param newQuery the new query.
     */
    private fun searchWithQuery(newQuery: String) {
        // If text didn't change, do nothing
        if (presenter.query == newQuery)
            return

        showProgressBar()
        adapter?.clear()

        presenter.restartPager(newQuery)
    }

    /**
     * Called from the presenter when the network request is received.
     *
     * @param page the current page.
     * @param mangas the list of manga of the page.
     */
    fun onAddPage(page: Int, mangas: List<BrowseSourceItem>) {
        val adapter = adapter ?: return
        hideProgressBar()
        if (page == 1) {
            adapter.clear()
            resetProgressItem()
        }
        adapter.onLoadMoreComplete(mangas)
    }

    /**
     * Called from the presenter when the network request fails.
     *
     * @param error the error received.
     */
    fun onAddPageError(error: Throwable) {
        Timber.e(error)
        val adapter = adapter ?: return
        adapter.onLoadMoreComplete(null)
        hideProgressBar()

        snack?.dismiss()
        val message = if (error is NoResultsException) catalogue_view.context.getString(R.string.no_results_found) else (error.message ?: "")
        snack = source_layout?.snack(message, Snackbar.LENGTH_INDEFINITE) {
            setAction(R.string.retry) {
                // If not the first page, show bottom progress bar.
                if (adapter.mainItemCount > 0) {
                    val item = progressItem ?: return@setAction
                    adapter.addScrollableFooterWithDelay(item, 0, true)
                } else {
                    showProgressBar()
                presenter.requestNext()
                }
            }
        }
    }

    /**
     * Sets a new progress item and reenables the scroll listener.
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

        presenter.swapDisplayMode()
        val isListMode = presenter.isListMode
        activity?.invalidateOptionsMenu()
        setupRecycler(view)
        if (!isListMode || !view.context.connectivityManager.isActiveNetworkMetered) {
            // Initialize mangas if going to grid view or if over wifi when going to list view
            val mangas = (0 until adapter.itemCount).mapNotNull {
                (adapter.getItem(it) as? BrowseSourceItem)?.manga
            }
            presenter.initializeMangas(mangas)
        }
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
            val item = adapter.getItem(holder.adapterPosition) as? BrowseSourceItem
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as BrowseSourceHolder
            }
        }

        return null
    }

    /**
     * Shows the progress bar.
     */
    private fun showProgressBar() {
        progress?.visible()
        snack?.dismiss()
        snack = null
    }

    /**
     * Hides active progress bars.
     */
    private fun hideProgressBar() {
        progress?.gone()
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onItemClick(view: View?, position: Int): Boolean {
        val item = adapter?.getItem(position) as? BrowseSourceItem ?: return false
        router.pushController(MangaDetailsController(item.manga, true).withFadeTransaction())

        return false
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
        snack?.dismiss()
        if (manga.favorite) {
            presenter.changeMangaFavorite(manga)
            adapter?.notifyItemChanged(position)
            snack = source_layout?.snack(R.string.removed_from_library, Snackbar.LENGTH_INDEFINITE) {
                setAction(R.string.undo) {
                    if (!manga.favorite) addManga(manga, position)
                }
                addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (!manga.favorite) presenter.confirmDeletion(manga)
                    }
                })
            }
            (activity as? MainActivity)?.setUndoSnackBar(snack)
        } else {
            addManga(manga, position)
            snack = source_layout?.snack(R.string.added_to_library)
        }
    }

    private fun addManga(manga: Manga, position: Int) {
        presenter.changeMangaFavorite(manga)
        adapter?.notifyItemChanged(position)

        val categories = presenter.getCategories()
        val defaultCategoryId = preferences.defaultCategory()
        val defaultCategory = categories.find { it.id == defaultCategoryId }
        when {
            defaultCategory != null -> presenter.moveMangaToCategory(manga, defaultCategory)
            defaultCategoryId == 0 || categories.isEmpty() -> // 'Default' or no category
                presenter.moveMangaToCategory(manga, null)
            else -> {
                val ids = presenter.getMangaCategoryIds(manga)
                if (ids.isNullOrEmpty()) {
                    presenter.moveMangaToCategory(manga, null)
                }
                val preselected = ids.mapNotNull { id ->
                    categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
                }.toTypedArray()

                ChangeMangaCategoriesDialog(this, listOf(manga), categories, preselected)
                        .showDialog(router)
            }
        }
    }

    /**
     * Update manga to use selected categories.
     *
     * @param mangas The list of manga to move to categories.
     * @param categories The list of categories where manga will be placed.
     */
    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        val manga = mangas.firstOrNull() ?: return
        presenter.updateMangaCategories(manga, categories)
    }

    protected companion object {
        const val SOURCE_ID_KEY = "sourceId"
        const val SEARCH_QUERY_KEY = "searchQuery"
        const val APPLY_INSET = "applyInset"
    }
}
