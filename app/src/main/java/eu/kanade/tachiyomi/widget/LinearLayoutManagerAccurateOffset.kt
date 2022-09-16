package eu.kanade.tachiyomi.widget

import android.content.Context
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.marginTop
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import kotlin.math.roundToInt

class LinearLayoutManagerAccurateOffset(context: Context?) : LinearLayoutManager(context) {

    // map of child adapter position to its height.
    private val childSizesMap = HashMap<Int, Int>()
    private val childTypeMap = HashMap<Int, Int>()
    private val childTypeHeightMap = HashMap<Int, HashMap<Int, Int>>()
    private val childTypeEstimateMap = HashMap<Int, Int>()
    var rView: RecyclerView? = null
    var computedRange: Int? = null

    private val toolbarHeight by lazy {
        val attrsArray = intArrayOf(R.attr.mainActionBarSize)
        val array = (context ?: rView?.context)?.obtainStyledAttributes(attrsArray)
        val height = array?.getDimensionPixelSize(0, 0) ?: 0
        array?.recycle()
        height
    }

    override fun onLayoutCompleted(state: RecyclerView.State) {
        super.onLayoutCompleted(state)
        computedRange = null
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: return
            val position = getPosition(child)
            childSizesMap[position] = child.height
            val type = getItemViewType(child)
            childTypeMap[position] = type
            childTypeHeightMap.getOrPut(type) { hashMapOf() }[position] = child.height
        }
    }

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        rView = view
    }

    override fun onDetachedFromWindow(view: RecyclerView?, recycler: RecyclerView.Recycler?) {
        super.onDetachedFromWindow(view, recycler)
        rView = null
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        if (childCount == 0) return 0
        computedRange?.let { return it }
        val childAvgHeightMap = HashMap<Int, Int>()
        val computedRange = (0 until itemCount).sumOf { getItemHeight(it, childAvgHeightMap) }
        this.computedRange = computedRange
        return computedRange
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        if (childCount == 0) return 0
        val firstChild = getChildAt(0) ?: return 0
        val firstChildPosition = (0 to childCount).toList()
            .mapNotNull { getChildAt(it) }
            .mapNotNull { pos -> getPosition(pos).takeIf { it != RecyclerView.NO_POSITION } }
            .minOrNull() ?: 0
        val childAvgHeightMap = HashMap<Int, Int>()
        val scrolledY: Int = -firstChild.y.toInt() +
            (0 until firstChildPosition).sumOf { getItemHeight(it, childAvgHeightMap) }
        return scrolledY + paddingTop
    }

    private fun getItemHeight(pos: Int, childAvgHeightMap: HashMap<Int, Int>): Int {
        return EstimatedItemHeight.itemOrEstimatedHeight(
            pos,
            rView?.adapter?.getItemViewType(pos),
            childSizesMap,
            childTypeMap,
            childTypeHeightMap,
            childTypeEstimateMap,
            childAvgHeightMap,
        )
    }

    override fun findFirstVisibleItemPosition(): Int {
        return getFirstPos(rView, toolbarHeight)
    }

    override fun findFirstCompletelyVisibleItemPosition(): Int {
        return getFirstCompletePos(rView, toolbarHeight)
    }
}

fun RecyclerView.LayoutManager.getFirstPos(recyclerView: RecyclerView?, toolbarHeight: Int): Int {
    val inset = recyclerView?.rootWindowInsetsCompat?.getInsets(systemBars())?.top ?: 0
    return (0 until childCount)
        .mapNotNull { getChildAt(it) }
        .filter {
            val isLibraryHeader = getItemViewType(it) == R.layout.library_category_header_item
            val bottom = (
                if (isLibraryHeader) {
                    it.findViewById<TextView>(R.id.category_title)?.bottom?.plus(it.y)?.roundToInt()
                } else {
                    it.bottom
                }
                ) ?: it.bottom
            bottom >= inset + toolbarHeight && it.height > 0
        }
        .mapNotNull { pos -> getPosition(pos).takeIf { it != RecyclerView.NO_POSITION } }
        .minOrNull() ?: RecyclerView.NO_POSITION
}

fun RecyclerView.LayoutManager.getFirstCompletePos(recyclerView: RecyclerView?, toolbarHeight: Int): Int {
    val inset = recyclerView?.rootWindowInsetsCompat?.getInsets(systemBars())?.top ?: 0
    return (0 until childCount)
        .mapNotNull { getChildAt(it) }
        .filter {
            val isLibraryHeader = getItemViewType(it) == R.layout.library_category_header_item
            val marginTop = if (isLibraryHeader) it.findViewById<TextView>(R.id.category_title)?.marginTop ?: 0 else 0
            it.y >= inset + toolbarHeight - marginTop && it.height > 0
        }
        .mapNotNull { pos -> getPosition(pos).takeIf { it != RecyclerView.NO_POSITION } }
        .minOrNull() ?: RecyclerView.NO_POSITION
}
