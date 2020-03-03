package eu.kanade.tachiyomi.ui.library

import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.core.math.MathUtils.clamp
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.SwipeGestureInterface
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import kotlinx.android.synthetic.main.filter_bottom_sheet.*
import kotlinx.android.synthetic.main.library_grid_recycler.*
import kotlinx.android.synthetic.main.library_list_controller.*
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.spinner_title.view.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max

class LibraryListController(bundle: Bundle? = null) : LibraryController(bundle),
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    FlexibleAdapter.OnItemMoveListener,
    LibraryCategoryAdapter.LibraryListener,
    SwipeGestureInterface {

    private lateinit var adapter: LibraryCategoryAdapter

    private var lastClickPosition = -1

    private var updateScroll = true

    private var spinnerAdapter: SpinnerAdapter? = null

    private var lastItemPosition:Int? = null
    private var lastItem:IFlexible<*>? = null
    private lateinit var customTitleSpinner: LinearLayout
    private lateinit var titlePopupMenu:PopupMenu

    private var switchingCategories = false

    /**
     * Recycler view of the list of manga.
     */
   // private lateinit var recycler: RecyclerView

    override fun contentView():View = recycler_layout

    override fun getTitle(): String? {
        return when {
            spinnerAdapter?.array?.size ?: 0 > 1 -> null
            spinnerAdapter?.array?.size == 1 -> return spinnerAdapter?.array?.firstOrNull()
            else -> return super.getTitle()
        }
    }

    private var scrollListener = object : RecyclerView.OnScrollListener () {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val position =
                (recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val order = when (val item = adapter.getItem(position)) {
                is LibraryHeaderItem -> item.category.order
                is LibraryItem -> presenter.categories.find { it.id == item.manga.category }?.order
                else -> null
            }
            if (order != null && order != activeCategory) {
                preferences.lastUsedCategory().set(order)
                activeCategory = order
                val category = presenter.categories.find { it.order == order }

                //val categortPosition = presenter.categories.indexOf(category)
                customTitleSpinner.category_title.text = category?.name ?: ""
                /*if (spinner.selectedItemPosition != categortPosition) {
                    updateScroll = true
                    spinner.setSelection(categortPosition, true)
                }*/
            }
        }
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        /*launchUI {
            view.updateLayoutParams<FrameLayout.LayoutParams> {
                val attrsArray = intArrayOf(android.R.attr.actionBarSize)
                val array = view.context.obtainStyledAttributes(attrsArray)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    topMargin =
                        view.rootWindowInsets.systemWindowInsetTop + array.getDimensionPixelSize(
                            0, 0
                        )
                }
                array.recycle()
            }
        }*/
        // pad the recycler if the filter bottom sheet is visible
        if (!phoneLandscape) {
            val height = view.context.resources.getDimensionPixelSize(R.dimen.rounder_radius) + 4.dpToPx
            recycler.updatePaddingRelative(bottom = height)
        }
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.library_list_controller, container, false)
    }


    override fun layoutView(view: View) {
        adapter = LibraryCategoryAdapter(this)
       /* recycler =
            (library_layout.inflate(R.layout.library_grid_recycler) as AutofitRecyclerView).apply {
                spanCount = if (libraryLayout == 0) 1 else mangaPerRow
                manager.spanSizeLookup = (object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        if (libraryLayout == 0) return 1
                        val item = this@LibraryListController.adapter.getItem(position)
                        return if (item is LibraryHeaderItem) manager.spanCount else 1
                    }
                })
            }*/

        //recycler.spanCount = if (libraryLayout == 0) 1 else mangaPerRow
        if (libraryLayout == 0)recycler.spanCount =  1
        else recycler.columnWidth = (90 + (preferences.gridSize().getOrDefault() * 30)).dpToPx
        recycler.manager.spanSizeLookup = (object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (libraryLayout == 0) return 1
                val item = this@LibraryListController.adapter.getItem(position)
                return if (item is LibraryHeaderItem) recycler.manager.spanCount else 1
            }
        })
        recycler.setHasFixedSize(true)
        recycler.adapter = adapter
        //recycler_layout.addView(recycler)
        adapter.fastScroller = fast_scroller
        recycler.addOnScrollListener(scrollListener)

       /* val dropdown = library_layout.inflate(R.layout.expanded_dropdown_menu) as
            TextInputLayout // ReSpinner(view .context)
        spinner = dropdown.filled_exposed_dropdown*/

        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.actionBarTintColor, tv, true)

        /*spinner.backgroundTintList = ContextCompat.getColorStateList(
            view.context, tv.resourceId
        )
        (spinner.parent.parent as ViewGroup).removeView(spinner.parent as View)
        (activity as MainActivity).supportActionBar?.customView = spinner.parent as View*/
        customTitleSpinner = library_layout.inflate(R.layout.spinner_title) as LinearLayout
        (activity as MainActivity).supportActionBar?.setDisplayShowCustomEnabled(false)
        spinnerAdapter = SpinnerAdapter(
            view.context,
            R.layout.library_spinner_textview,
            arrayOf(resources!!.getString(R.string.label_library))
        )
        customTitleSpinner.category_title.text = view.resources.getString(R.string.label_library)

        titlePopupMenu = PopupMenu(view.context, customTitleSpinner, Gravity.END or Gravity.CENTER)
        customTitleSpinner.setOnTouchListener(titlePopupMenu.dragToOpenListener)

        titlePopupMenu.setOnMenuItemClickListener { item ->
            scrollToHeader(item.itemId)
            true
        }
        (activity as MainActivity).supportActionBar?.customView = customTitleSpinner
        //spinnerAdapter?.setDropDownViewResource(R.layout.library_spinner_entry_text)
        //spinner.setAdapter(spinnerAdapter)
       // spinner.adapter = spinnerAdapter
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            (activity as MainActivity).supportActionBar
                ?.setDisplayShowCustomEnabled(router?.backstack?.lastOrNull()?.controller() ==
                    this && spinnerAdapter?.array?.size ?: 0 > 1)
        }
        else if (type == ControllerChangeType.PUSH_EXIT) {
            (activity as MainActivity).toolbar.menu.findItem(R.id
                .action_search)?.collapseActionView()
            (activity as MainActivity).supportActionBar?.setDisplayShowCustomEnabled(false)
        }
    }

    override fun onDestroy() {
        (activity as MainActivity).supportActionBar?.setDisplayShowCustomEnabled(false)
        super.onDestroy()
    }

    override fun onNextLibraryUpdate(mangaMap: List<LibraryItem>, freshStart: Boolean) {
        val recyclerLayout = view ?: return
        destroyActionModeIfNeeded()
        if (mangaMap.isNotEmpty()) {
            empty_view?.hide()
        } else {
            empty_view?.show(R.drawable.ic_book_black_128dp, R.string.information_empty_library)
        }
        adapter.setItems(mangaMap)


        //spinner.onItemSelectedListener = null
        val categoryNames =  presenter.categories.map { it.name }.toTypedArray()
        spinnerAdapter = SpinnerAdapter(recyclerLayout.context, R.layout.library_spinner_textview,
            if (categoryNames.isNotEmpty()) categoryNames
            else arrayOf(recyclerLayout.context.getString(R.string.label_library))
        )
        //spinnerAdapter?.setDropDownViewResource(R.layout.library_spinner_entry_text)
        //spinner.setAdapter(spinnerAdapter)

        val isCurrentController = router?.backstack?.lastOrNull()?.controller() ==
            this
        (activity as AppCompatActivity).supportActionBar
            ?.setDisplayShowCustomEnabled(isCurrentController && presenter.categories.size > 1)
        if (isCurrentController) setTitle()

        //spinner.setSelection(min(presenter.categories.size - 1, activeCategory))
        customTitleSpinner.category_title.text =
            presenter.categories[clamp(activeCategory,
                0,
                presenter.categories.size - 1)].name
        updateScroll = false
        if (!freshStart) {
            justStarted = false
            if (contentView().alpha == 0f)
                contentView().animate().alpha(1f).setDuration(500).start()


        } else if (justStarted) {
            if (freshStart) scrollToHeader(activeCategory)
        }
        else {
            updateScroll = true
        }
        adapter.isLongPressDragEnabled = canDrag()
        //tabsVisibilityRelay.call(false)

        titlePopupMenu.menu.clear()
        presenter.categories.forEach { category ->
            titlePopupMenu.menu.add(0, category.order, max(0, category.order), category.name)
        }
        customTitleSpinner.setOnClickListener {

            titlePopupMenu.show()
        }
        /*spinner.onItemSelectedListener = IgnoreFirstSpinnerListener { pos ->
            if (updateScroll) {
                updateScroll = false
                return@IgnoreFirstSpinnerListener
            }
            scrollToHeader(presenter.categories[pos].order)
        }*/
    }

    private fun scrollToHeader(pos: Int, fade:Boolean = false) {
        val headerPosition = adapter.indexOf(pos)
        switchingCategories = true
        if (headerPosition > -1) {
            recycler.suppressLayout(true)
            (recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                headerPosition, if (headerPosition == 0) 0 else (-30).dpToPx
            )
            recycler.suppressLayout(false)
        }
        launchUI {
            delay(100)
            switchingCategories = false
        }
    }

    override fun reattachAdapter() {
        if (libraryLayout == 0) recycler.spanCount = 1
        else recycler.columnWidth = (90 + (preferences.gridSize().getOrDefault() * 30)).dpToPx
        val position =
            (recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        libraryLayout = preferences.libraryLayout().getOrDefault()
        recycler.adapter = adapter

        (recycler.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
    }

    override fun onSearch(query: String?): Boolean {
        this.query = query ?: ""
        adapter.setFilter(query)
        adapter.performFilter()
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        super.onDestroyActionMode(mode)
        adapter.mode = SelectableAdapter.Mode.SINGLE
        adapter.clearSelection()
        adapter.notifyDataSetChanged()
        lastClickPosition = -1
        adapter.isLongPressDragEnabled = canDrag()
    }

    override fun setSelection(manga: Manga, selected: Boolean) {
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
            }
            else {
                (recycler.findViewHolderForAdapterPosition(it) as? LibraryHeaderItem.Holder)?.setSelection()
            }
        }
    }

    override fun startReading(position: Int) {
        if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            toggleSelection(position)
            return
        }
        val manga = (adapter.getItem(position) as? LibraryItem)?.manga ?: return
        startReading(manga)
    }

    private fun toggleSelection(position: Int) {
        val item = adapter.getItem(position) as? LibraryItem ?: return

        setSelection(item.manga, !adapter.isSelected(position))
        invalidateActionMode()
    }

    override fun canDrag(): Boolean {
        val filterOff = preferences.filterCompleted().getOrDefault() +
            preferences.filterTracked().getOrDefault() +
            preferences.filterUnread().getOrDefault() +
            preferences.filterMangaType().getOrDefault() +
            preferences.filterCompleted().getOrDefault() == 0 &&
            !preferences.hideCategories().getOrDefault()
        return filterOff && adapter.mode != SelectableAdapter.Mode.MULTI
    }

    /**
     * Called when a manga is clicked.
     *
     * @param position the position of the element clicked.
     * @return true if the item should be selected, false otherwise.
     */
    override fun onItemClick(view: View?, position: Int): Boolean {
        if (switchingCategories) return false
        val item = adapter.getItem(position) as? LibraryItem ?: return false
        return if (adapter.mode == SelectableAdapter.Mode.MULTI) {
            lastClickPosition = position
            toggleSelection(position)
            true
        } else {
            openManga(item.manga, null)
            false
        }
    }

    /**
     * Called when a manga is long clicked.
     *
     * @param position the position of the element clicked.
     */
    override fun onItemLongClick(position: Int) {
        createActionModeIfNeeded()
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

    override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        val position = viewHolder?.adapterPosition ?: return
        if (actionState == 2) {
            if (lastItemPosition != null && position != lastItemPosition
                && lastItem == adapter.getItem(position)) {
                // because for whatever reason you can repeatedly tap on a currently dragging manga
                adapter.removeSelection(position)
                (recycler.findViewHolderForAdapterPosition(position) as? LibraryHolder)?.toggleActivation()
                adapter.moveItem(position, lastItemPosition!!)
            }
            else {
                lastItem = adapter.getItem(position)
                lastItemPosition = position
                onItemLongClick(position)
            }
        }
    }

    override fun onUpdateManga(manga: LibraryManga) {
        if (manga.id == null) adapter.notifyDataSetChanged()
        else super.onUpdateManga(manga)
    }

    private fun setSelection(position: Int, selected: Boolean = true) {
        val item = adapter.getItem(position) as? LibraryItem ?: return

        setSelection(item.manga, selected)
        invalidateActionMode()
    }
    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (lastItemPosition == toPosition)
            lastItemPosition = null
    }

    override fun onItemReleased(position: Int) {
        if (adapter.selectedItemCount > 0) {
            lastItemPosition = null
            return
        }
        destroyActionModeIfNeeded()
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
                snack = snackbar_layout?.snack(R.string.already_in_category)
                return
            }
            if (newHeader?.category?.mangaSort == null) {
                presenter.moveMangaToCategory(item, newHeader?.category?.id, mangaIds, true)
            } else {
                val keepCatSort = preferences.keepCatSort().getOrDefault()
                if (keepCatSort == 0) {
                    MaterialDialog(activity!!).message(R.string.switch_to_dnd)
                        .positiveButton(R.string.action_switch) {
                            presenter.moveMangaToCategory(
                                item, newHeader.category.id, mangaIds, true
                            )
                            if (it.isCheckPromptChecked()) preferences.keepCatSort().set(2)
                        }
                        .checkBoxPrompt(R.string.remember_choice) {}
                        .negativeButton(
                            text = resources?.getString(
                                R.string.keep_current_sort,
                                resources!!.getString(newHeader.category.sortRes()).toLowerCase(
                                    Locale.getDefault()
                                )
                            )
                        ) {
                            presenter.moveMangaToCategory(
                                item, newHeader.category.id, mangaIds, false
                            )
                            if (it.isCheckPromptChecked()) preferences.keepCatSort().set(1)
                        }.cancelOnTouchOutside(false).show()
                }
                else {
                    presenter.moveMangaToCategory(
                        item, newHeader.category.id, mangaIds, keepCatSort == 2
                    )
                }
            }
        }
        lastItemPosition = null
    }

    override fun shouldMoveItem(fromPosition: Int, toPosition: Int): Boolean {
        //if (adapter.selectedItemCount > 1)
           // return false
        if (adapter.isSelected(fromPosition))
            toggleSelection(fromPosition)
        val item = adapter.getItem(fromPosition) as? LibraryItem ?: return false
        val newHeader = adapter.getSectionHeader(toPosition) as? LibraryHeaderItem
        //if (adapter.getItem(toPosition) is LibraryHeaderItem) return false
        return newHeader?.category?.id == item.manga.category ||
            !presenter.mangaIsInCategory(item.manga, newHeader?.category?.id)
    }

    override fun updateCategory(catId: Int): Boolean {
        val category = (adapter.getItem(catId) as? LibraryHeaderItem)?.category ?:
        return false
        val inQueue = LibraryUpdateService.categoryInQueue(category.id)
        snack?.dismiss()
        snack = snackbar_layout.snack(resources!!.getString(
            when {
                inQueue -> R.string.category_already_in_queue
                LibraryUpdateService.isRunning() ->
                    R.string.adding_category_to_queue
                else -> R.string.updating_category_x
            }, category.name))
        if (!inQueue)
            LibraryUpdateService.start(view!!.context, category)
        return true
    }

    override fun sortCategory(catId: Int, sortBy: Int) {
        presenter.sortCategory(catId, sortBy)
    }

    override fun selectAll(position: Int) {
        val header = adapter.getSectionHeader(position) ?: return
        val items = adapter.getSectionItemPositions(header)
        val allSelected = allSelected(position)
        for (i in items)
            setSelection(i, !allSelected)
    }

    override fun allSelected(position: Int): Boolean {
        val header = adapter.getSectionHeader(position) ?: return false
        val items = adapter.getSectionItemPositions(header)
        return items.all { adapter.isSelected(it) }
    }

    override fun showCategories(position: Int, view: View) {
        if (presenter.categories.size <= 1) return
        val header = adapter.getSectionHeader(position) as? LibraryHeaderItem ?: return
        if (presenter.categories.size <= 1) return
        val category = header.category
        val popupMenu = PopupMenu(view.context, view)
        presenter.categories.forEach {
            if (it.id != category.id)
                popupMenu.menu.add(0, it.order, it.order, it.name)
        }
        popupMenu.setOnMenuItemClickListener {
            scrollToHeader(it.itemId)
            true
        }
        popupMenu.show()
    }

    override fun onSwipeBottom(x: Float, y: Float) { }
    override fun onSwipeTop(x: Float, y: Float) { }
    override fun onSwipeLeft(x: Float, y: Float) = goToNextCategory(x, y,-1)
    override fun onSwipeRight(x: Float, y: Float) = goToNextCategory(x, y,1)

    private fun goToNextCategory(x: Float, y: Float, offset: Int) {
        val sheetRect = Rect()
        val recyclerRect = Rect()
        bottom_sheet.getGlobalVisibleRect(sheetRect)
        recycler.getGlobalVisibleRect(recyclerRect)

        if (sheetRect.contains(x.toInt(), y.toInt()) ||
            !recyclerRect.contains(x.toInt(), y.toInt())) {
           return
        }
        val position =
            (recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val order = when (val item = adapter.getItem(position)) {
            is LibraryHeaderItem -> item.category.order
            is LibraryItem -> presenter.categories.find { it.id == item.manga.category }?.order
                ?.plus(if (offset < 0) 1 else 0)
            else -> null
        }
        if (order != null) {
            var newOffset = order + offset
            while (adapter.indexOf(newOffset) == -1 && presenter.categories.any { it.order == newOffset }) {
                newOffset += offset
            }
            scrollToHeader (newOffset, true)
        }
    }
}