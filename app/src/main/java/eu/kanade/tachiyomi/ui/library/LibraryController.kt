package eu.kanade.tachiyomi.ui.library

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
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import eu.kanade.tachiyomi.ui.base.controller.SecondaryDrawerController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.download.DownloadController
import eu.kanade.tachiyomi.ui.library.filter.SortFilterBottomSheet
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.migration.MigrationController
import eu.kanade.tachiyomi.ui.migration.MigrationInterface
import eu.kanade.tachiyomi.ui.migration.manga.design.PreMigrationController
import eu.kanade.tachiyomi.ui.migration.manga.process.MigrationListController
import eu.kanade.tachiyomi.ui.migration.manga.process.MigrationProcedureConfig
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.util.view.marginBottom
import eu.kanade.tachiyomi.util.view.marginTop
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.widget.ExtendedNavigationView
import kotlinx.android.synthetic.main.filter_bottom_sheet.*
import kotlinx.android.synthetic.main.library_controller.*
import kotlinx.android.synthetic.main.main_activity.*
import rx.Subscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryController(
        bundle: Bundle? = null,
        private val preferences: PreferencesHelper = Injekt.get()
) : BaseController(bundle),
        TabbedController,
        SecondaryDrawerController,
        ActionMode.Callback,
        ChangeMangaCategoriesDialog.Listener,
        MigrationInterface,
        DownloadServiceListener,
        LibraryServiceListener {

    /**
     * Position of the active category.
     */
    var activeCategory: Int = preferences.lastUsedCategory().getOrDefault()
        private set

    /**
     * Action mode for selections.
     */
    private var actionMode: ActionMode? = null

    /**
     * Library search query.
     */
    private var query = ""

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

    /**
     * Number of manga per row in grid mode.
     */
    var mangaPerRow = 0
        private set

    /**
     * Adapter of the view pager.
     */
    private var adapter: LibraryAdapter? = null

    /**
     * Navigation view containing filter/sort/display items.
     */
    private var navView: LibraryNavigationView? = null

    /**
     * Drawer listener to allow swipe only for closing the drawer.
     */
    private var tabsVisibilityRelay: BehaviorRelay<Boolean> = BehaviorRelay.create(false)

    private var tabsVisibilitySubscription: Subscription? = null

    var snack: Snackbar? = null

    private var reorderMenuItem:MenuItem? = null

    private var presenter = LibraryPresenter(this)

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

        adapter = LibraryAdapter(this)
        library_pager.adapter = adapter
        library_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                preferences.lastUsedCategory().set(position)
                activeCategory = position
            }

            override fun onPageScrollStateChanged(state: Int) { }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) { }
        })

        library_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                enableReorderItems(position)
                bottom_sheet.lastCategory = adapter?.categories?.getOrNull(position)
                bottom_sheet.updateTitle()
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) { }

            override fun onPageScrollStateChanged(state: Int) { }
        })

        mangaPerRow = getColumnsPreferenceForCurrentOrientation().getOrDefault()

        if (selectedMangas.isNotEmpty()) {
            createActionModeIfNeeded()
        }

        if (MainActivity.bottomNav) {
            bottom_sheet.onCreate(pager_layout)

            bottom_sheet.onGroupClicked = {
                when (it) {
                    SortFilterBottomSheet.ACTION_REFRESH -> onRefresh()
                    SortFilterBottomSheet.ACTION_FILTER -> onFilterChanged()
                    SortFilterBottomSheet.ACTION_SORT -> onSortChanged()
                    SortFilterBottomSheet.ACTION_DISPLAY -> reattachAdapter()
                    SortFilterBottomSheet.ACTION_DOWNLOAD_BADGE ->
                        presenter.requestDownloadBadgesUpdate()
                    SortFilterBottomSheet.ACTION_UNREAD_BADGE -> presenter.requestUnreadBadgesUpdate()
                    SortFilterBottomSheet.ACTION_CAT_SORT -> onCatSortChanged()
                }
            }

            fab.setOnClickListener {
                router.pushController(DownloadController().withFadeTransaction())
            }
        }
        else {
            bottom_sheet.gone()
            shadow.gone()
            shadow2.gone()
        }
    }

    fun enableReorderItems(category: Category) {
        if (MainActivity.bottomNav) return
        adapter?.categories?.getOrNull(library_pager.currentItem)?.mangaSort = category.mangaSort
        enableReorderItems(sortType = category.mangaSort)
    }

    private fun enableReorderItems(position: Int? = null, sortType: Char? = null) {
        if (MainActivity.bottomNav) return
        val pos = position ?: library_pager.currentItem
        val orderOfCat = sortType ?: adapter?.categories?.getOrNull(pos)?.mangaSort
        if (reorderMenuItem?.isVisible != true) return
        val subMenu = reorderMenuItem?.subMenu ?: return
        if (orderOfCat != null) {
            subMenu.setGroupCheckable(R.id.reorder_group, true, true)
            when (orderOfCat) {
                'a', 'b' -> subMenu.findItem(R.id.action_alpha_asc)?.isChecked = true
                'c', 'd' -> subMenu.findItem(R.id.action_update_asc)?.isChecked = true
                'e', 'f' -> subMenu.findItem(R.id.action_unread)?.isChecked = true
                'g', 'h' -> subMenu.findItem(R.id.action_last_read)?.isChecked = true
            }
            subMenu.findItem(R.id.action_reverse)?.isVisible = true
        }
        else {
            subMenu.findItem(R.id.action_reverse)?.isVisible = false
            subMenu.setGroupCheckable(R.id.reorder_group, false, false)
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            activity?.tabs?.setupWithViewPager(library_pager)
            presenter.getLibrary()
            DownloadService.addListener(this)
            DownloadService.callListeners()
            LibraryUpdateService.setListener(this)
        }
    }

    override fun onDestroyView(view: View) {
        adapter?.onDestroy()
        DownloadService.removeListener(this)
        LibraryUpdateService.removeListener()
        adapter = null
        actionMode = null
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = null
        super.onDestroyView(view)
    }

    override fun downloadStatusChanged(downloading: Boolean) {
        launchUI {
            val scale = if (downloading) 1f else 0f
            fab.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
            fab.isClickable = downloading
            fab.isFocusable = downloading
            bottom_sheet.adjustTitleMargin(downloading)
        }
    }

    override fun onUpdateManga(manga: LibraryManga) {
        presenter.updateManga(manga)
    }

    override fun onDetach(view: View) {
        destroyActionModeIfNeeded()
        snack?.dismiss()
        snack = null
        super.onDetach(view)
    }

    override fun createSecondaryDrawer(drawer: DrawerLayout): ViewGroup? {
        if (MainActivity.bottomNav) return null
        val view = drawer.inflate(R.layout.library_drawer) as LibraryNavigationView
        navView = view
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)

        navView?.onGroupClicked = { group, item ->
            when (group) {
                is LibraryNavigationView.FilterGroup -> onFilterChanged(item)
                is LibraryNavigationView.SortGroup -> onSortChanged()
                is LibraryNavigationView.DisplayGroup -> reattachAdapter()
                is LibraryNavigationView.BadgeGroup -> onDownloadBadgeChanged()
            }
        }

        drawer.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        val statusScrim = view.findViewById(R.id.status_bar_scrim) as View
        statusScrim.setOnApplyWindowInsetsListener(HeightTopWindowInsetsListener)
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.recycler.updatePaddingRelative(
                bottom = view.recycler.marginBottom + insets.systemWindowInsetBottom,
                top = view.recycler.marginTop + insets.systemWindowInsetTop
            )
            insets
        }
        return view
    }

    override fun cleanupSecondaryDrawer(drawer: DrawerLayout) {
        navView = null
    }

    override fun configureTabs(tabs: TabLayout) {
        with(tabs) {
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
        }
    }

    override fun cleanupTabs(tabs: TabLayout) {
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = null
    }

    fun onNextLibraryUpdate(categories: List<Category>, mangaMap: Map<Int, List<LibraryItem>>) {
        val view = view ?: return
        val adapter = adapter ?: return

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
            it.name = resources?.getString(R.string.default_columns) ?: "Default"
        }
        // Set the categories
        adapter.categories = categories

        // Restore active category.
        library_pager.setCurrentItem(activeCat, false)

        bottom_sheet.lastCategory = adapter.categories.getOrNull(activeCat)
        bottom_sheet.updateTitle()

        tabsVisibilityRelay.call(categories.size > 1)

        // Delay the scroll position to allow the view to be properly measured.
        view.post {
            if (isAttached) {
                activity?.tabs?.setScrollPosition(library_pager.currentItem, 0f, true)
            }
        }

        // Send the manga map to child fragments after the adapter is updated.
        libraryMangaRelay.call(LibraryMangaEvent(mangaMap))
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

    private fun onRefresh() {
        if (!MainActivity.bottomNav) activity?.invalidateOptionsMenu()
        presenter.requestFullUpdate()
    }

    /**
     * Called when a filter is changed.
     */
    private fun onFilterChanged(item: ExtendedNavigationView.Item? = null) {
        if (item is ExtendedNavigationView.Item.MultiStateGroup && item.resTitle == R.string.categories) {
            if (!MainActivity.bottomNav) activity?.invalidateOptionsMenu()
            presenter.requestFullUpdate()
            return
        }
        presenter.requestFilterUpdate()
        destroyActionModeIfNeeded()
        if (!MainActivity.bottomNav) activity?.invalidateOptionsMenu()
    }

    private fun onDownloadBadgeChanged() {
        presenter.requestDownloadBadgesUpdate()
    }

    /**
     * Called when the sorting mode is changed.
     */
    private fun onSortChanged() {
        if (!MainActivity.bottomNav) activity?.invalidateOptionsMenu()
        presenter.requestSortUpdate()
    }

    fun onCatSortChanged(id: Int? = null) {
        val catId = id ?: adapter?.categories?.getOrNull(library_pager.currentItem)?.id ?: return
        presenter.requestCatSortUpdate(catId)
    }

    /**
     * Reattaches the adapter to the view pager to recreate fragments
     */
    private fun reattachAdapter() {
        val adapter = adapter ?: return

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
    fun destroyActionModeIfNeeded() {
        actionMode?.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library, menu)

        val reorganizeItem = menu.findItem(R.id.action_reorganize)
        reorganizeItem.isVisible =
            !MainActivity.bottomNav
            preferences.librarySortingMode().getOrDefault() == LibrarySort.DRAG_AND_DROP &&
                !preferences.hideCategories().getOrDefault()

        val config = resources?.configuration

        val phoneLandscape = (config?.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            (config.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK)) <
            Configuration.SCREENLAYOUT_SIZE_LARGE)
        menu.findItem(R.id.action_library_filter).isVisible = !MainActivity.bottomNav || phoneLandscape
        reorderMenuItem = reorganizeItem
        enableReorderItems()

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

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (router.backstack.lastOrNull()?.controller() == this@LibraryController) {
                    query = newText ?: ""
                    searchRelay.call(query)
                }
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }
        })
        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })
    }

    fun search(query:String) {
        this.query = query
    }

    override fun handleBack(): Boolean {
        val sheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return true
        }
        return super.handleBack()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val navView = navView ?: return

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
                if (MainActivity.bottomNav) {
                    if (bottom_sheet.sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED)
                    bottom_sheet.sheetBehavior?.state =
                        BottomSheetBehavior.STATE_EXPANDED
                    else
                        bottom_sheet.sheetBehavior?.state =
                            BottomSheetBehavior.STATE_COLLAPSED
                }
                else navView?.let { activity?.drawer?.openDrawer(GravityCompat.END) }
            }
            R.id.action_edit_categories -> {
                router.pushController(CategoryController().withFadeTransaction())
            }
            R.id.action_source_migration -> {
                router.pushController(MigrationController().withFadeTransaction())
            }
            R.id.action_alpha_asc -> reOrder(0)
            R.id.action_update_asc -> reOrder(1)
            R.id.action_unread -> reOrder(2)
            R.id.action_last_read -> reOrder(3)
            R.id.action_reverse -> reOrder(-1)
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun reOrder(type: Int) {
        val modType = if (type == -1) {
            val t = (adapter?.categories?.getOrNull(library_pager.currentItem)?.mangaSort
                ?.minus('a') ?: 0) + 1
            if (t % 2 != 0) t + 1
            else t - 1
        }
        else 2 * type + 1
        adapter?.categories?.getOrNull(library_pager.currentItem)?.id?.let {
            reorganizeRelay.call(it to modType)
            onSortChanged()
        }
    }

    /**
     * Invalidates the action mode, forcing it to refresh its content.
     */
    fun invalidateActionMode() {
        actionMode?.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.library_selection, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val count = selectedMangas.size
        if (count == 0) {
            // Destroy action mode if there are no items selected.
            destroyActionModeIfNeeded()
        } else {
            mode.title = resources?.getString(R.string.label_selected, count)
            menu.findItem(R.id.action_hide_title)?.isVisible =
                !preferences.libraryAsList().getOrDefault()
            if (!preferences.libraryAsList().getOrDefault()) {
                val showAll = (selectedMangas.all { (it as? LibraryManga)?.hide_title == true })
                menu.findItem(R.id.action_hide_title)?.title = activity?.getString(
                    if (showAll) R.string.action_show_title else R.string.action_hide_title
                )
            }
            if (preferences.librarySortingMode().getOrDefault() == LibrarySort.DRAG_AND_DROP) {
                val catId = (selectedMangas.first() as? LibraryManga)?.category
                val sameCat = (adapter?.categories?.getOrNull(library_pager.currentItem)?.id
                    == catId) && selectedMangas.all { (it as? LibraryManga)?.category == catId }
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
                adapter?.categories?.getOrNull(library_pager.currentItem)?.id?.let {
                    selectAllRelay.call(it)
                }
            }
            R.id.action_migrate -> {
                router.pushController(
                    if (preferences.skipPreMigration().getOrDefault()) {
                        MigrationListController.create(
                            MigrationProcedureConfig(
                                selectedMangas.mapNotNull { it.id },null)
                        )
                    }
                    else {
                        PreMigrationController.create( selectedMangas.mapNotNull { it.id } )
                    }
                   .withFadeTransaction())
                destroyActionModeIfNeeded()
            }
            R.id.action_hide_title -> {
                val showAll = (selectedMangas.filter { (it as? LibraryManga)?.hide_title == true }
                    ).size == selectedMangas.size
                presenter.hideShowTitle(selectedMangas.toList(), !showAll)
                destroyActionModeIfNeeded()
            }
            R.id.action_to_top, R.id.action_to_bottom -> {
                adapter?.categories?.getOrNull(library_pager.currentItem)?.id?.let {
                    reorganizeRelay.call(it to if (item.itemId == R.id.action_to_top) -1 else -2)
                }
                destroyActionModeIfNeeded()
            }
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
        router.pushController(MangaController(manga, startY).withFadeTransaction())
    }

    /**
     * Sets the selection for a given manga.
     *
     * @param manga the manga whose selection has changed.
     * @param selected whether it's now selected or not.
     */
    fun setSelection(manga: Manga, selected: Boolean) {
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

    fun lockFilterBar(lock: Boolean) {
        val drawer = (navView?.parent  as? DrawerLayout) ?: return
        if (lock) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            drawer.closeDrawers()
        } else {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            drawer.visible()
        }
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
}

object HeightTopWindowInsetsListener : View.OnApplyWindowInsetsListener {
    override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
        val topInset = insets.systemWindowInsetTop
        v.setPadding(0,topInset,0,0)
        if (v.layoutParams.height != topInset) {
            v.layoutParams.height = topInset
            v.requestLayout()
        }
        return insets
    }
}