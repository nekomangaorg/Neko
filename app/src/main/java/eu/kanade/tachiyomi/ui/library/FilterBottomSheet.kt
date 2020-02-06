package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.view.inflate
import kotlinx.android.synthetic.main.filter_bottom_sheet.view.*
import kotlinx.android.synthetic.main.filter_buttons.view.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.roundToInt

class FilterBottomSheet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : LinearLayout(context, attrs) {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    private lateinit var downloaded:FilterTagGroup

    private lateinit var unread:FilterTagGroup

    private lateinit var completed:FilterTagGroup

    private lateinit var tracked:FilterTagGroup

    private lateinit var categories:FilterTagGroup

    val items:List<FilterTagGroup> by lazy {
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
    var filters = listOf<FilterTagGroup>()

    init {

    }

    fun onCreate(pagerView:View) {
        val sheetBehavior = BottomSheetBehavior.from(this)
        topbar.setOnClickListener {
            if (sheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        pager = pagerView
        pager?.setPadding(0, 0, 0, topbar.height)
        sheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) {
                val minHeight = sheetBehavior.peekHeight
                val maxHeight = bottomSheet.height
                val percent = (progress * 100).roundToInt()
                val value = (percent * (maxHeight - minHeight) / 100) + minHeight
                pager?.setPadding(0, 0, 0, value)
                line.alpha = 1 - progress
            }

            override fun onStateChanged(p0: View, p1: Int) {

            }
        })
        topbar.viewTreeObserver.addOnGlobalLayoutListener {
            sheetBehavior.peekHeight = topbar.height
            if (sheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                pager?.setPadding(0, 0, 0, topbar.height)
            }
        }
        createTags()
    }

    fun createTags() {
        categories = inflate(R.layout.filter_buttons) as FilterTagGroup
        categories.setup(this, R.string.categories)
        categories.onItemClicked = { view, index -> onFilterClicked(view, index) }
        categories.firstButton.isActivated = preferences.showCategories().getOrDefault()

        downloaded = inflate(R.layout.filter_buttons) as FilterTagGroup
        downloaded.setup(this, R.string.action_filter_downloaded, R.string.action_filter_not_downloaded)
        downloaded.onItemClicked = { view, index -> onFilterClicked(view, index) }
        downloaded.setState(preferences.filterDownloaded())

        completed = inflate(R.layout.filter_buttons) as FilterTagGroup
        completed.setup(this, R.string.completed, R.string.ongoing)
        completed.onItemClicked = { view, index -> onFilterClicked(view, index) }
        completed.setState(preferences.filterCompleted())

        unread = inflate(R.layout.filter_buttons) as FilterTagGroup
        unread.setup(this, R.string.action_filter_not_started, R.string.action_filter_in_progress,
            R.string.action_filter_read)
        unread.onItemClicked = { view, index -> onFilterClicked(view, index) }
        unread.setState(preferences.filterUnread())

        tracked = inflate(R.layout.filter_buttons) as FilterTagGroup
        tracked.setup(this, R.string.action_filter_tracked, R.string.action_filter_not_tracked)
        tracked.onItemClicked = { view, index -> onFilterClicked(view, index) }
        tracked.setState(preferences.filterTracked())

        items.forEach {
            filterLayout.addView(it)
        }
    }

    private fun onFilterClicked(view: View, index: Int) {
        val transition = AutoTransition()
        transition.duration = 150
        TransitionManager.beginDelayedTransition(this, transition)
        /*f (index > -1) {
            filterScrollView.scrollX = 0
            filterLayout.removeView(view)
            filterLayout.addView(view, 0)
        }
        else{
            filterLayout.removeView(view)
            filterLayout.addView(view, items.indexOf(view as FilterTagGroup))
        }*/
        when (view) {
            categories -> {
                preferences.showCategories().set(index == 0)
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
        }
    }

    companion object {
        const val ACTION_REFRESH = 0
        const val ACTION_SORT = 1
        const val ACTION_FILTER = 2
        const val ACTION_DISPLAY = 3
        const val ACTION_BADGE = 4
    }
}