package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.edit
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.doOnNextLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.pxToDp
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

class AutofitRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    androidx.recyclerview.widget.RecyclerView(context, attrs) {

    var manager: LayoutManager = GridLayoutManagerAccurateOffset(context, 1)

    var lastMeasuredWidth = 0
    var columnWidth = -1f
        set(value) {
            field = value
            if (width > 0) {
                setSpan(true)
            }
        }

    var spanCount = 0
        set(value) {
            field = value
            if (value > 0) {
                managerSpanCount = value
            }
        }

    val itemWidth: Int
        get() {
            return if (width == 0) {
                measuredWidth / getTempSpan()
            } else {
                width / managerSpanCount
            }
        }

    init {
        layoutManager = manager
    }

    var managerSpanCount: Int
        get() {
            return (manager as? GridLayoutManager)?.spanCount
                ?: (manager as StaggeredGridLayoutManager).spanCount
        }
        set(value) {
            (manager as? GridLayoutManager)?.spanCount = value
            (manager as? StaggeredGridLayoutManager)?.spanCount = value
        }

    private fun getTempSpan(): Int {
        if (columnWidth > 0) {
            val dpWidth = (measuredWidth.toFloat().pxToDp / 100f).roundToInt()
            return max(1, (dpWidth / columnWidth).roundToInt())
        }
        return 3
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        setSpan()
        if (width == 0) {
            spanCount = getTempSpan()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        setSpan()
        lastMeasuredWidth = width
    }

    fun useStaggered(preferences: PreferencesHelper) {
        useStaggered(
            preferences.useStaggeredGrid().get() &&
                !preferences.uniformGrid().get() &&
                preferences.libraryLayout().get() != LibraryItem.LAYOUT_LIST,
        )
    }

    private fun useStaggered(use: Boolean) {
        if (use && manager !is StaggeredGridLayoutManagerAccurateOffset) {
            manager = StaggeredGridLayoutManagerAccurateOffset(
                context,
                null,
                1,
                StaggeredGridLayoutManager.VERTICAL,
            )
            setNewManager()
        } else if (!use && manager !is GridLayoutManagerAccurateOffset) {
            manager = GridLayoutManagerAccurateOffset(context, 1)
            setNewManager()
        }
    }

    private fun setNewManager() {
        val firstPos = findFirstVisibleItemPosition().takeIf { it != NO_POSITION }
        layoutManager = manager
        if (firstPos != null) {
            val insetsTop = rootWindowInsetsCompat?.getInsets(systemBars())?.top ?: 0
            doOnNextLayout {
                scrollToPositionWithOffset(firstPos, -paddingTop + 56.dpToPx + insetsTop)
            }
        }
    }

    fun scrollToPositionWithOffset(position: Int, offset: Int) {
        layoutManager ?: return
        return (layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, offset)
            ?: (layoutManager as StaggeredGridLayoutManagerAccurateOffset).scrollToPositionWithOffset(
                position,
                offset,
            )
    }

    fun findFirstVisibleItemPosition(): Int {
        layoutManager ?: return 0
        return (layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
            ?: (layoutManager as StaggeredGridLayoutManagerAccurateOffset).findFirstVisibleItemPosition()
    }

    fun findFirstCompletelyVisibleItemPosition(): Int {
        layoutManager ?: return 0
        return (layoutManager as? LinearLayoutManager)?.findFirstCompletelyVisibleItemPosition()
            ?: (layoutManager as StaggeredGridLayoutManagerAccurateOffset).findFirstCompletelyVisibleItemPosition()
    }

    fun setGridSize(preferences: PreferencesHelper) {
        // Migrate to float for grid size
        if (preferences.gridSize().isNotSet()) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val oldGridSize = prefs.getInt("grid_size", -1)
            if (oldGridSize != -1) {
                preferences.gridSize().set(
                    when (oldGridSize) {
                        4 -> 3f
                        3 -> 1.5f
                        2 -> 1f
                        1 -> 0f
                        0 -> -.5f
                        else -> .5f
                    },
                )
                prefs.edit {
                    remove("grid_size")
                }
            }
        }

        val size = 1.5f.pow(preferences.gridSize().get())
        val trueSize = MULTIPLE * ((size * 100 / MULTIPLE).roundToInt()) / 100f
        columnWidth = trueSize
    }

    private fun setSpan(force: Boolean = false) {
        if ((
            spanCount == 0 || force ||
                // Add 100dp check to make sure we dont update span for sidenav changes
                (width != lastMeasuredWidth && abs(width - lastMeasuredWidth) > 100.dpToPx)
            ) &&
            columnWidth > 0
        ) {
            val dpWidth = (width.pxToDp / 100f).roundToInt()
            val count = max(1, (dpWidth / columnWidth).roundToInt())
            spanCount = count
        }
    }

    companion object {
        private const val MULTIPLE_PERCENT = 0.25f
        const val MULTIPLE = MULTIPLE_PERCENT * 100
    }
}
