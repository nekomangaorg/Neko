package eu.kanade.tachiyomi.ui.library

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
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
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager.widget.ViewPager
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.f2prateek.rx.preferences.Preference
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxbinding.support.v4.view.pageSelections
import com.jakewharton.rxbinding.support.v7.widget.queryTextChanges
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.SecondaryDrawerController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.migration.MigrationController
import eu.kanade.tachiyomi.ui.migration.MigrationInterface
import eu.kanade.tachiyomi.ui.migration.manga.design.PreMigrationController
import eu.kanade.tachiyomi.ui.migration.manga.process.MigrationListController
import eu.kanade.tachiyomi.ui.migration.manga.process.MigrationProcedureConfig
import eu.kanade.tachiyomi.util.doOnApplyWindowInsets
import eu.kanade.tachiyomi.util.inflate
import eu.kanade.tachiyomi.util.marginBottom
import eu.kanade.tachiyomi.util.marginTop
import eu.kanade.tachiyomi.util.snack
import eu.kanade.tachiyomi.util.toast
import eu.kanade.tachiyomi.util.updatePaddingRelative
import kotlinx.android.synthetic.main.library_controller.*
import kotlinx.android.synthetic.main.main_activity.*
import rx.Subscription
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException

class LibraryController(
        bundle: Bundle? = null,
        private val preferences: PreferencesHelper = Injekt.get()
) : NucleusController<LibraryPresenter>(bundle),
        TabbedController,
        SecondaryDrawerController,
        ActionMode.Callback,
        ChangeMangaCategoriesDialog.Listener,
        MigrationInterface {

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

    private var selectedCoverManga: Manga? = null

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
    private var drawerListener: DrawerLayout.DrawerListener? = null

    private var tabsVisibilityRelay: BehaviorRelay<Boolean> = BehaviorRelay.create(false)

    private var tabsVisibilitySubscription: Subscription? = null

    private var searchViewSubscription: Subscription? = null

    var snack: Snackbar? = null

    private var reorderMenuItem:MenuItem? = null

    init {
        setHasOptionsMenu(true)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    override fun getTitle(): String? {
        return resources?.getString(R.string.label_library)
    }

    override fun createPresenter(): LibraryPresenter {
        return LibraryPresenter()
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.library_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = LibraryAdapter(this)
        library_pager.adapter = adapter
        library_pager.pageSelections().skip(1).subscribeUntilDestroy {
            preferences.lastUsedCategory().set(it)
            activeCategory = it
        }

        library_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                enableReorderItems(position)
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) { }

            override fun onPageScrollStateChanged(state: Int) { }
        })

        getColumnsPreferenceForCurrentOrientation().asObservable()
                .doOnNext { mangaPerRow = it }
                .skip(1)
                // Set again the adapter to recalculate the covers height
                .subscribeUntilDestroy { reattachAdapter() }

        if (selectedMangas.isNotEmpty()) {
            createActionModeIfNeeded()
        }
    }

    fun enableReorderItems(category: Category) {
        adapter?.categories?.getOrNull(library_pager.currentItem)?.mangaSort = category.mangaSort
        enableReorderItems(sortType = category.mangaSort)
    }

    private fun enableReorderItems(position: Int? = null, sortType: Char? = null) {
        val pos = position ?: library_pager.currentItem
        val orderOfCat = sortType ?: adapter?.categories?.getOrNull(pos)?.mangaSort
        if (reorderMenuItem?.isVisible != true) return
        val subMenu = reorderMenuItem?.subMenu ?: return
        if (orderOfCat != null) {
            subMenu.setGroupCheckable(R.id.reorder_group, true, true)
            when (orderOfCat) {
                'a' -> subMenu.findItem(R.id.action_alpha_asc)?.isChecked = true
                'b' -> subMenu.findItem(R.id.action_alpha_dsc)?.isChecked = true
                'c' -> subMenu.findItem(R.id.action_update_asc)?.isChecked = true
                'd' -> subMenu.findItem(R.id.action_update_dsc)?.isChecked = true
                'e' -> subMenu.findItem(R.id.action_unread)?.isChecked = true
            }
        }
        else {
            subMenu.setGroupCheckable(R.id.reorder_group, false, false)
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            activity?.tabs?.setupWithViewPager(library_pager)
            presenter.subscribeLibrary()
        }
    }

    override fun onDestroyView(view: View) {
        adapter?.onDestroy()
        adapter = null
        actionMode = null
        tabsVisibilitySubscription?.unsubscribe()
        tabsVisibilitySubscription = null
        super.onDestroyView(view)
    }

    override fun onDetach(view: View) {
        destroyActionModeIfNeeded()
        snack?.dismiss()
        snack = null
        super.onDetach(view)
    }

    override fun createSecondaryDrawer(drawer: DrawerLayout): ViewGroup {
        val view = drawer.inflate(R.layout.library_drawer) as LibraryNavigationView
        navView = view
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)

        navView?.onGroupClicked = { group ->
            when (group) {
                is LibraryNavigationView.FilterGroup -> onFilterChanged()
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
        view.doOnApplyWindowInsets { _, insets, _ ->
            view.recycler.updatePaddingRelative(
                bottom = view.recycler.marginBottom + insets.systemWindowInsetBottom,
                top = view.recycler.marginTop + insets.systemWindowInsetTop
            )
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
            val tabAnimator = (activity as? MainActivity)?.tabAnimator
            if (visible) {
                tabAnimator?.expand()
            } else {
                tabAnimator?.collapse()
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

    /**
     * Called when a filter is changed.
     */
    private fun onFilterChanged() {
        presenter.requestFilterUpdate()
        destroyActionModeIfNeeded()
        activity?.invalidateOptionsMenu()
    }

    private fun onDownloadBadgeChanged() {
        presenter.requestDownloadBadgesUpdate()
    }

    /**
     * Called when the sorting mode is changed.
     */
    fun onSortChanged() {
        activity?.invalidateOptionsMenu()
        presenter.requestSortUpdate()
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
        reorganizeItem.isVisible = preferences.librarySortingMode().getOrDefault() == LibrarySort.DRAG_AND_DROP
        reorderMenuItem = reorganizeItem
        enableReorderItems()

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = resources?.getString(R.string.search_hint)

        searchItem.collapseActionView()
        if (!query.isEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }

        // Mutate the filter icon because it needs to be tinted and the resource is shared.
        menu.findItem(R.id.action_filter).icon.mutate()

        searchViewSubscription?.unsubscribe()
        searchViewSubscription = searchView.queryTextChanges()
                // Ignore events if this controller isn't at the top
                .filter { router.backstack.lastOrNull()?.controller() == this }
                .subscribeUntilDestroy {
                    query = it.toString()
                    searchRelay.call(query)
                }

        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })
    }

    fun search(query:String) {
        this.query = query
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val navView = navView ?: return

        val filterItem = menu.findItem(R.id.action_filter)

        // Tint icon if there's a filter active
        val filterColor = if (navView.hasActiveFilters()) Color.rgb(255, 238, 7) else Color.WHITE
        DrawableCompat.setTint(filterItem.icon, filterColor)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_filter -> {
                navView?.let { activity?.drawer?.openDrawer(GravityCompat.END) }
            }
            R.id.action_update_library -> {
                activity?.let { LibraryUpdateService.start(it) }
                snack = view?.snack(R.string.updating_library)
            }
            R.id.action_edit_categories -> {
                router.pushController(CategoryController().withFadeTransaction())
            }
            R.id.action_source_migration -> {
                router.pushController(MigrationController().withFadeTransaction())
            }
            R.id.action_alpha_asc -> reOrder(1)
            R.id.action_alpha_dsc -> reOrder(2)
            R.id.action_update_asc -> reOrder(3)
            R.id.action_update_dsc -> reOrder(4)
            R.id.action_unread -> reOrder(5)
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun reOrder(type: Int) {
        adapter?.categories?.getOrNull(library_pager.currentItem)?.id?.let {
            reorganizeRelay.call(it to type)
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
            menu.findItem(R.id.action_edit_cover)?.isVisible = count == 1
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
            R.id.action_edit_cover -> {
                changeSelectedCover()
                destroyActionModeIfNeeded()
            }
            R.id.action_move_to_category -> showChangeMangaCategoriesDialog()
            R.id.action_delete -> deleteMangasFromLibrary()
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
        // Notify the presenter a manga is being opened.
        presenter.onOpenManga()

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
        val categories = presenter.categories.filter { it.id != 0 }

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
        snack = view?.snack(activity?.getString(R.string.manga_removed_library) ?: "", Snackbar.LENGTH_INDEFINITE)  {
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

    /**
     * Changes the cover for the selected manga.
     */
    private fun changeSelectedCover() {
        val manga = selectedMangas.firstOrNull() ?: return
        selectedCoverManga = manga

        if (manga.favorite) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent,
                    resources?.getString(R.string.file_select_cover)), REQUEST_IMAGE_OPEN)
        } else {
            activity?.toast(R.string.notification_first_add_to_library)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_OPEN) {
            if (data == null || resultCode != Activity.RESULT_OK) return
            val activity = activity ?: return
            val manga = selectedCoverManga ?: return

            try {
                // Get the file's input stream from the incoming Intent
                activity.contentResolver.openInputStream(data.data ?: Uri.EMPTY).use {
                    // Update cover to selected file, show error if something went wrong
                    if (it != null && presenter.editCoverWithStream(it, manga)) {
                        // TODO refresh cover
                    } else {
                        activity.toast(R.string.notification_cover_update_failed)
                    }
                }
            } catch (error: IOException) {
                activity.toast(R.string.notification_cover_update_failed)
                Timber.e(error)
            }
            selectedCoverManga = null
        }
    }

    private companion object {
        /**
         * Key to change the cover of a manga in [onActivityResult].
         */
        const val REQUEST_IMAGE_OPEN = 101
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