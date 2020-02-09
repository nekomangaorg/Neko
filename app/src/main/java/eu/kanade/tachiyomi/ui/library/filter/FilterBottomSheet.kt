package eu.kanade.tachiyomi.ui.library.filter

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.f2prateek.rx.preferences.Preference
import com.google.android.material.bottomsheet.BottomSheetBehavior
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.library.LibrarySort
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePadding
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.filter_bottom_sheet.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.min
import kotlin.math.roundToInt

class FilterBottomSheet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : LinearLayout(context, attrs),
    FilterTagGroupListener {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    private lateinit var downloaded: FilterTagGroup

    private lateinit var unread: FilterTagGroup

    private lateinit var completed: FilterTagGroup

    private lateinit var tracked: FilterTagGroup

    private lateinit var categories: FilterTagGroup

    private var mangaType: FilterTagGroup? = null

    var lastCategory:Category? = null

    var sheetBehavior:BottomSheetBehavior<View>? = null

    private lateinit var clearButton:ImageView

    private val filterItems:MutableList<FilterTagGroup> by lazy {
        val list = mutableListOf<FilterTagGroup>()
        if (Injekt.get<DatabaseHelper>().getCategories().executeAsBlocking().isNotEmpty())
            list.add(categories)
        list.add(downloaded)
        list.add(unread)
        list.add(completed)
        if (Injekt.get<TrackManager>().hasLoggedServices())
            list.add(tracked)
        list
    }

    var onGroupClicked: (Int) -> Unit = { _ ->  }
    var pager:View? = null

    fun onCreate(pagerView:View) {
        if (isLandscape() || isTablet()) {
            sideLayout.orientation = HORIZONTAL
            sortingLayout.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = 0
                topMargin = 0
            }
            sortScrollView?.updatePadding(
                bottom = 10.dpToPx,
                top = 0
            )
        }
        clearButton = pendingClearButton
        filterLayout.removeView(clearButton)
        sheetBehavior = BottomSheetBehavior.from(this)
        topbar.setOnClickListener {
            if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                //topbar.animate().alpha(0f).setDuration(100).start()
            } else {
                sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        pager = pagerView
        updateTitle()
        val shadow2:View = (pagerView.parent as ViewGroup).findViewById(R.id.shadow2)
        val shadow:View = (pagerView.parent as ViewGroup).findViewById(R.id.shadow)
        val coordLayout:View = (pagerView.parent as ViewGroup).findViewById(R.id.snackbar_layout)
        sheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) {
                topbar.alpha = 1 - progress
                shadow2.alpha = (1 - progress) * 0.25f
                updateRootPadding(progress)
            }

            override fun onStateChanged(p0: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_COLLAPSED) reSortViews()
                else setMainSortText()
                if (state == BottomSheetBehavior.STATE_EXPANDED)
                    topbar.alpha = 0f
                topbar.isClickable = state == BottomSheetBehavior.STATE_COLLAPSED
                topbar.isFocusable = state == BottomSheetBehavior.STATE_COLLAPSED
            }
        })
        topbar.viewTreeObserver.addOnGlobalLayoutListener {
            val phoneLandscape = (isLandscape() && !isTablet())
            sheetBehavior?.peekHeight = if (phoneLandscape) {
                if (shadow2.visibility != View.GONE) {
                    shadow.gone()
                    shadow2.gone()
                }
                0
            }
            else if (!sortText.text.isNullOrBlank()) {
                topbar.height
            }
            else 0
            if (sheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                val height = context.resources.getDimensionPixelSize(R.dimen.rounder_radius)
                pager?.setPadding(0, 0, 0, if (phoneLandscape) 0 else
                    (topbar.height - height))
                coordLayout.setPadding(0, 0, 0, topbar.height)
            }
            else {
                updateRootPadding()
            }
        }
        createTags()

        mainSortTextView.setOnClickListener { showMainSortOptions() }
        catSortTextView.setOnClickListener { showCatSortOptions() }
        clearButton.setOnClickListener { clearFilters() }
        downloadCheckbox.isChecked = preferences.downloadBadge().getOrDefault()
        downloadCheckbox.setOnCheckedChangeListener { _, isChecked ->
            preferences.downloadBadge().set(isChecked)
            onGroupClicked(ACTION_DOWNLOAD_BADGE)
        }
        setUnreadIcon()
        unread_badge.setOnClickListener {
            showUnreadMenu()
        }

        displayGroup.bindToPreference(preferences.libraryAsList())
    }

    private fun isLandscape(): Boolean {
        return context.resources.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun isTablet(): Boolean {
        return (context.resources.configuration.screenLayout and Configuration
            .SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    fun updateTitle() {
        launchUI {
            val text = withContext(Dispatchers.IO) {
                val filters = getFilters().toMutableList()
                if (filters.isEmpty()) {
                    context.getString(
                        R.string.sorting_by_, context.getString(
                            when (sorting()) {
                                LibrarySort.LAST_UPDATED -> R.string.action_sort_last_updated
                                LibrarySort.DRAG_AND_DROP -> R.string.action_sort_drag_and_drop
                                LibrarySort.TOTAL -> R.string.action_sort_total
                                LibrarySort.UNREAD -> R.string.action_filter_unread
                                LibrarySort.LAST_READ -> R.string.action_sort_last_read
                                else -> R.string.title
                            }
                        )
                    )
                } else {
                    filters.add(
                        0, when (sorting()) {
                            LibrarySort.LAST_UPDATED -> R.string.action_sort_last_updated
                            LibrarySort.DRAG_AND_DROP -> R.string.action_sort_drag_and_drop
                            LibrarySort.TOTAL -> R.string.action_sort_total
                            LibrarySort.UNREAD -> R.string.action_filter_unread
                            LibrarySort.LAST_READ -> R.string.action_sort_last_read
                            else -> R.string.title
                        }
                    )
                    filters.joinToString(", ") { context.getString(it) }
                }
            }
            sortText.text = text
            setMainSortText()
        }
    }

    fun adjustTitleMargin(downloading: Boolean) {
        val params = sortText.layoutParams as? MarginLayoutParams ?: return
        params.rightMargin = (if (downloading) 80 else 8).dpToPx
        sortText.layoutParams = params

        filterScrollView.updatePaddingRelative(end = (if (downloading) 80 else 20).dpToPx)
    }

    fun updateRootPadding(progress: Float? = null) {
        val minHeight = sheetBehavior?.peekHeight ?: 0
        val maxHeight = height
        val trueProgress = progress ?:
            if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) 1f else 0f
        val percent = (trueProgress * 100).roundToInt()
        val value = (percent * (maxHeight - minHeight) / 100) + minHeight
        val height = context.resources.getDimensionPixelSize(R.dimen.rounder_radius)
        pager?.setPadding(0, 0, 0, value - height)
    }

    fun sorting(trueSort:Boolean = false): Int {
        val sortingMode = preferences.librarySortingMode().getOrDefault()
        return if (!trueSort && sortingMode == LibrarySort.DRAG_AND_DROP &&
            lastCategory != null &&
            !preferences.hideCategories().getOrDefault()) {
            when (lastCategory?.mangaSort) {
                'a', 'b' -> LibrarySort.ALPHA
                'c', 'd' -> LibrarySort.LAST_UPDATED
                'e', 'f' -> LibrarySort.UNREAD
                'g', 'h' -> LibrarySort.LAST_READ
                else -> LibrarySort.DRAG_AND_DROP
            }
        }
        else {
            sortingMode
        }
    }

    private fun getFilters(): List<Int> {
        val filters = mutableListOf<Int>()
        val categoriesOn = !preferences.hideCategories().getOrDefault()
        if (!categoriesOn) {
            filters.add(R.string.hiding_categories)
        }
        var filter = preferences.filterDownloaded().getOrDefault()
        if (filter > 0) {
            filters.add(if (filter == 1) R.string.action_filter_downloaded else R.string
                .action_filter_not_downloaded)
        }
        filter = preferences.filterUnread().getOrDefault()
        if (filter > 0) {
            filters.add(when (filter) {
                3 -> R.string.action_filter_read
                2 -> R.string.action_filter_in_progress
                else -> R.string.action_filter_not_started
            })
        }
        filter = preferences.filterCompleted().getOrDefault()
        if (filter > 0) {
            filters.add(if (filter == 1) R.string.completed else R.string
                .ongoing)
        }
        filter = preferences.filterTracked().getOrDefault()
        if (filter > 0) {
            filters.add(if (filter == 1) R.string.action_filter_tracked else R.string
                .action_filter_not_tracked)
        }
        filter = preferences.filterMangaType().getOrDefault()
        if (filter > 0) {
            filters.add(if (filter == 1) R.string.manga_only else R.string.manwha_only)
        }
        return filters
    }


    fun createTags() {
        categories = inflate(R.layout.filter_buttons) as FilterTagGroup
        categories.setup(this, R.string.action_hide_categories)

        downloaded = inflate(R.layout.filter_buttons) as FilterTagGroup
        downloaded.setup(this, R.string.action_filter_downloaded, R.string.action_filter_not_downloaded)

        completed = inflate(R.layout.filter_buttons) as FilterTagGroup
        completed.setup(this, R.string.completed, R.string.ongoing)

        unread = inflate(R.layout.filter_buttons) as FilterTagGroup
        unread.setup(this, R.string.action_filter_not_started, R.string.action_filter_in_progress,
            R.string.action_filter_read)

        tracked = inflate(R.layout.filter_buttons) as FilterTagGroup
        tracked.setup(this, R.string.action_filter_tracked, R.string.action_filter_not_tracked)

        filterItems.forEach {
            filterLayout.addView(it)
        }

        checkForManwha()
    }

    private fun checkForManwha() {
        GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            val db:DatabaseHelper by injectLazy()
            val librryManga = db.getLibraryMangas().executeAsBlocking()
            if (librryManga.any { it.mangaType() == LibraryManga.MANWHA }) {
                launchUI {
                    val mangaType = inflate(R.layout.filter_buttons) as FilterTagGroup
                    mangaType.setup(
                        this@FilterBottomSheet,
                        R.string.manga,
                        R.string.manwha
                    )
                    this@FilterBottomSheet.mangaType = mangaType
                    filterLayout.addView(mangaType)
                    filterItems.add(mangaType)
                }
            }
            launchUI {
                categories.setState(preferences.hideCategories().getOrDefault())
                downloaded.setState(preferences.filterDownloaded())
                completed.setState(preferences.filterCompleted())
                unread.setState(preferences.filterUnread())
                tracked.setState(preferences.filterTracked())
                mangaType?.setState(preferences.filterMangaType())
                reSortViews()
            }

        }
    }

    private fun showMainSortOptions() {
        // Create a PopupMenu, giving it the clicked view for an anchor
        val popup = PopupMenu(context, mainSortTextView)

        // Inflate our menu resource into the PopupMenu's Menu
        popup.menuInflater.inflate(R.menu.main_sort, popup.menu)

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            onMainSortClicked(menuItem.itemId)
            true
        }

        if (popup.menu is MenuBuilder) {
            val m = popup.menu as MenuBuilder
            m.setOptionalIconsVisible(true)
        }

        val sortingMode = preferences.librarySortingMode().getOrDefault()
        val currentItem = popup.menu.findItem(
            when (sortingMode) {
                LibrarySort.DRAG_AND_DROP -> R.id.action_drag_and_drop
                LibrarySort.TOTAL -> R.id.action_total_chaps
                LibrarySort.LAST_READ -> R.id.action_last_read
                LibrarySort.UNREAD -> R.id.action_unread
                LibrarySort.LAST_UPDATED -> R.id.action_update
                else -> R.id.action_alpha
            }
        )
        currentItem.icon = tintVector(
            when {
                sortingMode == LibrarySort.DRAG_AND_DROP -> R.drawable.ic_check_white_24dp
                !preferences.librarySortingAscending().getOrDefault() ->
                    R.drawable.ic_arrow_down_white_24dp
                else -> R.drawable.ic_arrow_up_white_24dp
            }
        )

        // Finally show the PopupMenu
        popup.show()
    }

    private fun showCatSortOptions() {
        val category = lastCategory ?: return
        // Create a PopupMenu, giving it the clicked view for an anchor
        val popup = PopupMenu(context, catSortTextView)

        // Inflate our menu resource into the PopupMenu's Menu
        popup.menuInflater.inflate(R.menu.cat_sort, popup.menu)

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            onCatSortClicked(menuItem.itemId)
            true
        }

        val sortingMode = category.sortingMode()
        val currentItem = if (sortingMode == null) null
        else popup.menu.findItem(
            when (sortingMode) {
                LibrarySort.DRAG_AND_DROP -> R.id.action_drag_and_drop
                LibrarySort.TOTAL -> R.id.action_total_chaps
                LibrarySort.LAST_READ -> R.id.action_last_read
                LibrarySort.UNREAD -> R.id.action_unread
                LibrarySort.LAST_UPDATED -> R.id.action_update
                else -> R.id.action_alpha
            }
        )

        if (sortingMode != null && popup.menu is MenuBuilder) {
            val m = popup.menu as MenuBuilder
            m.setOptionalIconsVisible(true)
        }

        currentItem?.icon = tintVector(
            if (category.isAscending()) R.drawable.ic_arrow_up_white_24dp
            else R.drawable.ic_arrow_down_white_24dp
        )

        // Finally show the PopupMenu
        popup.show()
    }

    private fun onMainSortClicked(menuId: Int) {
        if (menuId == R.id.action_reverse) {
            preferences.librarySortingAscending().set(
                !preferences.librarySortingAscending().getOrDefault())
        }
        else {
            val sort = when (menuId) {
                    R.id.action_update -> LibrarySort.LAST_UPDATED
                    R.id.action_unread -> LibrarySort.UNREAD
                    R.id.action_total_chaps -> LibrarySort.TOTAL
                    R.id.action_last_read -> LibrarySort.LAST_READ
                    R.id.action_drag_and_drop -> LibrarySort.DRAG_AND_DROP
                    else -> LibrarySort.ALPHA
                }
            if (sort == preferences.librarySortingMode().getOrDefault()) {
                if (sort != LibrarySort.DRAG_AND_DROP)
                    onMainSortClicked(R.id.action_reverse)
                return
            }
            preferences.librarySortingMode().set(sort)
            preferences.librarySortingAscending().set(true)
        }
        setMainSortText()
        onGroupClicked(ACTION_SORT)
    }

    private fun onCatSortClicked(menuId: Int) {
        val category = lastCategory ?: return
        val modType = if (menuId == R.id.action_reverse) {
            val t = (category.mangaSort?.minus('a') ?: 0) + 1
                if (t % 2 != 0) t + 1
                else t - 1
        }
        else {
            val order = when (menuId) {
                R.id.action_last_read -> 3
                R.id.action_unread -> 2
                R.id.action_update -> 1
                else -> 0
            }
            if (order == category.catSortingMode()) {
                onCatSortClicked(R.id.action_reverse)
                return
            }
            (2 * order + 1)
        }
        launchUI {
            withContext(Dispatchers.IO) {
                setCatOrder(modType)
            }
            setCatSortText()
            onGroupClicked(ACTION_CAT_SORT)
        }
    }

    private fun setCatOrder(order: Int) {
        val category = lastCategory ?: return
        category.mangaSort = ('a' + (order - 1))
        if (category.id == 0)
            preferences.defaultMangaOrder().set(category.mangaSort.toString())
        else
            Injekt.get<DatabaseHelper>().insertCategory(category).asRxObservable().subscribe()
    }

    private fun setMainSortText() {
        launchUI {
            val sortId = withContext(Dispatchers.IO) { sorting(true) }
            val drawableL = withContext(Dispatchers.IO) {
                 tintVector(
                     when {
                         sortId == LibrarySort.DRAG_AND_DROP -> R.drawable.ic_sort_white_24dp
                         preferences.librarySortingAscending().getOrDefault() -> R.drawable.ic_arrow_up_white_24dp
                         else -> R.drawable.ic_arrow_down_white_24dp
                     }
                )
            }
            mainSortTextView.text = withContext(Dispatchers.IO) {
                context.getString(
                    if (sortId == LibrarySort.DRAG_AND_DROP) R.string.sort_library_by_
                    else R.string.sort_by_, context.getString(
                        when (sortId) {
                            LibrarySort.LAST_UPDATED -> R.string.action_sort_last_updated
                            LibrarySort.DRAG_AND_DROP -> R.string.action_sort_drag_and_drop
                            LibrarySort.TOTAL -> R.string.action_sort_total
                            LibrarySort.UNREAD -> R.string.action_filter_unread
                            LibrarySort.LAST_READ -> R.string.action_sort_last_read
                            else -> R.string.title
                        }
                    )
                )
            }
            mainSortTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawableL, null, null, null
            )
            setCatSortText()
        }
    }

    private fun setCatSortText() {
        launchUI {
            if (preferences.librarySortingMode().getOrDefault() == LibrarySort.DRAG_AND_DROP &&
                !preferences.hideCategories().getOrDefault() && lastCategory != null) {
                val sortId = withContext(Dispatchers.IO) { sorting() }
                val drawableL = withContext(Dispatchers.IO) {
                    tintVector(
                        when {
                            sortId == LibrarySort.DRAG_AND_DROP -> R.drawable.ic_label_outline_white_24dp
                            lastCategory?.isAscending() == true -> R.drawable.ic_arrow_up_white_24dp
                            else -> R.drawable.ic_arrow_down_white_24dp
                        }
                    )
                }
                catSortTextView.text = withContext(Dispatchers.IO) {
                   context.getString(
                        R.string.sort_category_by_, context.getString(
                            when (sortId) {
                                LibrarySort.LAST_UPDATED -> R.string.action_sort_last_updated
                                LibrarySort.DRAG_AND_DROP -> R.string.action_sort_drag_and_drop
                                LibrarySort.TOTAL -> R.string.action_sort_total
                                LibrarySort.UNREAD -> R.string.action_filter_unread
                                LibrarySort.LAST_READ -> R.string.action_sort_last_read
                                else -> R.string.title
                            }
                        )
                    )
                }
                catSortTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawableL, null, null, null
                )
                if (catSortTextView.visibility != View.VISIBLE) catSortTextView.visible()
            } else if (catSortTextView.visibility == View.VISIBLE) catSortTextView.gone()
        }
    }

    /**
     * Binds a radio group with a boolean preference.
     */
    private fun RadioGroup.bindToPreference(pref: Preference<Boolean>) {
        (getChildAt(pref.getOrDefault().toInt()) as RadioButton).isChecked = true
        setOnCheckedChangeListener { _, value ->
            val index = indexOfChild(findViewById(value))
            pref.set(index == 1)
            onGroupClicked(ACTION_DISPLAY)
        }
    }

    private fun Boolean.toInt() = if (this) 1 else 0

    private fun tintVector(resId: Int): Drawable? {
        return ContextCompat.getDrawable(context, resId)?.mutate()?.apply {
            setTint(context.getResourceColor(android.R.attr.colorAccent))
        }
    }


    override fun onFilterClicked(view: FilterTagGroup, index: Int, updatePreference:Boolean) {
        if (updatePreference) {
            if (view == categories) {
                preferences.hideCategories().set(index == 0)
                onGroupClicked(ACTION_REFRESH)
            } else {
                when (view) {
                    downloaded -> preferences.filterDownloaded()
                    unread -> preferences.filterUnread()
                    completed -> preferences.filterCompleted()
                    tracked -> preferences.filterTracked()
                    mangaType -> preferences.filterMangaType()
                    else -> null
                }?.set(index + 1)
                onGroupClicked (ACTION_FILTER)
            }
            updateTitle()
        }
        val filters = getFilters().size
        if (filters > 0 && clearButton.parent == null)
            filterLayout.addView(clearButton, 0)
        else if (filters == 0 && clearButton.parent != null)
            filterLayout.removeView(clearButton)
    }

    private fun clearFilters() {
        val action = if (preferences.hideCategories().getOrDefault()) ACTION_REFRESH
        else ACTION_FILTER

        preferences.hideCategories().set(false)
        preferences.filterDownloaded().set(0)
        preferences.filterUnread().set(0)
        preferences.filterCompleted().set(0)
        preferences.filterTracked().set(0)
        preferences.filterMangaType().set(0)

        val transition = androidx.transition.AutoTransition()
        transition.duration = 150
        androidx.transition.TransitionManager.beginDelayedTransition(filterLayout, transition)
        filterItems.forEach {
            it.reset()
        }
        reSortViews()
        onGroupClicked(action)
    }

    fun reSortViews() {
        filterLayout.removeAllViews()
        if (filterItems.any { it.isActivated })
            filterLayout.addView(clearButton)
        filterItems.filter { it.isActivated }.forEach {
            filterLayout.addView(it)
        }
        filterItems.filterNot { it.isActivated }.forEach {
            filterLayout.addView(it)
        }
        filterScrollView.scrollTo(0, 0)
    }

    private fun showUnreadMenu() {
        val popup = PopupMenu(context, unread_badge)

        popup.menuInflater.inflate(R.menu.unread_badge, popup.menu)

        popup.menu.getItem(min(preferences.unreadBadgeType().getOrDefault(), 1) + 1).isChecked =
            true

        popup.setOnMenuItemClickListener { menuItem ->
            preferences.unreadBadgeType().set(when (menuItem.itemId) {
                R.id.action_no_unread -> -1
                R.id.action_any_unread -> 0
                else -> 1
            })
            setUnreadIcon()
            onGroupClicked(ACTION_UNREAD_BADGE)
            true
        }

        // Finally show the PopupMenu
        popup.show()
    }

    private fun setUnreadIcon() {
        launchUI {
            val unreadType = preferences.unreadBadgeType().getOrDefault()
            val drawableL = withContext(Dispatchers.IO) {
                tintVector(
                    when (unreadType){
                        -1 -> R.drawable.ic_check_box_outline_blank_24dp
                        0 -> R.drawable.ic_unread_circle_white_24dp
                        else -> R.drawable.ic_looks_two_white_24dp
                    }
                )
            }
            unread_badge.setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawableL, null, null, null
            )
        }
    }

    companion object {
        const val ACTION_REFRESH = 0
        const val ACTION_SORT = 1
        const val ACTION_FILTER = 2
        const val ACTION_DISPLAY = 3
        const val ACTION_DOWNLOAD_BADGE = 4
        const val ACTION_UNREAD_BADGE = 5
        const val ACTION_CAT_SORT = 6
    }
}