package eu.kanade.tachiyomi.widget

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlin.math.min
import kotlin.math.roundToInt

object EstimatedItemHeight {
    /** gives height of a view holder, or an estimated based on others, or a hard coded estimate */
    fun itemOrEstimatedHeight(
        pos: Int,
        itemViewType: Int?,
        childSizesMap: HashMap<Int, Int>,
        childTypeMap: HashMap<Int, Int>,
        childTypeHeightMap: HashMap<Int, HashMap<Int, Int>>,
        childTypeEstimateMap: HashMap<Int, Int>,
        childAvgHeightMap: HashMap<Int, Int>,
    ): Int {
        return if (childSizesMap[pos] != null) {
            childSizesMap[pos] ?: 0
        } else {
            val type = childTypeMap.getOrPut(pos) { itemViewType ?: 0 }
            when {
                childTypeEstimateMap[type] != null -> childTypeEstimateMap[type] ?: 0
                childAvgHeightMap[type] == null && !childTypeHeightMap[type]?.values.isNullOrEmpty() -> {
                    val array = (childTypeHeightMap[type]?.values ?: mutableListOf(0)).toIntArray()
                    childAvgHeightMap[type] = array
                        .copyOfRange(0, min(array.size, 10))
                        .average()
                        .roundToInt()
                    if (array.size >= 10) {
                        childTypeEstimateMap[type] = childAvgHeightMap[type]!!
                    }
                    childAvgHeightMap[type] ?: 0
                }
                else -> childAvgHeightMap[type] ?: estimatedHeight(type)
            }
        }
    }

    /**
     * Used for estimates of heights of recycler view holders
     *
     * Only needed to provide in cases where a layout type only shows up when scroll
     * (for example: R.layout.chapters_item might not show until scrolling if
     * R.layout.manga_header_item is too tall
     */
    private fun estimatedHeight(id: Int): Int {
        return when (id) {
            R.layout.recent_manga_item -> 92.dpToPx
            R.layout.recents_header_item -> 40.dpToPx
            R.layout.recent_chapters_section_item -> 32.dpToPx
            R.layout.chapters_item -> 60.dpToPx
            R.layout.manga_header_item -> 500.dpToPx
            R.layout.chapter_header_item -> 47.dpToPx
            R.layout.manga_grid_item -> 222.dpToPx
            R.layout.manga_list_item -> 52.dpToPx
            else -> 0
        }
    }
}
