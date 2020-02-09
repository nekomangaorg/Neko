package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.category.CategoryAdapter
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.plusAssign
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.doOnApplyWindowInsets
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.filter_bottom_sheet.*
import kotlinx.android.synthetic.main.library_category.view.*
import kotlinx.android.synthetic.main.library_controller.*
import kotlinx.coroutines.delay
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.injectLazy

/**
 * Fragment containing the library manga for a certain category.
 */
class LibraryCategoryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        CoordinatorLayout(context, attrs),
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        FlexibleAdapter.OnItemMoveListener,
    CategoryAdapter.OnItemReleaseListener {

    /**
     * Preferences.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * The fragment containing this view.
     */
    private lateinit var controller: LibraryController

    private val db: DatabaseHelper by injectLazy()
    /**
     * Category for this view.
     */
    lateinit var category: Category
        private set

    /**
     * Recycler view of the list of manga.
     */
    private lateinit var recycler: RecyclerView

    /**
     * Adapter to hold the manga in this category.
     */
    private lateinit var adapter: LibraryCategoryAdapter

    /**
     * Subscriptions while the view is bound.
     */
    private var subscriptions = CompositeSubscription()

    private var lastTouchUpY = 0f

    private var lastClickPosition = -1

    fun onCreate(controller: LibraryController) {
        this.controller = controller

        recycler = if (preferences.libraryAsList().getOrDefault()) {
            (swipe_refresh.inflate(R.layout.library_list_recycler) as RecyclerView).apply {
                layoutManager = LinearLayoutManager(context)
            }
        } else {
            (swipe_refresh.inflate(R.layout.library_grid_recycler) as AutofitRecyclerView).apply {
                spanCount = controller.mangaPerRow
            }
        }

        adapter = LibraryCategoryAdapter(this)

        recycler.setHasFixedSize(true)
        recycler.adapter = adapter
        swipe_refresh.addView(recycler)
        adapter.fastScroller = fast_scroller


        if (!MainActivity.bottomNav) {
            fast_scroller.addOnScrollStateChangeListener {
                controller.lockFilterBar(it)
            }
        }
        else {
            fast_scroller.setIgnoreTouchesOutsideHandle(false)
        }
        val config = resources?.configuration

        val phoneLandscape = (config?.orientation == Configuration.ORIENTATION_LANDSCAPE &&
            (config.screenLayout.and(Configuration.SCREENLAYOUT_SIZE_MASK)) <
            Configuration.SCREENLAYOUT_SIZE_LARGE)
        if (MainActivity.bottomNav && !phoneLandscape) {
            val height = context.resources.getDimensionPixelSize(R.dimen.rounder_radius) + 5.dpToPx
            recycler.updatePaddingRelative(bottom = height)
        }
        else if (!MainActivity.bottomNav) {
            recycler.doOnApplyWindowInsets { v, insets, padding ->
                v.updatePaddingRelative(bottom = padding.bottom + insets.systemWindowInsetBottom)

                fast_scroller?.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = insets.systemWindowInsetBottom
                }
            }
        }

        // Double the distance required to trigger sync
        swipe_refresh.setDistanceToTriggerSync((2 * 64 * resources.displayMetrics.density).toInt())
        swipe_refresh.setOnRefreshListener {
            val inQueue = LibraryUpdateService.categoryInQueue(category.id)
            controller.snack?.dismiss()
            controller.snack = controller.snackbar_layout.snack(
                resources.getString(
                    when {
                        inQueue -> R.string.category_already_in_queue
                        LibraryUpdateService.isRunning(context) -> R.string.adding_category_to_queue
                        else -> R.string.updating_category_x
                    }, category.name))
            if (!inQueue)
                LibraryUpdateService.start(context, category)
            swipe_refresh.isRefreshing = false
        }
    }

    fun onBind(category: Category) {
        this.category = category

        adapter.mode = if (controller.selectedMangas.isNotEmpty()) {
            SelectableAdapter.Mode.MULTI
        } else {
            SelectableAdapter.Mode.SINGLE
        }
        adapter.isLongPressDragEnabled = canDrag()

        subscriptions += controller.searchRelay
                .doOnNext { adapter.setFilter(it) }
                .skip(1)
                .subscribe { adapter.performFilter() }

        subscriptions += controller.libraryMangaRelay
                .subscribe { onNextLibraryManga(it) }

        subscriptions += controller.selectionRelay
                .subscribe { onSelectionChanged(it) }

        subscriptions += controller.selectAllRelay
            .subscribe {
                if (it == category.id) {
                    adapter.currentItems.forEach { item ->
                        controller.setSelection(item.manga, true)
                    }
                    controller.invalidateActionMode()
                }
            }

        subscriptions += controller.reorganizeRelay
            .subscribe {
                if (it.first == category.id) {
                    if (it.second in -2..-1) {
                        val items = adapter.currentItems.toMutableList()
                        val mangas = controller.selectedMangas
                        val selectedManga = items.filter { item -> item.manga in mangas }
                        items.removeAll(selectedManga)
                        if (it.second == -1) items.addAll(0, selectedManga)
                        else items.addAll(selectedManga)
                        adapter.setItems(items)
                        adapter.notifyDataSetChanged()
                        saveDragSort()
                    }
                    else {
                        category.mangaSort = ('a' + (it.second - 1))
                        if (category.id == 0)
                            preferences.defaultMangaOrder().set(category.mangaSort.toString())
                        else
                            db.insertCategory(category).asRxObservable().subscribe()
                        controller.enableReorderItems(category)
                    }
                }
            }
    }

    fun canDrag(): Boolean {
        val sortingMode = preferences.librarySortingMode().getOrDefault()
        val filterOff = preferences.filterCompleted().getOrDefault() +
            preferences.filterTracked().getOrDefault() +
            preferences.filterUnread().getOrDefault() +
            preferences.filterCompleted().getOrDefault() == 0 &&
            !preferences.hideCategories().getOrDefault()
        return sortingMode == LibrarySort.DRAG_AND_DROP && filterOff &&
            adapter.mode != SelectableAdapter.Mode.MULTI
    }

    fun onRecycle() {
        adapter.setItems(emptyList())
        adapter.clearSelection()
        unsubscribe()
    }

    fun unsubscribe() {
        subscriptions.clear()
    }

    /**
     * Subscribe to [LibraryMangaEvent]. When an event is received, it updates the content of the
     * adapter.
     *
     * @param event the event received.
     */
    fun onNextLibraryManga(event: LibraryMangaEvent) {
        // Get the manga list for this category.
        adapter.isLongPressDragEnabled = canDrag()
        val mangaForCategory = event.getMangaForCategory(category).orEmpty()

        // Update the category with its manga.
       // if (!justDraggedAndDropped)
            adapter.setItems(mangaForCategory)
       // else
         //   justDraggedAndDropped = false

        swipe_refresh.isEnabled = !preferences.hideCategories().getOrDefault()

        if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            controller.selectedMangas.forEach { manga ->
                val position = adapter.indexOf(manga)
                if (position != -1 && !adapter.isSelected(position)) {
                    adapter.toggleSelection(position)
                    (recycler.findViewHolderForItemId(manga.id!!) as? LibraryHolder)?.toggleActivation()
                }
            }
        }
    }

    /**
     * Subscribe to [LibrarySelectionEvent]. When an event is received, it updates the selection
     * depending on the type of event received.
     *
     * @param event the selection event received.
     */
    private fun onSelectionChanged(event: LibrarySelectionEvent) {
        when (event) {
            is LibrarySelectionEvent.Selected -> {
                if (adapter.mode != SelectableAdapter.Mode.MULTI) {
                    adapter.mode = SelectableAdapter.Mode.MULTI
                }
                launchUI {
                    delay(100)
                    adapter.isLongPressDragEnabled = false
                }
                findAndToggleSelection(event.manga)
            }
            is LibrarySelectionEvent.Unselected -> {
                findAndToggleSelection(event.manga)
                if (adapter.indexOf(event.manga) != -1) lastClickPosition = -1
                if (controller.selectedMangas.isEmpty()) {
                    adapter.mode = SelectableAdapter.Mode.SINGLE
                    adapter.isLongPressDragEnabled = canDrag()
                }
            }
            is LibrarySelectionEvent.Cleared -> {
                adapter.mode = SelectableAdapter.Mode.SINGLE
                adapter.clearSelection()
                adapter.notifyDataSetChanged()
                lastClickPosition = -1
                adapter.isLongPressDragEnabled = canDrag()
            }
        }
    }

    /**
     * Toggles the selection for the given manga and updates the view if needed.
     *
     * @param manga the manga to toggle.
     */
    private fun findAndToggleSelection(manga: Manga) {
        val position = adapter.indexOf(manga)
        if (position != -1) {
            adapter.toggleSelection(position)
            (recycler.findViewHolderForItemId(manga.id!!) as? LibraryHolder)?.toggleActivation()
        }
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onItemClick(view: View?, position: Int): Boolean {
        // If the action mode is created and the position is valid, toggle the selection.
        val item = adapter.getItem(position) ?: return false
        return if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            lastClickPosition = position
            toggleSelection(position)
            true
        } else {
            openManga(item.manga, lastTouchUpY)
            false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_UP -> lastTouchUpY = ev.y
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Called when a manga is long clicked.
     *
     * @param position the position of the element clicked.
     */
    override fun onItemLongClick(position: Int) {
        controller.createActionModeIfNeeded()
        when {
            lastClickPosition == -1 -> setSelection(position)
            lastClickPosition > position -> for (i in position until lastClickPosition)
                setSelection(i)
            lastClickPosition < position -> for (i in lastClickPosition + 1..position)
                setSelection(i)
            else -> setSelection(position)
        }
        lastClickPosition = position
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {

    }

    override fun onItemReleased(position: Int) {
        if (adapter.selectedItemCount == 0) saveDragSort()
    }

    private fun saveDragSort() {
        val mangaIds = adapter.currentItems.mapNotNull { it.manga.id }
        category.mangaSort = null
        category.mangaOrder = mangaIds
        if (category.id == 0)
            preferences.defaultMangaOrder().set(mangaIds.joinToString("/"))
        else
            db.insertCategory(category).asRxObservable().subscribe()
        //justDraggedAndDropped = true
        controller.onCatSortChanged(category.id)
        controller.enableReorderItems(category)
    }
    override fun shouldMoveItem(fromPosition: Int, toPosition: Int): Boolean {
        if (adapter.selectedItemCount > 1)
            return false
        if (adapter.isSelected(fromPosition))
            toggleSelection(fromPosition)
        return true
    }

    override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        val position = viewHolder?.adapterPosition ?: return
        if (actionState == 2) onItemLongClick(position)
    }

    /**
     * Opens a manga.
     *
     * @param manga the manga to open.
     */
    private fun openManga(manga: Manga, startY:Float?) {
        controller.openManga(manga, startY)
    }

    /**
     * Tells the presenter to toggle the selection for the given position.
     *
     * @param position the position to toggle.
     */
    private fun toggleSelection(position: Int) {
        val item = adapter.getItem(position) ?: return

        controller.setSelection(item.manga, !adapter.isSelected(position))
        controller.invalidateActionMode()
    }


    /**
     * Tells the presenter to set the selection for the given position.
     *
     * @param position the position to toggle.
     */
    private fun setSelection(position: Int) {
        val item = adapter.getItem(position) ?: return

        controller.setSelection(item.manga, true)
        controller.invalidateActionMode()
    }

}
