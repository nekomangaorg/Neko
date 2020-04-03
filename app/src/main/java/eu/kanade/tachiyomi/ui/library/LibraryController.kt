package eu.kanade.tachiyomi.ui.library

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.library.LibraryServiceListener
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.library.filter.FilterBottomSheet
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.OnTouchEventInterface
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.main.SpinnerTitleInterface
import eu.kanade.tachiyomi.ui.main.SwipeGestureInterface
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.migration.manga.design.PreMigrationController
import eu.kanade.tachiyomi.ui.migration.manga.process.MigrationListController
import eu.kanade.tachiyomi.ui.migration.manga.process.MigrationProcedureConfig
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.applyWindowInsetsForRootController
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import kotlinx.android.synthetic.main.filter_bottom_sheet.*
import kotlinx.android.synthetic.main.library_grid_recycler.*
import kotlinx.android.synthetic.main.library_list_controller.*
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.delay
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign

class LibraryController(
    bundle: Bundle? = null,
    private val preferences: PreferencesHelper = Injekt.get()
) : BaseController(bundle),
    ActionMode.Callback,
    ChangeMangaCategoriesDialog.Listener,
    FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener,
    FlexibleAdapter.OnItemMoveListener, LibraryCategoryAdapter.LibraryListener,
    SpinnerTitleInterface, OnTouchEventInterface, SwipeGestureInterface,
    RootSearchInterface, LibraryServiceListener {

    init {
        setHasOptionsMenu(true)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    /**
     * Position of the active category.
     */
    private var activeCategory: Int = preferences.lastUsedCategory().getOrDefault()

    private var justStarted = true

    /**
     * Action mode for selections.
     */
    private var actionMode: ActionMode? = null

    private var libraryLayout: Int = preferences.libraryLayout().getOrDefault()

    /**
     * Library search query.
     */
    private var query = ""

    /**
     * Currently selected mangas.
     */
    private val selectedMangas = mutableSetOf<Manga>()

    private lateinit var adapter: LibraryCategoryAdapter

    private var lastClickPosition = -1

    private var updateScroll = true

    private var lastItemPosition: Int? = null
    private var lastItem: IFlexible<*>? = null

    private var switchingCategories = false
    var scrollDistance = 0f

    lateinit var presenter: LibraryPresenter
        private set

    private var observeLater: Boolean = false

    var snack: Snackbar? = null

    // Horizontal scroll values
    private var startPosX: Float? = null
    private var startPosY: Float? = null
    private var moved = false
    private var lockedRecycler = false
    private var lockedY = false
    private var nextCategory: Int? = null
    private var ogCategory: Int? = null
    private var prevCategory: Int? = null
    private val swipeDistance = 500f
    private var flinging = false
    private var isDragging = false
    private val scrollDistanceTilHidden = 1000.dpToPx

    override fun getTitle(): String? {
        return if (view != null && presenter.categories.size > 1) presenter.categories.find {
            it.order == activeCategory
        }?.name ?: super.getTitle()
        else super.getTitle()
    }

    private var scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val order = getCategoryOrder()
            if (bottom_sheet.canHide()) {
                scrollDistance += abs(dy)
                if (scrollDistance > scrollDistanceTilHidden) {
                    bottom_sheet.hideIfPossible()
                    scrollDistance = 0f
                }
            } else scrollDistance = 0f
            if (order != null && order != activeCategory) {
                preferences.lastUsedCategory().set(order)
                activeCategory = order
                setTitle()
            }
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        view.applyWindowInsetsForRootController(activity!!.bottom_nav)
        if (!::presenter.isInitialized) presenter = LibraryPresenter(this)

        layoutView(view)

        if (selectedMangas.isNotEmpty()) {
            createActionModeIfNeeded()
        }

        bottom_sheet.onCreate(recycler_layout)

        bottom_sheet.onGroupClicked = {
            when (it) {
                FilterBottomSheet.ACTION_REFRESH -> onRefresh()
                FilterBottomSheet.ACTION_FILTER -> onFilterChanged()
                FilterBottomSheet.ACTION_HIDE_FILTER_TIP -> activity?.toast(
                    R.string.hide_filters_tip, Toast.LENGTH_LONG
                )
            }
        }

        // pad the recycler if the filter bottom sheet is visible
        val height = view.context.resources.getDimensionPixelSize(R.dimen.rounder_radius) + 4.dpToPx
        recycler.updatePaddingRelative(bottom = height)

        presenter.onRestore()
        val library = presenter.getAllManga()
        if (library != null) presenter.updateViewBlocking()
        else {
            recycler_layout.alpha = 0f
            presenter.getLibraryBlocking()
        }
    }

    override fun onTouchEvent(event: MotionEvent?) {
        if (event == null) {
            resetScrollingValues()
            resetRecyclerY()
            return
        }
        if (flinging || presenter.categories.size <= 1) return
        if (isDragging) {
            resetScrollingValues()
            resetRecyclerY(false)
            return
        }
        val sheetRect = Rect()
        val recyclerRect = Rect()
        val appBarRect = Rect()
        bottom_sheet.getGlobalVisibleRect(sheetRect)
        view?.getGlobalVisibleRect(recyclerRect)
        activity?.appbar?.getGlobalVisibleRect(appBarRect)

        if (startPosX == null) {
            startPosX = event.rawX
            startPosY = event.rawY
            val position =
                (recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val order = activeCategory
            ogCategory = order
            var newOffsetN = order + 1
            while (adapter.indexOf(newOffsetN) == -1 && presenter.categories.any { it.order == newOffsetN }) {
                newOffsetN += 1
            }
            if (adapter.indexOf(newOffsetN) != -1) nextCategory = newOffsetN

            if (position == 0) prevCategory = null
            else {
                var newOffsetP = order - 1
                while (adapter.indexOf(newOffsetP) == -1 && presenter.categories.any { it.order == newOffsetP }) {
                    newOffsetP -= 1
                }
                if (adapter.indexOf(newOffsetP) != -1) prevCategory = newOffsetP
            }
            return
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            recycler_layout.post {
                if (!flinging) {
                    resetScrollingValues()
                    resetRecyclerY(true)
                }
            }
            return
        }
        if (startPosX != null && startPosY != null && (sheetRect.contains(
                startPosX!!.toInt(),
                startPosY!!.toInt()
            ) || !recyclerRect.contains(
                startPosX!!.toInt(),
                startPosY!!.toInt()
            ) || appBarRect.contains(startPosX!!.toInt(), startPosY!!.toInt()))
        ) {
            return
        }
        if (event.actionMasked != MotionEvent.ACTION_UP && startPosX != null) {
            val distance = abs(event.rawX - startPosX!!)
            val sign = sign(event.rawX - startPosX!!)

            if (lockedY) return

            if (distance > 60 && abs(event.rawY - startPosY!!) <= 30 && !lockedRecycler) {
                swipe_refresh.isEnabled = false
                lockedRecycler = true
                switchingCategories = true
                recycler.suppressLayout(true)
            } else if (!lockedRecycler && abs(event.rawY - startPosY!!) > 30) {
                lockedY = true
                resetRecyclerY()
                return
            }
            if (abs(event.rawY - startPosY!!) <= 30 || recycler.isLayoutSuppressed || lockedRecycler) {

                if ((prevCategory == null && sign > 0) || (nextCategory == null && sign < 0)) {
                    recycler_layout.x = sign * distance.pow(0.6f)
                    recycler_layout.alpha = 1f
                } else if (distance <= swipeDistance * 1.1f) {
                    recycler_layout.x = sign * (distance / (swipeDistance / 3f)).pow(3.5f)
                    recycler_layout.alpha =
                        (1f - (distance - (swipeDistance * 0.1f)) / swipeDistance)
                    if (moved) {
                        scrollToHeader(ogCategory ?: -1)
                        moved = false
                    }
                } else {
                    if (!moved) {
                        scrollToHeader((if (sign <= 0) nextCategory else prevCategory) ?: -1)
                        moved = true
                    }
                    recycler_layout.x = -sign * (max(0f, (swipeDistance * 2 - distance)) /
                        (swipeDistance / 3f)).pow(3.5f)
                    recycler_layout.alpha = ((distance - swipeDistance * 1.1f) / swipeDistance)
                    recycler_layout.alpha = min(1f, recycler_layout.alpha)
                }
            }
        }
    }

    private fun getCategoryOrder(): Int? {
        val position =
            (recycler.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
        var order = when (val item = adapter.getItem(position)) {
            is LibraryHeaderItem -> item.category.order
            is LibraryItem -> presenter.categories.find { it.id == item.manga.category }?.order
            else -> null
        }
        if (order == null) {
            val fPosition =
                (recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            order = when (val item = adapter.getItem(fPosition)) {
                is LibraryHeaderItem -> item.category.order
                is LibraryItem -> presenter.categories.find { it.id == item.manga.category }?.order
                else -> null
            }
        }
        return order
    }

    private fun resetScrollingValues() {
        swipe_refresh.isEnabled = true
        startPosX = null
        startPosY = null
        nextCategory = null
        prevCategory = null
        ogCategory = null
        lockedY = false
    }

    private fun resetRecyclerY(animated: Boolean = false, time: Long = 100) {
        swipe_refresh.isEnabled = true
        moved = false
        lockedRecycler = false
        if (animated) {
            val set = AnimatorSet()
            val translationXAnimator = ValueAnimator.ofFloat(recycler_layout.x, 0f)
            translationXAnimator.duration = time
            translationXAnimator.addUpdateListener { animation ->
                recycler_layout.x = animation.animatedValue as Float
            }

            val translationAlphaAnimator = ValueAnimator.ofFloat(recycler_layout.alpha, 1f)
            translationAlphaAnimator.duration = time
            translationAlphaAnimator.addUpdateListener { animation ->
                recycler_layout.alpha = animation.animatedValue as Float
            }
            set.playTogether(translationXAnimator, translationAlphaAnimator)
            set.start()

            launchUI {
                delay(time)
                if (!lockedRecycler) switchingCategories = false
            }
        } else {
            recycler_layout.x = 0f
            recycler_layout.alpha = 1f
            switchingCategories = false
        }
        recycler.suppressLayout(false)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.library_list_controller, container, false)
    }

    private fun layoutView(view: View) {
        adapter = LibraryCategoryAdapter(this)
        setRecyclerLayout()
        recycler.manager.spanSizeLookup = (object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (libraryLayout == 0) return 1
                val item = this@LibraryController.adapter.getItem(position)
                return if (item is LibraryHeaderItem) recycler.manager.spanCount
                else if (item is LibraryItem && item.manga.isBlank()) recycler.manager.spanCount
                else 1
            }
        })
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter
        adapter.fastScroller = fast_scroller
        recycler.addOnScrollListener(scrollListener)

        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.actionBarTintColor, tv, true)

        scrollViewWith(recycler, swipeRefreshLayout = swipe_refresh) { insets ->
            fast_scroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.systemWindowInsetTop
            }
        }

        swipe_refresh.setOnRefreshListener {
            swipe_refresh.isRefreshing = false
            if (!LibraryUpdateService.isRunning()) {
                when {
                    presenter.allCategories.size <= 1 -> updateLibrary()
                    preferences.updateOnRefresh().getOrDefault() == -1 -> {
                        MaterialDialog(activity!!).title(R.string.what_should_update)
                            .negativeButton(android.R.string.cancel)
                            .listItemsSingleChoice(items = listOf(
                                view.context.getString(
                                    R.string.top_category, presenter.allCategories.first().name
                                ), view.context.getString(
                                    R.string.categories_in_global_update
                                )
                            ), selection = { _, index, _ ->
                                preferences.updateOnRefresh().set(index)
                                when (index) {
                                    0 -> updateLibrary(presenter.allCategories.first())
                                    else -> updateLibrary()
                                }
                            })
                            .positiveButton(R.string.action_update)
                            .show()
                    }
                    else -> {
                        when (preferences.updateOnRefresh().getOrDefault()) {
                            0 -> updateLibrary(presenter.allCategories.first())
                            else -> updateLibrary()
                        }
                    }
                }
            }
        }
    }

    private fun updateLibrary(category: Category? = null) {
        val view = view ?: return
        LibraryUpdateService.start(view.context, category)
        snack = view.snack(R.string.updating_library) {
            anchorView = bottom_sheet
        }
    }

    private fun setRecyclerLayout() {
        if (libraryLayout == 0) {
            recycler.spanCount = 1
            recycler.updatePaddingRelative(start = 0, end = 0)
        } else {
            recycler.columnWidth = (90 + (preferences.gridSize().getOrDefault() * 30)).dpToPx
            recycler.updatePaddingRelative(start = 5.dpToPx, end = 5.dpToPx)
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            if (presenter.categories.size > 1) {
                activity?.toolbar?.showSpinner()
            } else {
                activity?.toolbar?.removeSpinner()
            }
            presenter.getLibrary()
            DownloadService.callListeners()
            LibraryUpdateService.setListener(this)
        }
        if (type == ControllerChangeType.POP_ENTER) bottom_sheet.hideIfPossible()
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (view == null) return
        if (observeLater && ::presenter.isInitialized) {
            presenter.getLibrary()
        }
        resetScrollingValues()
        resetRecyclerY()
    }

    override fun onActivityPaused(activity: Activity) {
        super.onActivityPaused(activity)
        observeLater = true
        if (::presenter.isInitialized) presenter.onDestroy()
    }

    override fun onDestroy() {
        if (::presenter.isInitialized) presenter.onDestroy()
        super.onDestroy()
    }

    override fun onDestroyView(view: View) {
        LibraryUpdateService.removeListener(this)
        actionMode = null
        super.onDestroyView(view)
    }

    fun onNextLibraryUpdate(mangaMap: List<LibraryItem>, freshStart: Boolean) {
        if (view == null) return
        destroyActionModeIfNeeded()
        if (mangaMap.isNotEmpty()) {
            empty_view?.hide()
        } else {
            empty_view?.show(
                R.drawable.ic_book_black_128dp,
                if (bottom_sheet.hasActiveFilters()) R.string.information_empty_library_filtered
                else R.string.information_empty_library
            )
        }
        adapter.setItems(mangaMap)

        val isCurrentController = router?.backstack?.lastOrNull()?.controller() == this

        setTitle()
        updateScroll = false
        if (!freshStart) {
            justStarted = false
            if (recycler_layout.alpha == 0f) recycler_layout.animate().alpha(1f).setDuration(500)
                .start()
        } else if (justStarted) {
            if (freshStart) scrollToHeader(activeCategory)
        } else {
            updateScroll = true
        }
        adapter.isLongPressDragEnabled = canDrag()

        val popupMenu = if (presenter.categories.size > 1 && isCurrentController) {
            activity?.toolbar?.showSpinner()
        } else {
            activity?.toolbar?.removeSpinner()
            null
        }

        presenter.categories.forEach { category ->
            popupMenu?.menu?.add(0, category.order, max(0, category.order), category.name)
        }

        popupMenu?.setOnMenuItemClickListener { item ->
            scrollToHeader(item.itemId)
            true
        }
    }

    private fun scrollToHeader(pos: Int) {
        val headerPosition = adapter.indexOf(pos)
        switchingCategories = true
        if (headerPosition > -1) {
            val appbar = activity?.appbar
            recycler.suppressLayout(true)
            val appbarOffset = if (appbar?.y ?: 0f > -20) 0 else (appbar?.y?.plus(
                    view?.rootWindowInsets?.systemWindowInsetTop ?: 0
                ) ?: 0f).roundToInt() + 30.dpToPx
            (recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                headerPosition, (if (headerPosition == 0) 0 else (-40).dpToPx) + appbarOffset
            )

            /*val headerItem = adapter.getItem(headerPosition) as? LibraryHeaderItem
            if (headerItem != null) {
                setTitle()
            }*/
            recycler.suppressLayout(false)
        }
        launchUI {
            delay(100)
            switchingCategories = false
        }
    }

    private fun onRefresh() {
        activity?.invalidateOptionsMenu()
        presenter.getLibrary()
        destroyActionModeIfNeeded()
    }

    /**
     * Called when a filter is changed.
     */
    private fun onFilterChanged() {
        activity?.invalidateOptionsMenu()
        presenter.requestFilterUpdate()
        destroyActionModeIfNeeded()
    }

    fun reattachAdapter() {
        libraryLayout = preferences.libraryLayout().getOrDefault()
        setRecyclerLayout()
        val position =
            (recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        recycler.adapter = adapter

        (recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
    }

    fun search(query: String?): Boolean {
        this.query = query ?: ""
        adapter.setFilter(query)
        adapter.performFilter()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        selectedMangas.clear()
        actionMode = null
        adapter.mode = SelectableAdapter.Mode.SINGLE
        adapter.clearSelection()
        adapter.notifyDataSetChanged()
        lastClickPosition = -1
        adapter.isLongPressDragEnabled = canDrag()
    }

    private fun setSelection(manga: Manga, selected: Boolean) {
        val currentMode = adapter.mode
        if (selected) {
            if (selectedMangas.add(manga)) {
                val positions = adapter.allIndexOf(manga)
                if (adapter.mode != SelectableAdapter.Mode.MULTI) {
                    adapter.mode = SelectableAdapter.Mode.MULTI
                }
                launchUI {
                    delay(100)
                    adapter.isLongPressDragEnabled = false
                }
                positions.forEach { position ->
                    adapter.addSelection(position)
                    (recycler.findViewHolderForAdapterPosition(position) as? LibraryHolder)?.toggleActivation()
                }
            }
        } else {
            if (selectedMangas.remove(manga)) {
                val positions = adapter.allIndexOf(manga)
                lastClickPosition = -1
                if (selectedMangas.isEmpty()) {
                    adapter.mode = SelectableAdapter.Mode.SINGLE
                    adapter.isLongPressDragEnabled = canDrag()
                }
                positions.forEach { position ->
                    adapter.removeSelection(position)
                    (recycler.findViewHolderForAdapterPosition(position) as? LibraryHolder)?.toggleActivation()
                }
            }
        }
        updateHeaders(currentMode != adapter.mode)
    }

    private fun updateHeaders(changedMode: Boolean = false) {
        val headerPositions = adapter.getHeaderPositions()
        headerPositions.forEach {
            if (changedMode) {
                adapter.notifyItemChanged(it)
            } else {
                (recycler.findViewHolderForAdapterPosition(it) as? LibraryHeaderItem.Holder)?.setSelection()
            }
        }
    }

    override fun startReading(position: Int) {
        if (recyclerIsScrolling()) return
        if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            toggleSelection(position)
            return
        }
        val manga = (adapter.getItem(position) as? LibraryItem)?.manga ?: return
        startReading(manga)
    }

    private fun toggleSelection(position: Int) {
        val item = adapter.getItem(position) as? LibraryItem ?: return
        if (item.manga.isBlank()) return
        setSelection(item.manga, !adapter.isSelected(position))
        invalidateActionMode()
    }

    override fun canDrag(): Boolean {
        val filterOff =
            !bottom_sheet.hasActiveFilters() && !preferences.hideCategories().getOrDefault()
        return filterOff && adapter.mode != SelectableAdapter.Mode.MULTI
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onItemClick(view: View?, position: Int): Boolean {
        if (recyclerIsScrolling()) return false
        val item = adapter.getItem(position) as? LibraryItem ?: return false
        return if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            lastClickPosition = position
            toggleSelection(position)
            false
        } else {
            openManga(item.manga)
            false
        }
    }

    private fun openManga(manga: Manga) = router.pushController(MangaDetailsController(
        manga
    ).withFadeTransaction())

    /**
     * Called when a manga is long clicked.
     *
     * @param position the position of the element clicked.
     */
    override fun onItemLongClick(position: Int) {
        if (recyclerIsScrolling()) return
        if (adapter.getItem(position) is LibraryHeaderItem) return
        createActionModeIfNeeded()
        when {
            lastClickPosition == -1 -> setSelection(position)
            lastClickPosition > position -> for (i in position until lastClickPosition) setSelection(
                i
            )
            lastClickPosition < position -> for (i in lastClickPosition + 1..position) setSelection(
                i
            )
            else -> setSelection(position)
        }
        lastClickPosition = position
    }

    override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        val position = viewHolder?.adapterPosition ?: return
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            isDragging = true
            activity?.appbar?.y = 0f
            if (lastItemPosition != null && position != lastItemPosition && lastItem == adapter.getItem(
                    position
                )
            ) {
                // because for whatever reason you can repeatedly tap on a currently dragging manga
                adapter.removeSelection(position)
                (recycler.findViewHolderForAdapterPosition(position) as? LibraryHolder)?.toggleActivation()
                adapter.moveItem(position, lastItemPosition!!)
            } else {
                lastItem = adapter.getItem(position)
                lastItemPosition = position
                onItemLongClick(position)
            }
        }
    }

    override fun onUpdateManga(manga: LibraryManga) {
        if (manga.id == null) adapter.notifyDataSetChanged()
        else presenter.updateManga(manga)
    }

    private fun setSelection(position: Int, selected: Boolean = true) {
        val item = adapter.getItem(position) as? LibraryItem ?: return

        setSelection(item.manga, selected)
        invalidateActionMode()
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        // Because padding a recycler causes it to scroll up we have to scroll it back down... wild
        if ((adapter.getItem(fromPosition) is LibraryItem && adapter.getItem(fromPosition) is LibraryItem) || adapter.getItem(
                fromPosition
            ) == null
        ) recycler.scrollBy(0, recycler.paddingTop)
        activity?.appbar?.y = 0f
        if (lastItemPosition == toPosition) lastItemPosition = null
        else if (lastItemPosition == null) lastItemPosition = fromPosition
    }

    fun toggleFilters() {
        if (bottom_sheet.sheetBehavior?.isHideable == true && bottom_sheet.sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) bottom_sheet.sheetBehavior?.state =
            BottomSheetBehavior.STATE_HIDDEN
        else if (bottom_sheet.sheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED && bottom_sheet.sheetBehavior?.skipCollapsed == false) bottom_sheet.sheetBehavior?.state =
            BottomSheetBehavior.STATE_COLLAPSED
        else bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun shouldMoveItem(fromPosition: Int, toPosition: Int): Boolean {
        if (adapter.isSelected(fromPosition)) toggleSelection(fromPosition)
        val item = adapter.getItem(fromPosition) as? LibraryItem ?: return false
        val newHeader = adapter.getSectionHeader(toPosition) as? LibraryHeaderItem
        if (toPosition <= 1) return false
        return (adapter.getItem(toPosition) !is LibraryHeaderItem) && (newHeader?.category?.id == item.manga.category || !presenter.mangaIsInCategory(
            item.manga,
            newHeader?.category?.id
        ))
    }

    override fun onItemReleased(position: Int) {
        isDragging = false
        if (adapter.selectedItemCount > 0) {
            lastItemPosition = null
            return
        }
        destroyActionModeIfNeeded()
        // if nothing moved
        if (lastItemPosition == null) return
        val item = adapter.getItem(position) as? LibraryItem ?: return
        val newHeader = adapter.getSectionHeader(position) as? LibraryHeaderItem
        val libraryItems = adapter.getSectionItems(adapter.getSectionHeader(position))
            .filterIsInstance<LibraryItem>()
        val mangaIds = libraryItems.mapNotNull { (it as? LibraryItem)?.manga?.id }
        if (newHeader?.category?.id == item.manga.category) {
            presenter.rearrangeCategory(item.manga.category, mangaIds)
        } else {
            if (presenter.mangaIsInCategory(item.manga, newHeader?.category?.id)) {
                adapter.moveItem(position, lastItemPosition!!)
                snack = view?.snack(R.string.already_in_category) {
                    anchorView = bottom_sheet
                }
                return
            }
            if (newHeader?.category?.mangaSort == null) {
                moveMangaToCategory(item.manga, newHeader?.category, mangaIds, true)
            } else {
                val keepCatSort = preferences.keepCatSort().getOrDefault()
                if (keepCatSort == 0) {
                    MaterialDialog(activity!!).message(R.string.switch_to_dnd)
                        .positiveButton(R.string.action_switch) {
                            moveMangaToCategory(
                                item.manga, newHeader.category, mangaIds, true
                            )
                            if (it.isCheckPromptChecked()) preferences.keepCatSort().set(2)
                        }.checkBoxPrompt(R.string.remember_choice) {}.negativeButton(
                            text = resources?.getString(
                                R.string.keep_current_sort,
                                resources!!.getString(newHeader.category.sortRes()).toLowerCase(
                                    Locale.getDefault()
                                )
                            )
                        ) {
                            moveMangaToCategory(
                                item.manga, newHeader.category, mangaIds, false
                            )
                            if (it.isCheckPromptChecked()) preferences.keepCatSort().set(1)
                        }.cancelOnTouchOutside(false).show()
                } else {
                    moveMangaToCategory(
                        item.manga, newHeader.category, mangaIds, keepCatSort == 2
                    )
                }
            }
        }
        lastItemPosition = null
    }

    private fun moveMangaToCategory(
        manga: LibraryManga,
        category: Category?,
        mangaIds: List<Long>,
        useDND: Boolean
    ) {
        if (category?.id == null) return
        val oldCatId = manga.category
        presenter.moveMangaToCategory(manga, category.id, mangaIds, useDND)
        snack?.dismiss()
        snack = view?.snack(
            resources!!.getString(R.string.moved_to_category, category.name)
        ) {
            anchorView = bottom_sheet
            setAction(R.string.action_undo) {
                manga.category = category.id!!
                presenter.moveMangaToCategory(manga, oldCatId, mangaIds, useDND)
            }
        }
    }

    override fun updateCategory(catId: Int): Boolean {
        val category = (adapter.getItem(catId) as? LibraryHeaderItem)?.category ?: return false
        val inQueue = LibraryUpdateService.categoryInQueue(category.id)
        snack?.dismiss()
        snack = view?.snack(
            resources!!.getString(
                when {
                    inQueue -> R.string.category_already_in_queue
                    LibraryUpdateService.isRunning() -> R.string.adding_category_to_queue
                    else -> R.string.updating_category_x
                }, category.name
            ), Snackbar.LENGTH_LONG
        ) {
            anchorView = bottom_sheet
        }
        if (!inQueue) LibraryUpdateService.start(view!!.context, category)
        return true
    }

    override fun sortCategory(catId: Int, sortBy: Int) {
        presenter.sortCategory(catId, sortBy)
    }

    override fun selectAll(position: Int) {
        val header = adapter.getSectionHeader(position) ?: return
        val items = adapter.getSectionItemPositions(header)
        val allSelected = allSelected(position)
        for (i in items) setSelection(i, !allSelected)
    }

    override fun allSelected(position: Int): Boolean {
        val header = adapter.getSectionHeader(position) ?: return false
        val items = adapter.getSectionItemPositions(header)
        return items.all { adapter.isSelected(it) }
    }

    override fun onSwipeBottom(x: Float, y: Float) {}
    override fun onSwipeTop(x: Float, y: Float) {
        val sheetRect = Rect()
        activity!!.bottom_nav.getGlobalVisibleRect(sheetRect)
        if (sheetRect.contains(x.toInt(), y.toInt())) {
            if (bottom_sheet.sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) toggleFilters()
        }
    }

    override fun onSwipeLeft(x: Float, xPos: Float) = goToNextCategory(x, xPos)
    override fun onSwipeRight(x: Float, xPos: Float) = goToNextCategory(x, xPos)

    private fun goToNextCategory(x: Float, xPos: Float) {
        if (lockedRecycler && abs(x) > 1000f) {
            val sign = sign(x).roundToInt()
            if ((sign < 0 && nextCategory == null) || (sign > 0) && prevCategory == null) return
            val distance = recycler_layout.alpha
            val speed = max(5000f / abs(x), 0.75f)
            if (sign(recycler_layout.x) == sign(x)) {
                flinging = true
                val duration = (distance * 100 * speed).toLong()
                val set = AnimatorSet()
                val translationXAnimator = ValueAnimator.ofFloat(abs(xPos - startPosX!!),
                    swipeDistance)
                translationXAnimator.duration = duration
                translationXAnimator.addUpdateListener { animation ->
                    recycler_layout.x = sign *
                        (animation.animatedValue as Float / (swipeDistance / 3f)).pow(3.5f)
                }

                val translationAlphaAnimator = ValueAnimator.ofFloat(recycler_layout.alpha, 0f)
                translationAlphaAnimator.duration = duration
                translationAlphaAnimator.addUpdateListener { animation ->
                    recycler_layout.alpha = animation.animatedValue as Float
                }
                set.playTogether(translationXAnimator, translationAlphaAnimator)
                set.start()
                set.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        recycler_layout.x = -sign * (swipeDistance / (swipeDistance / 3f)).pow(3.5f)
                        recycler_layout.alpha = 0f
                        recycler_layout.post {
                            scrollToHeader((if (sign <= 0) nextCategory else prevCategory) ?: -1)
                            recycler_layout.post {
                                resetScrollingValues()
                                resetRecyclerY(true, (100 * speed).toLong())
                                flinging = false
                            }
                        }
                    }

                    override fun onAnimationCancel(animation: Animator?) {}
                    override fun onAnimationRepeat(animation: Animator?) {}
                    override fun onAnimationStart(animation: Animator?) {}
                })
            }
        }
    }

    override fun handleRootBack(): Boolean {
        val sheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        if (sheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED && sheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return true
        }
        return false
    }

    //region Toolbar options methods
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = resources?.getString(R.string.library_search_hint)

        searchItem.collapseActionView()
        if (query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }

        // Mutate the filter icon because it needs to be tinted and the resource is shared.
        menu.findItem(R.id.action_library_filter).icon.mutate()

        setOnQueryTextChangeListener(searchView) { search(it) }
        searchItem.fixExpand(onExpand = { invalidateMenuOnExpand() })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> expandActionViewFromInteraction = true
            R.id.action_library_display -> DisplayBottomSheet(this).show()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
    //endregion

    //region Action Mode Methods
    /**
     * Creates the action mode if it's not created already.
     */
    private fun createActionModeIfNeeded() {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(this)
            val view = activity?.window?.currentFocus ?: return
            val imm =
                activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    ?: return
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * Destroys the action mode.
     */
    private fun destroyActionModeIfNeeded() {
        actionMode?.finish()
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
        selectItem.isVisible = false
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
            } else menu.findItem(R.id.action_move_manga).isVisible = false
        }
        return false
    }
    //endregion

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_move_to_category -> showChangeMangaCategoriesDialog()
            R.id.action_delete -> {
                MaterialDialog(activity!!).message(R.string.confirm_manga_deletion)
                    .positiveButton(R.string.action_remove) {
                        deleteMangasFromLibrary()
                    }.negativeButton(android.R.string.no).show()
            }
            R.id.action_migrate -> {
                val skipPre = preferences.skipPreMigration().getOrDefault()
                router.pushController(
                    if (skipPre) {
                        MigrationListController.create(
                            MigrationProcedureConfig(selectedMangas.mapNotNull { it.id }, null)
                        )
                    } else {
                        PreMigrationController.create(selectedMangas.mapNotNull { it.id })
                    }.withFadeTransaction().tag(if (skipPre) MigrationListController.TAG else null)
                )
                destroyActionModeIfNeeded()
            }
            else -> return false
        }
        return true
    }

    private fun deleteMangasFromLibrary() {
        val mangas = selectedMangas.toList()
        presenter.removeMangaFromLibrary(mangas)
        destroyActionModeIfNeeded()
        snack?.dismiss()
        snack = view?.snack(
            activity?.getString(R.string.manga_removed_library) ?: "", Snackbar.LENGTH_INDEFINITE
        ) {
            anchorView = bottom_sheet
            var undoing = false
            setAction(R.string.action_undo) {
                presenter.addMangas(mangas)
                undoing = true
            }
            addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (!undoing) presenter.confirmDeletion(mangas)
                }
            })
        }
        (activity as? MainActivity)?.setUndoSnackBar(snack)
    }

    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        presenter.moveMangasToCategories(categories, mangas)
        destroyActionModeIfNeeded()
    }

    // / Method for the category view
    private fun startReading(manga: Manga) {
        val activity = activity ?: return
        val chapter = presenter.getFirstUnread(manga) ?: return
        val intent = ReaderActivity.newIntent(activity, manga, chapter)
        destroyActionModeIfNeeded()
        startActivity(intent)
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
        val commonCategoriesIndexes =
            presenter.getCommonCategories(mangas).map { categories.indexOf(it) }.toTypedArray()

        ChangeMangaCategoriesDialog(this, mangas, categories, commonCategoriesIndexes).showDialog(
            router
        )
    }

    override fun recyclerIsScrolling() = switchingCategories || lockedRecycler || lockedY
}
