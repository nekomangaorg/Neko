package eu.kanade.tachiyomi.ui.library.filter

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.filter_bottom_sheet.view.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.max
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

    private var trackers: FilterTagGroup? = null

    private var mangaType: FilterTagGroup? = null

    var sheetBehavior:BottomSheetBehavior<View>? = null

    private lateinit var clearButton:ImageView

    private val filterItems:MutableList<FilterTagGroup> by lazy {
        val list = mutableListOf<FilterTagGroup>()
        list.add(unread)
        list.add(downloaded)
        list.add(completed)
        if (Injekt.get<TrackManager>().hasLoggedServices())
            list.add(tracked)
        list
    }

    var onGroupClicked: (Int) -> Unit = { _ ->  }
    var pager:View? = null

    fun onCreate(pagerView:View) {
        clearButton = clear_button
        filter_layout.removeView(clearButton)
        sheetBehavior = BottomSheetBehavior.from(this)
        val phoneLandscape = (isLandscape() && !isTablet())
        sheetBehavior?.isHideable = true
        sheetBehavior?.skipCollapsed = phoneLandscape
        top_bar.setOnClickListener {
            if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        pager = pagerView
        val shadow2:View = (pagerView.parent as ViewGroup).findViewById(R.id.shadow2)
        val shadow:View = (pagerView.parent as ViewGroup).findViewById(R.id.shadow)
//        val snackbarLayout:View = (pagerView.parent as ViewGroup).findViewById(R.id.snackbar_layout)
        if (phoneLandscape) {
            sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }
        sheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) {
                top_bar.alpha = 1 - max(0f, progress)
                shadow2.alpha = (1 - max(0f, progress)) * 0.25f
                if (phoneLandscape)
                    shadow.alpha = progress
                else
                    shadow.alpha = 1 + min(0f, progress)
                //if (progress >= 0)
                    updateRootPadding(progress)
            }

            override fun onStateChanged(p0: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (phoneLandscape)
                        reSortViews()
                    if (phoneLandscape)
                        shadow.alpha = 0f
                    else
                        shadow.alpha = 1f
                    pager?.updatePaddingRelative(bottom = sheetBehavior?.peekHeight ?: 0)
                }
                if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    top_bar.alpha = 0f
                    if (phoneLandscape)
                        shadow.alpha = 1f
                }
                if (state == BottomSheetBehavior.STATE_HIDDEN) {
                    reSortViews()
                    shadow.alpha = 0f
                    pager?.updatePaddingRelative(bottom = 0)
//                    snackbarLayout.updatePaddingRelative(bottom = 0)
                }
                //top_bar.isClickable = state == BottomSheetBehavior.STATE_COLLAPSED
                //top_bar.isFocusable = state == BottomSheetBehavior.STATE_COLLAPSED
            }
        })
        if (preferences.hideFiltersAtStart().getOrDefault()) {
            sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
            shadow.alpha = 0f
        }
        else {
            pager?.updatePaddingRelative(bottom = sheetBehavior?.peekHeight ?: 0)
//            snackbarLayout.updatePaddingRelative(bottom = sheetBehavior?.peekHeight ?: 0)
        }
        if (phoneLandscape && shadow2.visibility != View.GONE) {
            shadow2.gone()
        }
        hide_filters.isChecked = preferences.hideFiltersAtStart().getOrDefault()
        hide_filters.setOnCheckedChangeListener { _, isChecked ->
            preferences.hideFiltersAtStart().set(isChecked)
            if (isChecked)
                onGroupClicked(ACTION_HIDE_FILTER_TIP)
        }
        createTags()
        clearButton.setOnClickListener { clearFilters() }
    }

    private fun isLandscape(): Boolean {
        return context.resources.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun isTablet(): Boolean {
        return (context.resources.configuration.screenLayout and Configuration
            .SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    fun updateRootPadding(progress: Float? = null) {
        val minHeight = sheetBehavior?.peekHeight ?: 0
        val maxHeight = height
        val trueProgress = progress ?:
            if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) 1f else 0f
        val percent = (trueProgress * 100).roundToInt()
        val value = (percent * (maxHeight - minHeight) / 100) + minHeight
        val height = context.resources.getDimensionPixelSize(R.dimen.rounder_radius)
        if (trueProgress >= 0)
            pager?.updatePaddingRelative(bottom = value - height)
        else
            pager?.updatePaddingRelative(bottom = (minHeight * (1 + trueProgress)).toInt())
    }

    fun hasActiveFilters() = filterItems.any { it.isActivated }

    private fun createTags() {
        //categories = inflate(R.layout.filter_buttons) as FilterTagGroup
       // categories.setup(this, R.string.action_hide_categories)

        //  list.add(categories)

        hide_categories.isChecked = preferences.hideCategories().getOrDefault()
        hide_categories.setOnCheckedChangeListener { _, isChecked ->
            preferences.hideCategories().set(isChecked)
            onGroupClicked(ACTION_REFRESH)
        }

        downloaded = inflate(R.layout.filter_buttons) as FilterTagGroup
        downloaded.setup(this, R.string.action_filter_downloaded, R.string.action_filter_not_downloaded)

        completed = inflate(R.layout.filter_buttons) as FilterTagGroup
        completed.setup(this, R.string.completed, R.string.ongoing)

        unread = inflate(R.layout.filter_buttons) as FilterTagGroup
        unread.setup(this, R.string.action_filter_not_started, R.string.action_filter_in_progress,
            R.string.action_filter_read)

        tracked = inflate(R.layout.filter_buttons) as FilterTagGroup
        tracked.setup(this, R.string.action_filter_tracked, R.string.action_filter_not_tracked)

        reSortViews()

        checkForManhwa()
    }

    private fun checkForManhwa() {
        GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            val db:DatabaseHelper by injectLazy()
            val showCategoriesCheckBox = withContext(Dispatchers.IO) {
                db.getCategories().executeAsBlocking()
                    .isNotEmpty()
            }
            val librryManga = db.getLibraryMangas().executeAsBlocking()
            val types = mutableListOf<Int>()
            if (librryManga.any { it.mangaType() == Manga.TYPE_MANHWA }) types.add(R.string.manhwa)
            if (librryManga.any { it.mangaType() == Manga.TYPE_MANHUA }) types.add(R.string.manhua)
            if (librryManga.any { it.mangaType() == Manga.TYPE_COMIC }) types.add(R.string.comic)
            if (types.isNotEmpty()) {
                launchUI {
                    val mangaType = inflate(R.layout.filter_buttons) as FilterTagGroup
                    mangaType.setup(
                        this@FilterBottomSheet,
                        types.first(),
                        types.getOrNull(1),
                        types.getOrNull(2)
                    )
                    this@FilterBottomSheet.mangaType = mangaType
                    filter_layout.addView(mangaType)
                    filterItems.remove(tracked)
                    filterItems.add(mangaType)
                    filterItems.add(tracked)
                }
            }
            withContext(Dispatchers.Main)  {
                hide_categories.visibleIf(showCategoriesCheckBox)
               // categories.setState(preferences.hideCategories().getOrDefault())
                downloaded.setState(preferences.filterDownloaded())
                completed.setState(preferences.filterCompleted())
                unread.setState(preferences.filterUnread())
                tracked.setState(preferences.filterTracked())
                mangaType?.setState(preferences.filterMangaType())
                reSortViews()
            }

            if (filterItems.contains(tracked)) {
                val loggedServices = Injekt.get<TrackManager>().services.filter { it.isLogged }
                if (loggedServices.size > 1) {
                    val serviceNames = loggedServices.map { it.name }
                    withContext(Dispatchers.Main) {
                        trackers = inflate(R.layout.filter_buttons) as FilterTagGroup
                        trackers?.setup(
                            this@FilterBottomSheet,
                            serviceNames.first(),
                            serviceNames.getOrNull(1),
                            serviceNames.getOrNull(2)
                        )
                        if (tracked.isActivated) {
                            filter_layout.addView(trackers)
                            filterItems.add(trackers!!)
                            trackers?.setState(FILTER_TRACKER)
                            reSortViews()
                        }
                    }
                }
            }

        }
    }

    override fun onFilterClicked(view: FilterTagGroup, index: Int, updatePreference:Boolean) {
        if (updatePreference) {
            if (view == trackers) {
                FILTER_TRACKER = view.nameOf(index) ?: ""
            } else {
                when (view) {
                    downloaded -> preferences.filterDownloaded()
                    unread -> preferences.filterUnread()
                    completed -> preferences.filterCompleted()
                    tracked -> preferences.filterTracked()
                    mangaType -> preferences.filterMangaType()
                    else -> null
                }?.set(index + 1)
            }
            onGroupClicked(ACTION_FILTER)
        }
        if (preferences.filterTracked().getOrDefault() == 1 &&
            trackers != null && trackers?.parent == null) {
            filter_layout.addView(trackers)
            filterItems.add(trackers!!)
        }
        else if (preferences.filterTracked().getOrDefault() != 1 &&
            trackers?.parent != null) {
            filter_layout.removeView(trackers)
            trackers?.reset()
            FILTER_TRACKER = ""
            filterItems.remove(trackers!!)
        }
        val hasFilters = hasActiveFilters()
        if (hasFilters && clearButton.parent == null)
            filter_layout.addView(clearButton, 0)
        else if (!hasFilters && clearButton.parent != null)
            filter_layout.removeView(clearButton)
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
        FILTER_TRACKER = ""

        val transition = androidx.transition.AutoTransition()
        transition.duration = 150
        androidx.transition.TransitionManager.beginDelayedTransition(filter_layout, transition)
        filterItems.forEach {
            it.reset()
        }
        reSortViews()
        onGroupClicked(action)
    }

    private fun reSortViews() {
        filter_layout.removeAllViews()
        if (filterItems.any { it.isActivated })
            filter_layout.addView(clearButton)
        filterItems.filter { it.isActivated }.forEach {
            filter_layout.addView(it)
        }
        filterItems.filterNot { it.isActivated }.forEach {
            filter_layout.addView(it)
        }
        filter_scroll.scrollTo(0, 0)
    }

    fun adjustFiltersMargin(downloading: Boolean) {
        filter_scroll.updatePaddingRelative(end = (if (downloading) 80 else 20).dpToPx)
    }

    companion object {
        const val ACTION_REFRESH = 0
        const val ACTION_FILTER = 1
        const val ACTION_HIDE_FILTER_TIP = 2
        var FILTER_TRACKER = ""
            private set
    }
}