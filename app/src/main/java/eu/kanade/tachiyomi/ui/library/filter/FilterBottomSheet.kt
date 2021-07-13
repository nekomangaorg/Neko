package eu.kanade.tachiyomi.ui.library.filter

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.databinding.FilterBottomSheetBinding
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.library.LibraryGroup
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.compatToolTipText
import eu.kanade.tachiyomi.util.view.hide
import eu.kanade.tachiyomi.util.view.inflate
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.isHidden
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FilterBottomSheet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs),
    FilterTagGroupListener {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    private lateinit var binding: FilterBottomSheetBinding

    private val trackManager: TrackManager by injectLazy()

    private val hasTracking
        get() = trackManager.hasLoggedServices()

    private lateinit var downloaded: FilterTagGroup

    private lateinit var unreadProgress: FilterTagGroup

    private lateinit var unread: FilterTagGroup

    private lateinit var completed: FilterTagGroup

    private lateinit var merged: FilterTagGroup

    private lateinit var missingChapters: FilterTagGroup

    private var tracked: FilterTagGroup? = null

    private var trackers: FilterTagGroup? = null

    private var mangaType: FilterTagGroup? = null

    var sheetBehavior: BottomSheetBehavior<View>? = null

    var filterOrder = preferences.filterOrder().get()

    private lateinit var clearButton: ImageView

    private val filterItems: MutableList<FilterTagGroup> by lazy {
        val list = mutableListOf<FilterTagGroup>()
        list.add(unreadProgress)
        list.add(unread)
        list.add(downloaded)
        list.add(completed)
        if (hasTracking) {
            tracked?.let { list.add(it) }
        }
        list.add(merged)
        list.add(missingChapters)
        list
    }

    var onGroupClicked: (Int) -> Unit = { _ -> }
    var libraryRecyler: View? = null
    var controller: LibraryController? = null
    var bottomBarHeight = 0
    val shadowAlpha = 0.15f
    val shadow2Alpha = 0.05f

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FilterBottomSheetBinding.bind(this)
    }

    fun onCreate(controller: LibraryController) {
        clearButton = binding.clearButton
        binding.filterLayout.removeView(clearButton)
        sheetBehavior = BottomSheetBehavior.from(this)
        sheetBehavior?.isHideable = true
        this.controller = controller
        libraryRecyler = controller.binding.libraryGridRecycler.recycler
        libraryRecyler?.post {
            bottomBarHeight =
                controller.activityBinding?.bottomNav?.height
                    ?: controller.activityBinding?.root?.rootWindowInsets?.systemWindowInsetBottom
                        ?: 0
        }
        val shadow2: View = controller.binding.shadow2
        val shadow: View = controller.binding.shadow
        sheetBehavior?.addBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, progress: Float) {
                    this@FilterBottomSheet.controller?.updateFilterSheetY()
                    binding.pill.alpha = (1 - max(0f, progress)) * 0.25f
                    shadow2.alpha = (1 - max(0f, progress)) * shadow2Alpha
                    shadow.alpha = (1 + min(0f, progress)) * shadowAlpha
                    updateRootPadding(progress)
                }

                override fun onStateChanged(p0: View, state: Int) {
                    this@FilterBottomSheet.controller?.updateFilterSheetY()
                    stateChanged(state)
                }
            }
        )

        sheetBehavior?.hide()
        binding.expandCategories.setOnClickListener {
            onGroupClicked(ACTION_EXPAND_COLLAPSE_ALL)
        }
        binding.groupBy.setOnClickListener {
            onGroupClicked(ACTION_GROUP_BY)
        }
        binding.viewOptions.setOnClickListener {
            onGroupClicked(ACTION_DISPLAY)
        }

        val activeFilters = hasActiveFiltersFromPref()
        if (activeFilters && sheetBehavior.isHidden() && sheetBehavior?.skipCollapsed == false) {
            sheetBehavior?.collapse()
        }

        post {
            libraryRecyler ?: return@post
            updateRootPadding(
                when (sheetBehavior?.state) {
                    BottomSheetBehavior.STATE_HIDDEN -> -1f
                    BottomSheetBehavior.STATE_EXPANDED -> 1f
                    else -> 0f
                }
            )
            shadow.alpha = if (sheetBehavior.isHidden()) 0f else shadowAlpha

            if (binding.secondLayout.width + (binding.groupBy.width * 2) + 20.dpToPx < width) {
                binding.secondLayout.removeView(binding.viewOptions)
                binding.firstLayout.addView(binding.viewOptions)
                binding.secondLayout.isVisible = false
            } else if (binding.viewOptions.parent == binding.firstLayout) {
                binding.firstLayout.removeView(binding.viewOptions)
                binding.secondLayout.addView(binding.viewOptions)
                binding.secondLayout.isVisible = true
            }
        }

        createTags()
        clearButton.setOnClickListener { clearFilters() }

        setExpandText(controller.canCollapseOrExpandCategory(), false)

        clearButton.compatToolTipText = context.getString(R.string.clear_filters)
        preferences.filterOrder().asFlow()
            .drop(1)
            .onEach {
                filterOrder = it
                clearFilters()
            }
            .launchIn(controller.viewScope)
    }

    private fun stateChanged(state: Int) {
        val shadow = controller?.binding?.shadow ?: return
        controller?.updateHopperY()
        if (state == BottomSheetBehavior.STATE_COLLAPSED) {
            shadow.alpha = shadowAlpha
            libraryRecyler?.updatePaddingRelative(bottom = sheetBehavior?.peekHeight
                ?: 0 + 10.dpToPx + bottomBarHeight)
        }
        if (state == BottomSheetBehavior.STATE_EXPANDED) {
            binding.pill.alpha = 0f
        }
        if (state == BottomSheetBehavior.STATE_HIDDEN) {
            onGroupClicked(ACTION_HIDE_FILTER_TIP)
            reSortViews()
            shadow.alpha = 0f
            libraryRecyler?.updatePaddingRelative(bottom = 10.dpToPx + bottomBarHeight)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        val sheetBehavior = BottomSheetBehavior.from(this)
        stateChanged(sheetBehavior.state)
    }

    fun updateRootPadding(progress: Float? = null) {
        val minHeight = sheetBehavior?.peekHeight ?: 0
        val maxHeight = height
        val trueProgress = progress
            ?: if (sheetBehavior.isExpanded()) 1f else 0f
        val percent = (trueProgress * 100).roundToInt()
        val value = (percent * (maxHeight - minHeight) / 100) + minHeight
        if (trueProgress >= 0) {
            libraryRecyler?.updatePaddingRelative(bottom = value + 10.dpToPx + bottomBarHeight)
        } else {
            libraryRecyler?.updatePaddingRelative(bottom = (minHeight * (1 + trueProgress)).toInt() + bottomBarHeight)
        }
    }

    fun setExpandText(allExpanded: Boolean?, animated: Boolean = true) {
        binding.expandCategories.isVisible = allExpanded != null
        allExpanded ?: return
        binding.expandCategories.setText(
            if (!allExpanded) {
                R.string.expand_all_categories
            } else {
                R.string.collapse_all_categories
            }
        )
        if (animated) {
            binding.expandCategories.icon = AnimatedVectorDrawableCompat.create(
                binding.expandCategories.context,
                if (!allExpanded) {
                    R.drawable.anim_expand_less_to_more
                } else {
                    R.drawable.anim_expand_more_to_less
                }
            )
            (binding.expandCategories.icon as? AnimatedVectorDrawableCompat)?.start()
        } else {
            binding.expandCategories.setIconResource(
                if (!allExpanded) {
                    R.drawable.ic_expand_more_24dp
                } else {
                    R.drawable.ic_expand_less_24dp
                }
            )
        }
    }

    fun hasActiveFilters() = filterItems.any { it.isActivated }

    private fun hasActiveFiltersFromPref(): Boolean {
        return preferences.filterDownloaded().getOrDefault() > 0 ||
            preferences.filterUnread().getOrDefault() > 0 ||
            preferences.filterCompleted().getOrDefault() > 0 ||
            preferences.filterTracked().getOrDefault() > 0 ||
            preferences.filterMangaType().getOrDefault() > 0 ||
            preferences.filterMerged().getOrDefault() > 0 ||
            preferences.filterMissingChapters().getOrDefault() > 0 ||
            FILTER_TRACKER.isNotEmpty()
    }

    private fun createTags() {
        downloaded = inflate(R.layout.filter_tag_group) as FilterTagGroup
        downloaded.setup(this, R.string.downloaded, R.string.not_downloaded)

        completed = inflate(R.layout.filter_tag_group) as FilterTagGroup
        completed.setup(this, R.string.completed, R.string.ongoing)

        unreadProgress = inflate(R.layout.filter_tag_group) as FilterTagGroup
        unreadProgress.setup(this, R.string.not_started, R.string.in_progress)

        unread = inflate(R.layout.filter_tag_group) as FilterTagGroup
        unread.setup(this, R.string.unread, R.string.read)

        missingChapters = inflate(R.layout.filter_tag_group) as FilterTagGroup
        missingChapters.setup(this, R.string.has_missing_chp, R.string.no_missing_chp)

        merged = inflate(R.layout.filter_tag_group) as FilterTagGroup
        merged.setup(this, R.string.merged, R.string.not_merged)

        if (hasTracking) {
            tracked = inflate(R.layout.filter_tag_group) as FilterTagGroup
            tracked?.setup(this, R.string.tracked, R.string.not_tracked)
        }

        reSortViews()

        checkForManhwa()
    }

    private fun checkForManhwa() {
        GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT) {
            val db: DatabaseHelper by injectLazy()
            val showCategoriesCheckBox = withContext(Dispatchers.IO) {
                db.getCategories().executeAsBlocking()
                    .isNotEmpty()
            }
            val libraryManga = db.getLibraryMangaList().executeAsBlocking()
            val types = mutableListOf<Int>()
            if (libraryManga.any { it.seriesType() == Manga.TYPE_MANHWA }) types.add(R.string.manhwa)
            if (libraryManga.any { it.seriesType() == Manga.TYPE_MANHUA }) types.add(R.string.manhua)
            if (libraryManga.any { it.seriesType() == Manga.TYPE_COMIC }) types.add(R.string.comic)
            if (types.isNotEmpty()) {
                launchUI {
                    val mangaType = inflate(R.layout.filter_tag_group) as FilterTagGroup
                    mangaType.setup(
                        this@FilterBottomSheet,
                        R.string.manga,
                        types.first(),
                        types.getOrNull(1),
                        types.getOrNull(2)
                    )
                    this@FilterBottomSheet.mangaType = mangaType
                    reorderFilters()
                    reSortViews()
                }
            }
            withContext(Dispatchers.Main) {
                downloaded.setState(preferences.filterDownloaded())
                completed.setState(preferences.filterCompleted())
                val unreadP = preferences.filterUnread().getOrDefault()
                if (unreadP <= 2) {
                    unread.state = unreadP - 1
                } else if (unreadP >= 3) {
                    unreadProgress.state = unreadP - 3
                }
                tracked?.setState(preferences.filterTracked())
                mangaType?.setState(
                    when (preferences.filterMangaType().getOrDefault()) {
                        Manga.TYPE_MANGA -> context.getString(R.string.manga)
                        Manga.TYPE_MANHUA -> context.getString(R.string.manhua)
                        Manga.TYPE_MANHWA -> context.getString(R.string.manhwa)
                        Manga.TYPE_COMIC -> context.getString(R.string.comic)
                        else -> ""
                    }
                )
                missingChapters.setState(preferences.filterMissingChapters())
                merged.setState(preferences.filterMerged())

                reorderFilters()
                reSortViews()
            }

            if (filterItems.contains(tracked)) {
                val loggedServices = Injekt.get<TrackManager>().services.filter { it.isLogged }
                if (loggedServices.isNotEmpty()) {
                    val serviceNames = loggedServices.map { context.getString(it.nameRes()) }
                    withContext(Dispatchers.Main) {
                        trackers = inflate(R.layout.filter_tag_group) as FilterTagGroup
                        trackers?.setup(
                            this@FilterBottomSheet,
                            serviceNames.first(),
                            serviceNames.getOrNull(1),
                            serviceNames.getOrNull(2)
                        )
                        if (tracked?.isActivated == true) {
                            binding.filterLayout.addView(trackers)
                            filterItems.add(trackers!!)
                            trackers?.setState(FILTER_TRACKER)
                            reSortViews()
                        }
                    }
                }
            }
        }
    }

    private fun reorderFilters() {
        val array = filterOrder.toCharArray().distinct()
        filterItems.clear()
        for (c in array) {
            mapOfFilters(c)?.let {
                filterItems.add(it)
            }
        }
        listOfNotNull(
            unreadProgress,
            unread,
            downloaded,
            completed,
            mangaType,
            tracked,
            missingChapters,
            merged
        )
            .forEach {
                if (!filterItems.contains(it)) {
                    filterItems.add(it)
                }
            }
    }

    private fun addForClear(): Int {
        return if (clearButton.parent != null) 1 else 0
    }

    private fun mapOfFilters(char: Char): FilterTagGroup? {
        return when (Filters.filterOf(char)) {
            Filters.UnreadProgress -> unreadProgress
            Filters.Unread -> unread
            Filters.Downloaded -> downloaded
            Filters.Completed -> completed
            Filters.SeriesType -> mangaType
            Filters.MissingChapters -> missingChapters
            Filters.Merged -> merged
            Filters.Tracked -> if (hasTracking) tracked else null
            else -> null
        }
    }

    override fun onFilterClicked(view: FilterTagGroup, index: Int, updatePreference: Boolean) {
        if (updatePreference) {
            when (view) {
                trackers -> {
                    FILTER_TRACKER = view.nameOf(index) ?: ""
                    null
                }
                unreadProgress -> {
                    unread.reset()
                    preferences.filterUnread().set(
                        when (index) {
                            in 0..1 -> index + 3
                            else -> 0
                        }
                    )
                    null
                }
                unread -> {
                    unreadProgress.reset()
                    preferences.filterUnread()
                }
                downloaded -> preferences.filterDownloaded()
                completed -> preferences.filterCompleted()
                tracked -> preferences.filterTracked()
                merged -> preferences.filterMerged()
                missingChapters -> preferences.filterMissingChapters()
                mangaType -> {
                    val newIndex = when (view.nameOf(index)) {
                        context.getString(R.string.manga) -> Manga.TYPE_MANGA
                        context.getString(R.string.manhua) -> Manga.TYPE_MANHUA
                        context.getString(R.string.manhwa) -> Manga.TYPE_MANHWA
                        context.getString(R.string.comic) -> Manga.TYPE_COMIC
                        else -> 0
                    }
                    preferences.filterMangaType().set(newIndex)
                    null
                }
                else -> null
            }?.set(index + 1)
            onGroupClicked(ACTION_FILTER)
        }
        val hasFilters = hasActiveFilters()
        if (hasFilters && clearButton.parent == null) {
            binding.filterLayout.addView(clearButton, 0)
        } else if (!hasFilters && clearButton.parent != null) {
            binding.filterLayout.removeView(clearButton)
        }
        if (tracked?.isActivated == true && trackers != null && trackers?.parent == null) {
            binding.filterLayout.addView(trackers, filterItems.indexOf(tracked!!) + 2)
            filterItems.add(filterItems.indexOf(tracked!!) + 1, trackers!!)
        } else if (tracked?.isActivated == false && trackers?.parent != null) {
            binding.filterLayout.removeView(trackers)
            trackers?.reset()
            FILTER_TRACKER = ""
            filterItems.remove(trackers!!)
        }
    }

    fun updateGroupTypeButton(groupType: Int) {
        binding.groupBy.setIconResource(LibraryGroup.groupTypeDrawableRes(groupType))
    }

    fun clearFilters() {
        preferences.filterDownloaded().set(0)
        preferences.filterUnread().set(0)
        preferences.filterCompleted().set(0)
        preferences.filterTracked().set(0)
        preferences.filterMerged().set(0)
        preferences.filterMissingChapters().set(0)
        preferences.filterMangaType().set(0)
        FILTER_TRACKER = ""

        val transition = androidx.transition.AutoTransition()
        transition.duration = 150
        androidx.transition.TransitionManager.beginDelayedTransition(
            binding.filterLayout,
            transition
        )
        reorderFilters()
        filterItems.forEach {
            it.reset()
        }
        trackers?.let {
            filterItems.remove(it)
        }
        reSortViews()
        onGroupClicked(ACTION_FILTER)
    }

    private fun reSortViews() {
        binding.filterLayout.removeAllViews()
        if (filterItems.any { it.isActivated }) {
            binding.filterLayout.addView(clearButton)
        }
        filterItems.filter { it.isActivated }.forEach {
            binding.filterLayout.addView(it)
        }
        filterItems.filterNot { it.isActivated }.forEach {
            binding.filterLayout.addView(it)
        }
        binding.filterScroll.scrollTo(0, 0)
    }

    companion object {
        const val ACTION_REFRESH = 0
        const val ACTION_FILTER = 1
        const val ACTION_HIDE_FILTER_TIP = 2
        const val ACTION_DISPLAY = 3
        const val ACTION_EXPAND_COLLAPSE_ALL = 4
        const val ACTION_GROUP_BY = 5

        const val STATE_IGNORE = 0
        const val STATE_INCLUDE = 1
        const val STATE_EXCLUDE = 2

        var FILTER_TRACKER = ""
            private set
    }

    enum class Filters(val value: Char, @StringRes val stringRes: Int) {
        UnreadProgress('u', R.string.read_progress),
        Unread('r', R.string.unread),
        Downloaded('d', R.string.downloaded),
        Completed('c', R.string.status),
        SeriesType('m', R.string.series_type),
        MissingChapters('o', R.string.missing_chapters),
        Merged('n', R.string.merged),
        Tracked('t', R.string.tracked);

        companion object {
            val DEFAULT_ORDER = listOf(
                UnreadProgress,
                Unread,
                Downloaded,
                Completed,
                SeriesType,
                MissingChapters,
                Merged,
                Tracked
            ).joinToString("") { it.value.toString() }

            fun filterOf(char: Char) = values().find { it.value == char }
        }
    }
}
