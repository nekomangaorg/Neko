package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.inflate
import kotlinx.android.synthetic.main.filter_bottom_sheet.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

    private lateinit var downloaded:FilterTagGroup

    private lateinit var unread:FilterTagGroup

    private lateinit var completed:FilterTagGroup

    private lateinit var tracked:FilterTagGroup

    private lateinit var categories:FilterTagGroup

    private var mangaType:FilterTagGroup? = null

    var lastCategory:Category? = null

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
        val sheetBehavior = BottomSheetBehavior.from(this)
        topbar.setOnClickListener {
            if (sheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        line.alpha = 0f



        sortText.alpha = if (sheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) 1f else 0f
        title.alpha = if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) 1f else 0f

        pager = pagerView
        pager?.setPadding(0, 0, 0, topbar.height)
        updateTitle()
        sheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) {
                updateRootPadding(progress)
                sortText.alpha = 1 - progress
                title.alpha = progress
            }

            override fun onStateChanged(p0: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                    reSortViews()
                }
            }
        })
        topbar.viewTreeObserver.addOnGlobalLayoutListener {
            sheetBehavior.peekHeight = topbar.height
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                pager?.setPadding(0, 0, 0, topbar.height)
                sortText.alpha = 1f
                title.alpha = 0f
            }
            else {
                updateRootPadding()
            }
        }
        createTags()
    }

    fun updateTitle() {
        val filters = getFilters().toMutableList()
        if (filters.isEmpty()) {
            sortText.text = context.getString(
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
        }
        else {
            filters.add(0, when (sorting()) {
                LibrarySort.LAST_UPDATED -> R.string.action_sort_last_updated
                LibrarySort.DRAG_AND_DROP -> R.string.action_sort_drag_and_drop
                LibrarySort.TOTAL -> R.string.action_sort_total
                LibrarySort.UNREAD -> R.string.action_filter_unread
                LibrarySort.LAST_READ -> R.string.action_sort_last_read
                else -> R.string.action_sort_alpha
            })
            sortText.text = filters.joinToString(", ") { context.getString(it) }
        }
    }

    fun adjustTitleMargin(downloading: Boolean) {
        val params = sortText.layoutParams as? MarginLayoutParams ?: return
        params.rightMargin = (if (downloading) 80 else 8).dpToPx
        sortText.layoutParams = params
    }

    fun updateRootPadding(progress: Float? = null) {
        val sheetBehavior = BottomSheetBehavior.from(this)
        val minHeight = sheetBehavior.peekHeight
        val maxHeight = height
        val trueProgress = progress ?: if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) 1f else 0f
        val percent = (trueProgress * 100).roundToInt()
        val value = (percent * (maxHeight - minHeight) / 100) + minHeight
        pager?.setPadding(0, 0, 0, value)
    }

    fun sorting(): Int {
        val sortingMode = preferences.librarySortingMode().getOrDefault()
        return if (sortingMode == LibrarySort.DRAG_AND_DROP &&
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
            preferences.librarySortingMode().getOrDefault()
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