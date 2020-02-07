package eu.kanade.tachiyomi.ui.library.filter

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
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
import eu.kanade.tachiyomi.util.view.marginBottom
import eu.kanade.tachiyomi.util.view.marginTop
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePadding
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
    val recycler = androidx.recyclerview.widget.RecyclerView(context)
    var pager:View? = null

    fun onCreate(pagerView:View) {
        if (context.resources.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            sortLayout.orientation = HORIZONTAL
            val marginValue = 10.dpToPx
            arrayListOf(mainSortTextView, catSortTextView, displayLayout).forEach {
                it.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = 0
                    topMargin = 0
                }
            }
            sortScrollView.updatePadding(
                bottom = marginValue,
                top = 0
            )
        }
        sheetBehavior = BottomSheetBehavior.from(this)
        topbar.setOnClickListener {
            if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        sortText.alpha = if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) 1f else 0f
        title.alpha = if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) 1f else 0f

        pager = pagerView
        pager?.setPadding(0, 0, 0, topbar.height)
        updateTitle()
        sheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) {
                updateRootPadding(progress)
                val newProg = when {
                    progress > 0.9f -> 1f
                    progress < 0.1f -> 0f
                    else -> progress
                }
                sortText.alpha = 1 - newProg
                title.alpha = newProg
            }

            override fun onStateChanged(p0: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_COLLAPSED) reSortViews()
                else setMainSortText()
            }
        })
        topbar.viewTreeObserver.addOnGlobalLayoutListener {
            sheetBehavior?.peekHeight = topbar.height
            if (sheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                val height = context.resources.getDimensionPixelSize(R.dimen.rounder_radius)
                pager?.setPadding(0, 0, 0, topbar.height - height)
                sortText.alpha = 1f
                title.alpha = 0f
            }
            else {
                updateRootPadding()
            }
        }
        createTags()

        mainSortTextView.setOnClickListener { showMainSortOptions() }
        catSortTextView.setOnClickListener { showCatSortOptions() }

        displayGroup.bindToPreference(preferences.libraryAsList())
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
                            else -> R.string.action_sort_alpha
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
            preferences.showCategories().getOrDefault()) {
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
        val categoriesOn = preferences.showCategories().getOrDefault()
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
        categories.setup(this, R.string.hide_categories)

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
                categories.setState(!preferences.showCategories().getOrDefault())
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
            onMainSortClicked(menuItem)
            true
        }
        popup.menu.findItem(R.id.action_reverse).isVisible =
            preferences.librarySortingMode().getOrDefault() != LibrarySort.DRAG_AND_DROP

        // Finally show the PopupMenu
        popup.show()
    }

    private fun showCatSortOptions() {
        // Create a PopupMenu, giving it the clicked view for an anchor
        val popup = PopupMenu(context, catSortTextView)

        // Inflate our menu resource into the PopupMenu's Menu
        popup.menuInflater.inflate(R.menu.cat_sort, popup.menu)

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            onCatSortClicked(menuItem)
            true
        }
        popup.menu.findItem(R.id.action_reverse).isVisible = lastCategory?.mangaSort != null

        // Finally show the PopupMenu
        popup.show()
    }

    private fun onMainSortClicked(menu: MenuItem) {
        if (menu.itemId == R.id.action_reverse) {
            preferences.librarySortingAscending().set(
                !preferences.librarySortingAscending().getOrDefault())
        }
        else {
            preferences.librarySortingMode().set(
                when (menu.itemId) {
                    R.id.action_update -> LibrarySort.LAST_UPDATED
                    R.id.action_unread -> LibrarySort.UNREAD
                    R.id.action_total_chaps -> LibrarySort.TOTAL
                    R.id.action_last_read -> LibrarySort.LAST_READ
                    R.id.action_drag_and_drop -> LibrarySort.DRAG_AND_DROP
                    else -> LibrarySort.ALPHA
                }
            )
            preferences.librarySortingAscending().set(true)
        }
        setMainSortText()
        onGroupClicked(ACTION_SORT)
    }

    private fun onCatSortClicked(menu: MenuItem) {
        val category = lastCategory ?: return
        val modType = if (menu.itemId == R.id.action_reverse) {
            val t = (category.mangaSort?.minus('a') ?: 0) + 1
                if (t % 2 != 0) t + 1
                else t - 1
        }
        else {
            val order = when (menu.itemId) {
                R.id.action_last_read -> 3
                R.id.action_unread -> 2
                R.id.action_update -> 1
                else -> 0
            }
            (2 * order + 1)
        }
        setCatOrder(modType)
        setCatSortText()
        onGroupClicked(ACTION_SORT)
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
        //if (sheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) return
        launchUI {
            val sortId = withContext(Dispatchers.IO) { sorting(true) }
            val drawable = withContext(Dispatchers.IO) {
                tintVector(
                    when {
                        sortId == LibrarySort.DRAG_AND_DROP -> R.drawable.ic_sort_white_24dp
                        preferences.librarySortingAscending().getOrDefault() -> R.drawable
                            .ic_arrow_up_white_24dp
                        else -> R.drawable.ic_arrow_down_white_24dp
                    }
                )
            }
            mainSortTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawable, null, null, null
            )
            mainSortTextView.text = withContext(Dispatchers.IO) {
                context.getString(
                    if (sortId == LibrarySort.DRAG_AND_DROP) R.string.sort_library_by_
                    else R.string.sort_by_
                    , context.getString(
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
            setCatSortText()
        }
    }

    private fun setCatSortText() {
        launchUI {
            if (preferences.librarySortingMode().getOrDefault() == LibrarySort.DRAG_AND_DROP && preferences.showCategories().getOrDefault() && lastCategory != null) {
                val sortId = withContext(Dispatchers.IO) { sorting() }
                val drawable = withContext(Dispatchers.IO) {
                    tintVector(
                        when {
                            sortId == LibrarySort.DRAG_AND_DROP -> R.drawable.ic_sort_white_24dp
                            lastCategory?.isAscending() == true -> R.drawable
                                .ic_arrow_up_white_24dp
                            else -> R.drawable.ic_arrow_down_white_24dp
                        }
                    )
                }
                catSortTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawable, null, null, null
                )
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
            setTint(context.getResourceColor(R.attr.actionBarTintColor))
        }
    }


    override fun onFilterClicked(view: FilterTagGroup, index: Int, updatePreference:Boolean) {
        if (updatePreference) {
            when (view) {
                categories -> {
                    preferences.showCategories().set(index != 0)
                    onGroupClicked(ACTION_REFRESH)
                }
                downloaded -> {
                    preferences.filterDownloaded().set(index + 1)
                    onGroupClicked(ACTION_FILTER)
                }
                unread -> {
                    preferences.filterUnread().set(index + 1)
                    onGroupClicked(ACTION_FILTER)
                }
                completed -> {
                    preferences.filterCompleted().set(index + 1)
                    onGroupClicked(ACTION_FILTER)
                }
                tracked -> {
                    preferences.filterTracked().set(index + 1)
                    onGroupClicked(ACTION_FILTER)
                }
                mangaType -> {
                    preferences.filterMangaType().set(index + 1)
                    onGroupClicked(ACTION_FILTER)
                }
            }
            updateTitle()
        }
    }

    fun reSortViews() {
        filterLayout.removeAllViews()
        filterItems.filter { it.isActivated }.forEach {
            filterLayout.addView(it)
        }
        filterItems.filterNot { it.isActivated }.forEach {
            filterLayout.addView(it)
        }
        filterScrollView.scrollTo(0, 0)
    }

    companion object {
        const val ACTION_REFRESH = 0
        const val ACTION_SORT = 1
        const val ACTION_FILTER = 2
        const val ACTION_DISPLAY = 3
        const val ACTION_BADGE = 4
    }
}