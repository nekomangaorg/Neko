package eu.kanade.tachiyomi.ui.library

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.f2prateek.rx.preferences.Preference
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.DownloadServiceListener
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.download.DownloadController
import eu.kanade.tachiyomi.ui.library.filter.SortFilterBottomSheet
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaChaptersController
import eu.kanade.tachiyomi.ui.migration.MigrationInterface
import eu.kanade.tachiyomi.ui.migration.manga.design.PreMigrationController
import eu.kanade.tachiyomi.ui.migration.manga.process.MigrationListController
import eu.kanade.tachiyomi.ui.migration.manga.process.MigrationProcedureConfig
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.applyWindowInsetsForController
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.snack
import kotlinx.android.synthetic.main.filter_bottom_sheet.*
import kotlinx.android.synthetic.main.library_controller.*
import kotlinx.android.synthetic.main.main_activity.*
import rx.Subscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class LibraryController(
        bundle: Bundle? = null,
        protected val preferences: PreferencesHelper = Injekt.get()
) : BaseController(bundle), TabbedController,
        ActionMode.Callback,
        ChangeMangaCategoriesDialog.Listener,
        MigrationInterface,
        DownloadServiceListener,
        LibraryServiceListener {

    /**
     * Position of the active category.
     */
    protected var activeCategory: Int = preferences.lastUsedCategory().getOrDefault()

    /**
     * Action mode for selections.
     */
    private var actionMode: ActionMode? = null

    /**
     * Library search query.
     */
    protected var query = ""

    var customQuery = ""

    /**
     * Currently selected mangas.
     */
    val selectedMangas = mutableSetOf<Manga>()

    /**
     * Current mangas to move.
     */
    private var migratingMangas = mutableSetOf<Manga>()

    /**
     * Relay to notify the UI of selection updates.
     */
    val selectionRelay: PublishRelay<LibrarySelectionEvent> = PublishRelay.create()

    /**
     * Relay to notify search query changes.
     */
    val searchRelay: BehaviorRelay<String> = BehaviorRelay.create()

    /**
     * Relay to notify the library's viewpager for updates.
     */
    val libraryMangaRelay: BehaviorRelay<LibraryMangaEvent> = BehaviorRelay.create()

    /**
     * Relay to notify the library's viewpager to select all manga
     */
    val selectAllRelay: PublishRelay<Int> = PublishRelay.create()

    /**
     * Relay to notify the library's viewpager to reotagnize all
     */
    val reorganizeRelay: PublishRelay<Pair<Int, Int>> = PublishRelay.create()

    val stopRefreshRelay: PublishRelay<Boolean> = PublishRelay.create()

    protected var phoneLandscape = false

    /**
     * Number of manga per row in grid mode.
     */
    var mangaPerRow = 0
        private set

    /**
     * Adapter of the view pager.
     */
    private var pagerAdapter: LibraryAdapter? = null

    /**
     * Drawer listener to allow swipe only for closing the drawer.
     */
    protected var tabsVisibilityRelay: BehaviorRelay<Boolean> = BehaviorRelay.create(false)

    private var tabsVisibilitySubscription: Subscription? = null

    private var observeLater:Boolean = false

    var snack: Snackbar? = null

    lateinit var presenter:LibraryPresenter
        private set

    protected var justStarted = true

    var libraryLayout:Int = preferences.libraryLayout().getOrDefault()

    private var usePager: Boolean = !preferences.libraryAsSingleList().getOrDefault()

    open fun contentView():View = pager_layout

    init {
        setHasOptionsMenu(true)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_library)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.library_controller, container, false)
    }


    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        view.applyWindowInsetsForController()
        mangaPerRow = getColumnsPreferenceForCurrentOrientation().getOrDefault()
        if (!::presenter.isInitialized)
            presenter = LibraryPresenter(this)

        layoutView(view)

        if (selectedMangas.isNotEmpty()) {
            createActionModeIfNeeded()
        }

        //bottom_sheet.onCreate(pager_layout)
        bottom_sheet.onCreate(contentView())

        bottom_sheet.onGroupClicked = {
            when (it) {
                SortFilterBottomSheet.ACTION_REFRESH -> onRefresh()
                SortFilterBottomSheet.ACTION_FILTER -> onFilterChanged()
                SortFilterBottomSheet.ACTION_SORT -> onSortChanged()
                SortFilterBottomSheet.ACTION_DISPLAY -> reattachAdapter()
                SortFilterBottomSheet.ACTION_DOWNLOAD_BADGE -> presenter.requestDownloadBadgesUpdate()
                SortFilterBottomSheet.ACTION_UNREAD_BADGE -> presenter.requestUnreadBadgesUpdate()
                SortFilterBottomSheet.ACTION_CAT_SORT -> onCatSortChanged()
            }
        }

        fab.setOnClickListener {
            router.pushController(DownloadController().withFadeTransaction())
        }

        if (presenter.isDownloading()) {
            fab.scaleY = 1f
            fab.scaleX = 1f
            fab.isClickable = true
            fab.isFocusable = true
        }

        val config = resources?.configuration
        phoneLandscape = (config?.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            (config.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK)) <
            Configuration.SCREENLAYOUT_SIZE_LARGE)

        presenter.onRestore()
        val library = presenter.getAllManga()
        if (library != null)  presenter.updateViewBlocking()
        else {
            contentView().alpha = 0f
            presenter.getLibraryBlocking()
        }
    }


    open fun layoutView(view: View) {
        pagerAdapter = LibraryAdapter(this)
        library_pager.adapter = pagerAdapter
        library_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                preferences.lastUsedCategory().set(position)
                activeCategory = position
            }

            override fun onPageScrolled(
                position: Int, positionOffset: Float, positionOffsetPixels: Int
            ) {
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            //if (library_pager != null)
                //activity?.tabs?.setupWithViewPager(library_pager)
            presenter.getLibrary()
            DownloadService.addListener(this)
            DownloadService.callListeners()
            LibraryUpdateService.setListener(this)
        }
    }

    override fun onChangeEnded(
        changeHandler: ControllerChangeHandler,
        changeType: ControllerChangeType
    ) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter) {
            if (customQuery.isNotEmpty()) {
                query = customQuery
                ((activity as MainActivity).toolbar.menu.findItem(
                    R.id.action_search
                )?.actionView as? SearchView)?.setQuery(
                    customQuery, true
                )
            }
            customQuery = ""
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (observeLater && ::presenter.isInitialized) {
            presenter.getLibrary()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        observeLater = true
        if (::presenter.isInitialized)
            presenter.onDestroy()
    }

    override fun onDestroy() {
        if (::presenter.isInitialized)
            presenter.onDestroy()
        super.onDestroy()
    }

    override fun onDestroyView(view: View) {
        pagerAdapter?.onDestroy()
        DownloadService.removeListener(this)
        LibraryUpdateService.removeListener(this)
        pagerAdapter = null
        actionMode = null
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = null
        super.onDestroyView(view)
    }

    override fun downloadStatusChanged(downloading: Boolean) {
        launchUI {
            val scale = if (downloading) 1f else 0f
            val fab = fab ?: return@launchUI
            fab.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
            fab.isClickable = downloading
            fab.isFocusable = downloading
            bottom_sheet?.adjustFiltersMargin(downloading)
        }
    }

    override fun onUpdateManga(manga: LibraryManga) {
        if (manga.id != null) presenter.updateManga(manga)
        else stopRefreshRelay.call(true)
    }

    override fun onDetach(view: View) {
        destroyActionModeIfNeeded()
        snack?.dismiss()
        snack = null
        super.onDetach(view)
    }

    override fun configureTabs(tabs: TabLayout) {
       /* with(tabs) {
            tabGravity = TabLayout.GRAVITY_CENTER
            tabMode = TabLayout.MODE_SCROLLABLE
        }
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = tabsVisibilityRelay.subscribe { visible ->
            val tabAnimator = (activity as? MainActivity)?.tabAnimator ?: return@subscribe
            if (visible) {
                tabAnimator.expand()
            } else if (!visible) {
                tabAnimator.collapse()
            }
        }*/
    }

    override fun cleanupTabs(tabs: TabLayout) {
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = null
    }

    open fun onNextLibraryUpdate(mangaMap: List<LibraryItem>, freshStart: Boolean = false) { }

    fun onNextLibraryUpdate(categories: List<Category>, mangaMap: Map<Int, List<LibraryItem>>,
        freshStart: Boolean = false) {
        val view = view ?: return
        val adapter = pagerAdapter ?: return

        // Show empty view if needed
        if (mangaMap.isNotEmpty()) {
            empty_view.hide()
        } else {
            empty_view.show(R.drawable.ic_book_black_128dp, R.string.information_empty_library)
        }

        // Get the current active category.
        val activeCat = if (adapter.categories.isNotEmpty())
            library_pager.currentItem
        else
            activeCategory

        categories.find { it.id == 0 }?.let {
            it.name = resources?.getString(
                if (categories.size == 1) R.string.pref_category_library
                else R.string.default_columns
            ) ?: "Default"
        }
        // Set the categories
        adapter.categories = categories

        // Restore active category.
        library_pager.setCurrentItem(activeCat, false)

        //tabsVisibilityRelay.call(categories.size > 1)

        libraryMangaRelay.call(LibraryMangaEvent(mangaMap))

        view.post {
            //if (isAttached) {
              //  activity?.tabs?.setScrollPosition(library_pager.currentItem, 0f, true)
            //}
        }

        if (!freshStart && justStarted) {
            if (!freshStart) {
                justStarted = false
                if (pager_layout.alpha == 0f) pager_layout.animate().alpha(1f).setDuration(500).start()
            }
        }
        // Delay the scroll position to allow the view to be properly measured.

        // Send the manga map to child fragments after the adapter is updated.
    }

    /**
     * Returns a preference for the number of manga per row based on the current orientation.
     *
     * @return the preference.
     */
    private fun getColumnsPreferenceForCurrentOrientation(): Preference<Int> {
        return if (resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT)
            preferences.portraitColumns()
        else
            preferences.landscapeColumns()
    }

    /**
     * Called when a filter is changed.
     */
    private fun onFilterChanged() {
        activity?.invalidateOptionsMenu()
        presenter.requestFilterUpdate()
        destroyActionModeIfNeeded()
    }

    private fun onRefresh() {
        activity?.invalidateOptionsMenu()
        presenter.getLibrary()
        destroyActionModeIfNeeded()
    }

    /**
     * Called when the sorting mode is changed.
     */
    private fun onSortChanged() {
        presenter.requestSortUpdate()
        destroyActionModeIfNeeded()
    }

    open fun onCatSortChanged(id: Int? = null) {
        val catId = (id ?: pagerAdapter?.categories?.getOrNull(library_pager.currentItem)?.id)
                ?: return
         presenter.requestCatSortUpdate(catId)
    }

    /**
     * Reattaches the adapter to the view pager to recreate fragments
     */
    open fun reattachAdapter() {
        val adapter = pagerAdapter ?: return

        val position = library_pager.currentItem

        adapter.recycle = false
        library_pager.adapter = adapter
        library_pager.currentItem = position
        adapter.recycle = true
    }

    /**
     * Creates the action mode if it's not created already.
     */
    fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)
            val view = activity?.window?.currentFocus ?: return
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as?
            InputMethodManager ?: return
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * Destroys the action mode.
     */
    protected fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = resources?.getString(R.string.search_hint)

        searchItem.collapseActionView()
        if (query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }

        // Mutate the filter icon because it needs to be tinted and the resource is shared.
        menu.findItem(R.id.action_library_filter).icon.mutate()

        setOnQueryTextChangeListener(searchView) {
            onSearch(it)
        }
        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })
    }

    open fun onSearch(query: String?): Boolean {
        this.query = query ?: ""
        searchRelay.call(query)
        return true
    }

    open fun search(query: String) {
        this.customQuery = query
    }

    override fun handleRootBack(): Boolean {
        val sheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        if (sheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED &&
            sheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return true
        }
        return false
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val navView = bottom_sheet ?: return

        val filterItem = menu.findItem(R.id.action_library_filter)

        // Tint icon if there's a filter active
        val filterColor = if (navView.hasActiveFilters()) Color.rgb(255, 238, 7)
        else activity?.getResourceColor(R.attr.actionBarTintColor) ?: Color.WHITE
        DrawableCompat.setTint(filterItem.icon, filterColor)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_library_filter -> {
                if (bottom_sheet.sheetBehavior?.isHideable == true &&
                    bottom_sheet.sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED)
                    bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                else if (bottom_sheet.sheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED
                    && bottom_sheet.sheetBehavior?.skipCollapsed == false)
                    bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                else bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            }
            R.id.action_library_display -> {
                DisplayBottomSheet(this).show()
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    fun showFiltersBottomSheet() {
        if (bottom_sheet.sheetBehavior?.state == BottomSheetBehavior.STATE_HIDDEN)
            bottom_sheet.sheetBehavior?.state =
                if (bottom_sheet.sheetBehavior?.skipCollapsed == false)
                    BottomSheetBehavior.STATE_COLLAPSED
                else  BottomSheetBehavior.STATE_EXPANDED
    }


    /**
     * Invalidates the action mode, forcing it to refresh its content.
     */
    fun invalidateActionMode() {
        actionMode?.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.library_selection, menu)
        val selectItem = menu.findItem(R.id.action_select_all)
        selectItem.isVisible = !preferences.libraryAsSingleList().getOrDefault()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = selectedMangas.size
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = resources?.getString(R.string.label_selected, count)
            if (preferences.librarySortingMode().getOrDefault() == LibrarySort.DRAG_AND_DROP) {
                val catId = (selectedMangas.first() as? LibraryManga)?.category
                val sameCat = /*(adapter?.categories?.getOrNull(library_pager.currentItem)?.id
                    == catId) &&*/ selectedMangas.all { (it as? LibraryManga)?.category == catId }
                menu.findItem(R.id.action_move_manga).isVisible = sameCat
            }
            else menu.findItem(R.id.action_move_manga).isVisible = false
        }
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_move_to_category -> showChangeMangaCategoriesDialog()
            R.id.action_delete -> {
                MaterialDialog(activity!!)
                    .message(R.string.confirm_manga_deletion)
                    .positiveButton(R.string.action_remove) {
                        deleteMangasFromLibrary()
                    }
                    .negativeButton(android.R.string.no)
                    .show()
            }
            R.id.action_select_all -> {
                pagerAdapter?.categories?.getOrNull(library_pager.currentItem)?.id?.let {
                    selectAllRelay.call(it)
                }
            }
            R.id.action_migrate -> {
                val skipPre = preferences.skipPreMigration().getOrDefault()
                router.pushController(
                    if (skipPre) {
                        MigrationListController.create(
                            MigrationProcedureConfig(
                                selectedMangas.mapNotNull { it.id },null)
                        )
                    }
                    else {
                        PreMigrationController.create( selectedMangas.mapNotNull { it.id } )
                    }
                   .withFadeTransaction().tag(if (skipPre) MigrationListController.TAG else null))
                destroyActionModeIfNeeded()
            }
            /*R.id.action_to_top, R.id.action_to_bottom -> {
                adapter?.categories?.getOrNull(library_pager.currentItem)?.id?.let {
                    reorganizeRelay.call(it to if (item.itemId == R.id.action_to_top) -1 else -2)
                }
                destroyActionModeIfNeeded()
            }*/
            else -> return false
        }
        return true
    }

    override fun migrateManga(prevManga: Manga, manga: Manga, replace: Boolean): Manga? {
        if (manga.id != prevManga.id) {
            presenter.migrateManga(prevManga, manga, replace = replace)
        }
        val nextManga = migratingMangas.firstOrNull() ?: return null
        migratingMangas.remove(nextManga)
        return nextManga
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        // Clear all the manga selections and notify child views.
        selectedMangas.clear()
        selectionRelay.call(LibrarySelectionEvent.Cleared())
        actionMode = null
    }

    fun openManga(manga: Manga, startY: Float?) {
        router.pushController(MangaChaptersController(manga).withFadeTransaction())
       // router.pushController(MangaController(manga, startY).withFadeTransaction())
    }

    /**
     * Sets the selection for a given manga.
     *
     * @param manga the manga whose selection has changed.
     * @param selected whether it's now selected or not.
     */
    open fun setSelection(manga: Manga, selected: Boolean) {
        if (selected) {
            if (selectedMangas.add(manga)) {
                selectionRelay.call(LibrarySelectionEvent.Selected(manga))
            }
        } else {
            if (selectedMangas.remove(manga)) {
                selectionRelay.call(LibrarySelectionEvent.Unselected(manga))
            }
        }
    }

    /**
     * Move the selected manga to a list of categories.
     */
    private fun showChangeMangaCategoriesDialog() {
        // Create a copy of selected manga
        val mangas = selectedMangas.toList()

        // Hide the default category because it has a different behavior than the ones from db.
        val categories = presenter.allCategories.filter { it.id != 0 }

        // Get indexes of the common categories to preselect.
        val commonCategoriesIndexes = presenter.getCommonCategories(mangas)
                .map { categories.indexOf(it) }
                .toTypedArray()

        ChangeMangaCategoriesDialog(this, mangas, categories, commonCategoriesIndexes)
                .showDialog(router)
    }

    private fun deleteMangasFromLibrary() {
        val mangas = selectedMangas.toList()
        presenter.removeMangaFromLibrary(mangas)
        destroyActionModeIfNeeded()
        snack?.dismiss()
        snack = snackbar_layout?.snack(activity?.getString(R.string.manga_removed_library) ?: "", Snackbar
            .LENGTH_INDEFINITE)  {
            var undoing = false
            setAction(R.string.action_undo) {
                presenter.addMangas(mangas)
                undoing = true
            }
            addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (!undoing)
                        presenter.confirmDeletion(mangas)
                }
            })
        }
        (activity as? MainActivity)?.setUndoSnackBar(snack)
    }

    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        presenter.moveMangasToCategories(categories, mangas)
        destroyActionModeIfNeeded()
    }

    /// Method for the category view
    fun startReading(manga: Manga) {
        val activity = activity ?: return
        val chapter = presenter.getFirstUnread(manga) ?: return
        val intent = ReaderActivity.newIntent(activity, manga, chapter)
        destroyActionModeIfNeeded()
        startActivity(intent)
    }
}